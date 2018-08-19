package net.pincette.xml.stream;

import static java.util.Optional.empty;
import static java.util.regex.Pattern.compile;
import static net.pincette.io.StreamConnector.copy;
import static net.pincette.util.Collections.list;
import static net.pincette.util.MimeType.getParameter;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.takeWhile;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.Util.isXml;
import static net.pincette.xml.Util.stream;
import static net.pincette.xml.stream.Util.accumulate;
import static net.pincette.xml.stream.Util.addElement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import net.pincette.util.Pair;
import org.w3c.dom.Element;

/**
 * Implements XInclude without support for content negotiation and XPointer. The filter relies on
 * the presence of xml:base attributes or the <code>baseURI</code> constructor argument to determine
 * the base URI.
 *
 * @author Werner Donn\u00e9
 */
public class XIncludeEventReader extends EventReaderDelegate {
  private static final Pattern PI_ENCODING = compile("encoding=[\"']([^\"']+)[\"']");
  private static final String XINCLUDE = "http://www.w3.org/2001/XInclude";

  private final XMLEventFactory eventFactory = XMLEventFactory.newFactory();
  private final XMLInputFactory factory;
  private final Set<String> included;
  private final BaseURITracker tracker;
  private XMLEventReader reader;

  public XIncludeEventReader(final String baseURI, final XMLInputFactory factory) {
    this(baseURI, factory, null);
  }

  public XIncludeEventReader(
      final String baseURI, final XMLInputFactory factory, final XMLEventReader reader) {
    this(baseURI, factory, null, reader);
  }

  private XIncludeEventReader(
      final String baseURI,
      final XMLInputFactory factory,
      final Set<String> included,
      final XMLEventReader reader) {
    super(reader);
    tracker = new BaseURITracker(baseURI);
    this.factory = factory;
    this.included = included != null ? included : new HashSet<>();
  }

  private static String charset(final String encoding) {
    return !"".equals(encoding) ? encoding : "UTF-8";
  }

  private static XMLEventReader fallbackReader(final Element fallback) throws XMLStreamException {
    final EventAccumulator accumulator = new EventAccumulator();

    addElement(fallback, accumulator);

    final List<XMLEvent> events = accumulator.getEvents();

    events.remove(0); // Start fallback.
    events.remove(events.size() - 1); // End fallback.

    return new ListEventReader(events);
  }

  private static byte[] getBytes(final InputStream in) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    tryToDoRethrow(() -> copy(in, out));

