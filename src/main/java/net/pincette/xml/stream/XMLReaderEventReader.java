package net.pincette.xml.stream;

import static java.io.File.createTempFile;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.Util.secureTransformerFactory;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import net.pincette.io.DeleteFileInputStream;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * An XMLEventReader wrapper around an XMLReader.
 *
 * @author Werner Donné
 */
public class XMLReaderEventReader implements XMLEventReader {
  private final XMLReader parser;
  private boolean closed;
  private boolean initialized;
  private InputSource input;
  private XMLEventReader reader;
  private String systemId;

  public XMLReaderEventReader(final XMLReader reader, final InputSource input) {
    parser = reader;
    this.input = input;
  }

  public XMLReaderEventReader(final XMLReader reader, final String systemId) {
    parser = reader;
    this.systemId = systemId;
  }

  private static File copy(final XMLReader parser, final InputSource input, final String systemId) {
    return tryToGetRethrow(
            () -> {
              final File tmpFile = createTempFile("XMLReaderEventReader.", ".xml");

              tmpFile.deleteOnExit();

              secureTransformerFactory()
                  .newTransformer()
                  .transform(getSource(parser, input, systemId), new StreamResult(tmpFile));

              return tmpFile;
            })
        .orElse(null);
  }

  private static XMLInputFactory getInputFactory(final XMLReader parser) {
    return parser.getEntityResolver() != null
        ? withResolver(parser.getEntityResolver())
        : XMLInputFactory.newFactory();
  }

  private static XMLEventReader getReader(
      final XMLReader parser, final InputSource input, final String systemId) {
    return tryToGetRethrow(() -> new DeleteFileInputStream(copy(parser, input, systemId)))
        .flatMap(
            in ->
                tryToGetRethrow(
                    () ->
                        Util.getCloseInputStreamEventReader(
                            getInputFactory(parser).createXMLEventReader(in), in)))
        .orElse(null);
  }

  private static SAXSource getSource(
      final XMLReader parser, final InputSource input, final String systemId) {
    final SAXSource source = new SAXSource();

    if (input != null) {
      source.setInputSource(input);
    }

    if (systemId != null) {
      source.setSystemId(systemId);
    } else {
      Optional.ofNullable(input).map(InputSource::getSystemId).ifPresent(source::setSystemId);
    }

    source.setXMLReader(parser);

    return source;
  }

  private static XMLInputFactory withResolver(final EntityResolver resolver) {
    final XMLInputFactory factory = Util.newInputFactory(false, true);

    factory.setXMLResolver(new EntityResolverWrapper(resolver));

    return factory;
  }

  public void close() throws XMLStreamException {
    if (!closed) {
      reader.close();
      closed = true;
    }
  }

  public String getElementText() throws XMLStreamException {
    return reader.getElementText();
  }

  public Object getProperty(final String name) {
    return reader.getProperty(name);
  }

  public boolean hasNext() {
    initialize();

    return reader.hasNext();
  }

  /** Should be called lazily because the parser may be modified before parsing begins. */
  private void initialize() {
    if (!initialized) {
      tryToDoRethrow(
          () -> {
            reader =
                input != null || systemId != null
                    ? getReader(parser, input, systemId)
                    : new DevNullEventReader();
            initialized = true;
          });
    }
  }

  public Object next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    return tryToGetRethrow(this::nextEvent).orElse(null);
  }

  public XMLEvent nextEvent() throws XMLStreamException {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    return reader.nextEvent();
  }

  public XMLEvent nextTag() throws XMLStreamException {
    initialize();

    return reader.nextTag();
  }

  public XMLEvent peek() throws XMLStreamException {
    initialize();

    return reader.peek();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
