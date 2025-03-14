package net.pincette.xml.sax;

import static java.util.Optional.ofNullable;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.Util.secureDocumentBuilderFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * With this class, a SAX stream can be accumulated in a DOM document. An instance of it can be
 * reused. The client must call <code>startDocument</code> and <code>endDocument</code>.
 *
 * @author Werner Donné
 */
public class Accumulator extends XMLFilterImpl {
  private static final DocumentBuilder documentBuilder = createDocumentBuilder();

  private Node currentNode = null;
  private Document document = null;
  private final Deque<Map<String, String>> prefixMappings = new ArrayDeque<>();
  private Result result = null;

  public Accumulator() {
    this(null, (Result) null);
  }

  public Accumulator(final XMLReader parent) {
    this(null, null, parent);
  }

  public Accumulator(final Result result) {
    this(null, result);
  }

  public Accumulator(final Result result, final XMLReader parent) {
    this(null, result, parent);
  }

  public Accumulator(final Document document) {
    this.document = document;
  }

  public Accumulator(final Document document, final XMLReader parent) {
    super(parent);
    this.document = document;
  }

  public Accumulator(final Document document, final Result result) {
    this.document = document;
    this.result = result;
  }

  public Accumulator(final Document document, final Result result, final XMLReader parent) {
    super(parent);
    this.document = document;
    this.result = result;
  }

  private static DocumentBuilder createDocumentBuilder() {
    return tryToGetRethrow(() -> secureDocumentBuilderFactory().newDocumentBuilder()).orElse(null);
  }

  /**
   * This installs an accumulator after <code>filter</code>. You would call it in <code>startElement
   * </code>, before you let the event go through.
   */
  public static void postAccumulate(final XMLFilter filter, final ProcessElement process) {
    final ContentHandler handler = filter.getContentHandler();
    final Accumulator accumulator =
        new Accumulator(
            documentBuilder.newDocument(),
            acc -> {
              filter.setContentHandler(ofNullable(handler).orElseGet(XMLFilterImpl::new));
              process.process(acc.getDocument().getDocumentElement(), filter);
              acc.endDocument();
            });

    filter.setContentHandler(accumulator);
    accumulator.setParent(filter);
    accumulator.startDocument();
  }

  /**
   * This installs an accumulator before <code>filter</code>. You would call it in <code>
   * startElement</code>, and initialize it with the incoming event, which you don't let go through.
   */
  public static void preAccumulate(
      final String namespaceURI,
      final String localName,
      final String qName,
      final Attributes atts,
      final XMLFilter filter,
      final ProcessElement process) {
    if (filter.getParent() == null) {
      return;
    }

    final ContentHandler handler = filter.getParent().getContentHandler();
    final Accumulator accumulator =
        new Accumulator(
            documentBuilder.newDocument(),
            acc -> {
              process.process(acc.getDocument().getDocumentElement(), filter);
              acc.endDocument();
              filter.getParent().setContentHandler(handler);
            });

    filter.getParent().setContentHandler(accumulator);
    accumulator.setParent(filter.getParent());
    accumulator.startDocument();
    accumulator.startElement(namespaceURI, localName, qName, atts);
  }

  private static void setAttributes(final Element element, final Attributes atts) {
    for (int i = 0; i < atts.getLength(); ++i) {
      if (atts.getURI(i) == null || "".equals(atts.getURI(i))) {
        element.setAttribute(atts.getQName(i), atts.getValue(i));

        if ("ID".equals(atts.getType(i))) {
          element.setIdAttribute(atts.getQName(i), true);
        }
      } else {
        element.setAttributeNS(atts.getURI(i), atts.getQName(i), atts.getValue(i));

        if ("ID".equals(atts.getType(i))) {
          element.setIdAttributeNS(atts.getURI(i), atts.getLocalName(i), true);
        }
      }
    }
  }

  private static void setPrefixMappings(final Element element, final Map<String, String> mappings) {
    mappings.forEach((k, v) -> element.setAttribute("".equals(k) ? "xmlns" : ("xmlns:" + k), v));
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) {
    currentNode.appendChild(document.createTextNode(new String(ch, start, length)));
  }

  @Override
  public void endDocument() {
    prefixMappings.pop();
    currentNode = null;
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    prefixMappings.pop();
    currentNode = currentNode.getParentNode();

    if (result != null && currentNode == document) {
      result.report(this);
    }
  }

  @Override
  public void endPrefixMapping(final String prefix) {
    ofNullable(prefixMappings.peek()).ifPresent(m -> m.remove(prefix));
  }

  public Document getDocument() {
    return document;
  }

  @Override
  public void ignorableWhitespace(final char[] ch, final int start, final int length) {
    // Ignoring.
  }

  @Override
  public void processingInstruction(final String target, final String data) {
    currentNode.appendChild(document.createProcessingInstruction(target, data));
  }

  @Override
  public void setDocumentLocator(final Locator locator) {
    // Not needed.
  }

  @Override
  public void skippedEntity(final String name) {
    // Not needed.
  }

  @Override
  public void startDocument() {
    if (document == null) {
      document = tryToGetRethrow(documentBuilder::newDocument).orElse(null);
    } else if (document.getDocumentElement() != null) {
      document.removeChild(document.getDocumentElement());
    }

    currentNode = document;
    prefixMappings.push(new HashMap<>());
  }

  @Override
  public void startElement(
      final String namespaceURI,
      final String localName,
      final String qName,
      final Attributes atts) {
    final Element element =
        document.createElementNS("".equals(namespaceURI) ? null : namespaceURI, qName);

    setAttributes(element, atts);
    setPrefixMappings(element, ofNullable(prefixMappings.peek()).orElseGet(HashMap::new));
    prefixMappings.push(new HashMap<>());
    currentNode.appendChild(element);
    currentNode = element;
  }

  @Override
  public void startPrefixMapping(final String prefix, final String uri) {
    ofNullable(prefixMappings.peek()).ifPresent(m -> m.put(prefix, uri));
  }

  @FunctionalInterface
  public interface ProcessElement {
    void process(Element element, XMLFilter filter) throws SAXException;
  }

  @FunctionalInterface
  public interface Result {
    void report(Accumulator accumulator) throws SAXException;
  }
}