    return out.toByteArray();
  }

  private static Optional<String> getDeclaredEncoding(final byte[] bytes) {
    return getProcessingInstruction(bytes)
        .map(PI_ENCODING::matcher)
        .filter(Matcher::find)
        .map(matcher -> matcher.group(1));
  }

  private static Optional<String> getProcessingInstruction(final byte[] bytes) {
    if (bytes.length < 13 || bytes[0] != (byte) '<' || bytes[1] != (byte) '?') {
      return empty();
    }

    return Optional.of(
            takeWhile(0, i -> i + 1, i -> i < bytes.length && bytes[i] != (byte) '>')
                .reduce(new StringBuilder(), StringBuilder::append, (b1, b2) -> b1)
                .toString())
        .filter(pi -> pi.endsWith("?"))
        .map(pi -> pi + ">");
  }

  private static boolean isXInclude(final XMLEvent event) {
    return event != null
        && event.isStartElement()
        && XINCLUDE.equals(event.asStartElement().getName().getNamespaceURI())
        && "include".equals(event.asStartElement().getName().getLocalPart());
  }

  @Override
  public boolean hasNext() {
    if (reader != null && reader.hasNext()) {
      return true;
    }

    reader = null;

    if (!super.hasNext()) {
      return false;
    }

    if (isXInclude(tryToGetRethrow(super::peek).orElse(null))) {
      reader =
          tryToGetRethrow(
                  () -> include(accumulate(getParent(), super.nextEvent().asStartElement())))
              .orElse(null);

      return hasNext();
    }

    return true;
  }

  private XMLEventReader include(final Element element) throws XMLStreamException {
    final Optional<Element> fallback =
        stream(element.getChildNodes())
            .filter(
                n -> XINCLUDE.equals(n.getNamespaceURI()) && "fallback".equals(n.getLocalName()))
            .map(n -> (Element) n)
            .findFirst();
    final String href = element.getAttribute("href");

    if ("".equals(href)) {
      return fallback.flatMap(f -> tryToGetRethrow(() -> fallbackReader(f))).orElse(null);
    }

    try {
      final String parse = element.getAttribute("parse");
      final URL url = new URL(new URL(tracker.getBaseURI()), href);

      if (url.getRef() != null
          || (!"".equals(parse) && !"xml".equals(parse) && !"text".equals(parse))) {
        throw new XMLStreamException("Invalid XInclude element.");
      }

      if (included.contains(url.toString())) {
        throw new XMLStreamException("Recursive XInclude element.");
      }

      return openUrl(url, href, "".equals(parse) ? "xml" : parse, element.getAttribute("encoding"))
          .orElse(null);
    } catch (IOException e) {
      return fallback
          .flatMap(f -> tryToGetRethrow(() -> fallbackReader(f)))
          .orElseThrow(() -> new XMLStreamException("No fallback in XInclude element."));
    }
  }

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    if (reader != null) {
      return reader.nextEvent();
    }

    final XMLEvent event = super.nextEvent();

    tracker.add(event);

    return event;
  }

  private Optional<XMLEventReader> openUrl(
      final URL url, final String href, final String parse, final String encoding) {
    return "xml".equals(parse)
        ? openUrlXml(url, href)
        : tryToGetRethrow(() -> openUrlStream(url, href, encoding))
            .flatMap(pair -> pair)
            .map(
                pair ->
                    new ListEventReader(
                        list(
                            eventFactory.createCharacters(
                                tryToGetRethrow(() -> new String(pair.first, pair.second))
                                    .orElse("")))));
  }

  private Optional<Pair<byte[], String>> openUrlStream(
      final URL url, final String href, final String encoding)
      throws IOException, XMLStreamException {
    if (factory.getXMLResolver() != null) {
      return resolveEntity(href)
          .map(XIncludeEventReader::getBytes)
          .map(bytes -> pair(bytes, charset(encoding)));
    }

    final URLConnection connection = url.openConnection();
    final String mimeType = connection.getContentType();
    final Function<byte[], String> tryXml =
        bytes ->
            isXml(mimeType)
                ? getDeclaredEncoding(bytes).orElse(charset(encoding))
                : charset(encoding);

    return !isXml(mimeType) && !mimeType.startsWith("text")
        ? empty()
        : Optional.of(getBytes(connection.getInputStream()))
            .map(
                bytes ->
                    pair(
                        bytes,
                        getParameter(mimeType, "charset").orElseGet(() -> tryXml.apply(bytes))));
  }

  private Optional<XMLEventReader> openUrlXml(final URL url, final String href) {
    final Set<String> copy = new HashSet<>(included);

    copy.add(tracker.getBaseURI());

    return tryToGetRethrow(
        () ->
            new SetXIncludeSourceReader(
                href,
                new GobbleDocumentEventsReader(
                    new XIncludeEventReader(
                        url.toString(),
                        factory,
                        copy,
                        new SetBaseURIEventReader(
                            url.toString(),
                            factory.getXMLResolver() != null
                                ? resolveXMLEntity(href).orElse(null)
                                : factory.createXMLEventReader(
                                    url.toString(), url.openStream()))))));
  }

  @Override
  public XMLEvent peek() {
    return hasNext()
        ? Optional.ofNullable(reader)
            .flatMap(r -> tryToGetRethrow(r::peek))
            .orElseGet(() -> tryToGetRethrow(super::peek).orElse(null))
        : null;
  }

  private Optional<InputStream> resolveEntity(final String href) throws XMLStreamException {
    final Object result =
        factory.getXMLResolver().resolveEntity(null, href, tracker.getBaseURI(), null);
    final Supplier<InputStream> trySource =
        () -> result instanceof StreamSource ? ((StreamSource) result).getInputStream() : null;

    return Optional.ofNullable(
        result instanceof InputStream ? (InputStream) result : trySource.get());
  }

  private Optional<XMLEventReader> resolveXMLEntity(final String href) throws XMLStreamException {
    final Object result =
        factory.getXMLResolver().resolveEntity(null, href, tracker.getBaseURI(), null);
    final Supplier<XMLEventReader> tryInputStream =
        () ->
            result instanceof InputStream
                ? tryToGetRethrow(() -> factory.createXMLEventReader((InputStream) result))
                    .orElse(null)
                : null;
    final Supplier<XMLEventReader> trySource =
        () ->
            result instanceof Source
                ? tryToGetRethrow(() -> factory.createXMLEventReader((Source) result)).orElse(null)
                : tryInputStream.get();
    final Supplier<XMLEventReader> tryStream =
        () ->
            result instanceof XMLStreamReader
                ? new StreamEventReader((XMLStreamReader) result)
                : trySource.get();

    return Optional.ofNullable(
        result instanceof XMLEventReader ? (XMLEventReader) result : tryStream.get());
  }

  private static class SetXIncludeSourceReader extends EventReaderDelegateBase {
    private final String href;
    private boolean seen;

    private SetXIncludeSourceReader(final String href, final XMLEventReader parent) {
      super(parent);
      this.href = href;
    }

    public XMLEvent nextEvent() throws XMLStreamException {
      final XMLEvent event = super.nextEvent();

      if (!seen && event.isStartElement()) {
        seen = true;

        return Util.setNamespace(
            Util.setAttribute(
                event.asStartElement(), new QName(XINCLUDE, "xinclude-href", "xi"), href),
            "xi",
            XINCLUDE);
      }

      return event;
    }
  }
}
