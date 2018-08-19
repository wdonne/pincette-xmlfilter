package net.pincette.xml.stream;

import static net.pincette.util.StreamUtil.stream;
import static net.pincette.xml.stream.Util.events;
import static net.pincette.xml.stream.Util.newDocument;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Creates a DOM tree.
 *
 * @author Werner Donn\u00e9
 */
public class DOMEventWriter implements XMLEventWriter {
  private Document document;
  private Deque<Node> elements = new ArrayDeque<>();

  public DOMEventWriter() {
    this(null);
  }

  public DOMEventWriter(final Document document) {
    this.document = document != null ? document : newDocument();
  }

  private static Element addAttribute(final Element element, final Attribute attribute) {
    element.setAttributeNS(
        "".equals(attribute.getName().getNamespaceURI())
            ? null
            : attribute.getName().getNamespaceURI(),
        (attribute.getName().getPrefix() == null || "".equals(attribute.getName().getPrefix())
                ? ""
                : (attribute.getName().getPrefix() + ":"))
            + attribute.getName().getLocalPart(),
        attribute.getValue());

    return element;
  }

  private static Element addNamespace(final Element element, final Namespace namespace) {
    element.setAttributeNS(
        XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
        XMLConstants.XMLNS_ATTRIBUTE
            + ("".equals(namespace.getPrefix()) ? "" : (":" + namespace.getPrefix())),
        namespace.getNamespaceURI());

    return element;
  }

  public void add(final XMLEvent event) {
    switch (event.getEventType()) {
      case XMLEvent.ATTRIBUTE:
        current()
            .filter(c -> c instanceof Element)
            .map(c -> (Element) c)
            .ifPresent(c -> addAttribute(c, (Attribute) event));
        break;

      case XMLEvent.CDATA:
        current()
            .ifPresent(
                c -> c.appendChild(document.createCDATASection(event.asCharacters().getData())));
        break;

      case XMLEvent.CHARACTERS:
      case XMLEvent.SPACE:
        current()
            .filter(c -> c instanceof Element)
            .ifPresent(c -> c.appendChild(document.createTextNode(event.asCharacters().getData())));
        break;

      case XMLEvent.COMMENT:
        current()
            .ifPresent(c -> c.appendChild(document.createComment(((Comment) event).getText())));
        break;

      case XMLEvent.END_DOCUMENT:
      case XMLEvent.END_ELEMENT:
        elements.pop();
        break;

      case XMLEvent.ENTITY_REFERENCE:
        current()
            .ifPresent(
                c ->
                    c.appendChild(
                        document.createEntityReference(((EntityReference) event).getName())));
        break;

      case XMLEvent.NAMESPACE:
        current()
            .filter(c -> c instanceof Element)
            .map(c -> (Element) c)
            .ifPresent(c -> addNamespace(c, (Namespace) event));
        break;

      case XMLEvent.PROCESSING_INSTRUCTION:
        current()
            .ifPresent(
                c ->
                    c.appendChild(
                        document.createProcessingInstruction(
                            ((ProcessingInstruction) event).getTarget(),
                            ((ProcessingInstruction) event).getData())));
        break;

      case XMLEvent.START_DOCUMENT:
        elements.push(document);
        break;

      case XMLEvent.START_ELEMENT:
        current().ifPresent(c -> elements.push(addElement(c, event.asStartElement())));
        break;

      default:
        break;
    }
  }

  public void add(final XMLEventReader reader) throws XMLStreamException {
    events(reader).forEach(this::add);
    reader.close();
  }

  private Node addElement(final Node node, final StartElement element) {
    final Element result =
        document.createElementNS(
            "".equals(element.getName().getNamespaceURI())
                ? null
                : element.getName().getNamespaceURI(),
            ("".equals(element.getName().getPrefix()) ? "" : (element.getName().getPrefix() + ":"))
                + element.getName().getLocalPart());

    stream((Iterator<Namespace>) element.getNamespaces()).forEach(n -> addNamespace(result, n));
    stream((Iterator<Attribute>) element.getAttributes()).forEach(a -> addAttribute(result, a));

    return (node != null ? node : document).appendChild(result);
  }

  public void close() throws XMLStreamException {/* No resources. */}

  public boolean complete() {
    return elements.isEmpty();
  }

  private Optional<Node> current() {
    return Optional.ofNullable(elements.isEmpty() ? null : elements.peek());
  }

  public void flush() {/* No resources. */}

  public Document getDocument() {
    return document;
  }

  public NamespaceContext getNamespaceContext() {
    return null;
  }

  public void setNamespaceContext(final NamespaceContext context) {/* Not supported. */}

  public String getPrefix(final String uri) {
    return null;
  }

  public void setDefaultNamespace(final String uri) {/* Not supported. */}

  public void setPrefix(final String prefix, final String uri) {/* Not supported. */}
}
