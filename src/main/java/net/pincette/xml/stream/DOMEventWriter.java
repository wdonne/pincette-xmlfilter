package net.pincette.xml.stream;

import static javax.xml.stream.XMLStreamConstants.ATTRIBUTE;
import static javax.xml.stream.XMLStreamConstants.CDATA;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.COMMENT;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.ENTITY_REFERENCE;
import static javax.xml.stream.XMLStreamConstants.NAMESPACE;
import static javax.xml.stream.XMLStreamConstants.PROCESSING_INSTRUCTION;
import static javax.xml.stream.XMLStreamConstants.SPACE;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static net.pincette.util.StreamUtil.stream;
import static net.pincette.xml.stream.Util.events;
import static net.pincette.xml.stream.Util.newDocument;

import java.util.ArrayDeque;
import java.util.Deque;
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
 * @author Werner Donné
 */
public class DOMEventWriter implements XMLEventWriter {
  private final Document document;
  private final Deque<Node> elements = new ArrayDeque<>();

  public DOMEventWriter() {
    this(null);
  }

  public DOMEventWriter(final Document document) {
    this.document = document != null ? document : newDocument();
  }

  private static void addAttribute(final Element element, final Attribute attribute) {
    element.setAttributeNS(
        "".equals(attribute.getName().getNamespaceURI())
            ? null
            : attribute.getName().getNamespaceURI(),
        (attribute.getName().getPrefix() == null || "".equals(attribute.getName().getPrefix())
                ? ""
                : (attribute.getName().getPrefix() + ":"))
            + attribute.getName().getLocalPart(),
        attribute.getValue());
  }

  private static void addNamespace(final Element element, final Namespace namespace) {
    element.setAttributeNS(
        XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
        XMLConstants.XMLNS_ATTRIBUTE
            + ("".equals(namespace.getPrefix()) ? "" : (":" + namespace.getPrefix())),
        namespace.getNamespaceURI());
  }

  public void add(final XMLEvent event) {
    switch (event.getEventType()) {
      case ATTRIBUTE:
        current()
            .filter(Element.class::isInstance)
            .map(c -> (Element) c)
            .ifPresent(c -> addAttribute(c, (Attribute) event));
        break;

      case CDATA:
        current()
            .ifPresent(
                c -> c.appendChild(document.createCDATASection(event.asCharacters().getData())));
        break;

      case CHARACTERS, SPACE:
        current()
            .filter(Element.class::isInstance)
            .ifPresent(c -> c.appendChild(document.createTextNode(event.asCharacters().getData())));
        break;

      case COMMENT:
        current()
            .ifPresent(c -> c.appendChild(document.createComment(((Comment) event).getText())));
        break;

      case END_DOCUMENT, END_ELEMENT:
        elements.pop();
        break;

      case ENTITY_REFERENCE:
        current()
            .ifPresent(
                c ->
                    c.appendChild(
                        document.createEntityReference(((EntityReference) event).getName())));
        break;

      case NAMESPACE:
        current()
            .filter(Element.class::isInstance)
            .map(c -> (Element) c)
            .ifPresent(c -> addNamespace(c, (Namespace) event));
        break;

      case PROCESSING_INSTRUCTION:
        current()
            .ifPresent(
                c ->
                    c.appendChild(
                        document.createProcessingInstruction(
                            ((ProcessingInstruction) event).getTarget(),
                            ((ProcessingInstruction) event).getData())));
        break;

      case START_DOCUMENT:
        elements.push(document);
        break;

      case START_ELEMENT:
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

    stream(element.getNamespaces()).forEach(n -> addNamespace(result, n));
    stream(element.getAttributes()).forEach(a -> addAttribute(result, a));

    return (node != null ? node : document).appendChild(result);
  }

  public void close() throws XMLStreamException {
    /* No resources. */
  }

  public boolean complete() {
    return elements.isEmpty();
  }

  private Optional<Node> current() {
    return Optional.ofNullable(elements.isEmpty() ? null : elements.peek());
  }

  public void flush() {
    /* No resources. */
  }

  public Document getDocument() {
    return document;
  }

  public NamespaceContext getNamespaceContext() {
    return null;
  }

  public void setNamespaceContext(final NamespaceContext context) {
    /* Not supported. */
  }

  public String getPrefix(final String uri) {
    return null;
  }

  public void setDefaultNamespace(final String uri) {
    /* Not supported. */
  }

  public void setPrefix(final String prefix, final String uri) {
    /* Not supported. */
  }
}
