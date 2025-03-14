package net.pincette.xml.stream;

import static java.util.Optional.ofNullable;
import static net.pincette.xml.Util.children;
import static net.pincette.xml.stream.Util.clearNode;
import static net.pincette.xml.stream.Util.createEndElement;
import static net.pincette.xml.stream.Util.createStartElement;
import static org.w3c.dom.Node.CDATA_SECTION_NODE;
import static org.w3c.dom.Node.COMMENT_NODE;
import static org.w3c.dom.Node.DOCUMENT_NODE;
import static org.w3c.dom.Node.DOCUMENT_TYPE_NODE;
import static org.w3c.dom.Node.ELEMENT_NODE;
import static org.w3c.dom.Node.ENTITY_REFERENCE_NODE;
import static org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE;
import static org.w3c.dom.Node.TEXT_NODE;

import java.util.NoSuchElementException;
import java.util.function.Supplier;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import net.pincette.util.Cases;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

/**
 * Generates events from a DOM node. If the <code>destruct</code> parameter is turned on written
 * elements will be removed from the DOM.
 *
 * @author Werner Donné
 */
public class DOMEventReader implements XMLEventReader {
  private final XMLEventFactory factory = XMLEventFactory.newInstance();
  private final Node node;
  private final boolean destruct;
  private final Position end;
  private Position position;

  /** Doesn't destruct the DOM. */
  public DOMEventReader(final Node node) {
    this(node, false);
  }

  public DOMEventReader(final Node node, final boolean destruct) {
    this.node = node;
    this.destruct = destruct;
    end =
        node instanceof Document
            ? new Position(node, false, true)
            : new Position(node, true, false);
  }

  private static XMLEvent createDTD(
      final DocumentType documentType, final XMLEventFactory factory) {
    final Supplier<String> docType =
        () ->
            documentType.getSystemId() != null ? (" \"" + documentType.getSystemId() + "\" ") : "";

    return factory.createDTD(
        "<!DOCTYPE "
            + documentType.getName()
            + (documentType.getPublicId() != null
                ? ("PUBLIC \"" + documentType.getPublicId() + "\" " + docType.get())
                : "")
            + (documentType.getSystemId() != null && documentType.getPublicId() == null
                ? ("SYSTEM \"" + documentType.getSystemId() + "\" ")
                : "")
            + (documentType.getInternalSubset() != null
                ? ("[" + documentType.getInternalSubset() + "]")
                : "")
            + ">");
  }

  private static XMLEvent createEvent(final Position position, final XMLEventFactory factory) {
    final Supplier<XMLEvent> document =
        () -> position.endDocument ? factory.createEndDocument() : factory.createStartDocument();
    final Supplier<XMLEvent> element =
        () ->
            position.endElement
                ? createEndElement((Element) position.node, factory)
                : createStartElement((Element) position.node, factory);

    return switch (position.node.getNodeType()) {
      case DOCUMENT_NODE -> document.get();
      case ELEMENT_NODE -> element.get();
      case DOCUMENT_TYPE_NODE -> createDTD((DocumentType) position.node, factory);
      case ENTITY_REFERENCE_NODE ->
          Util.createEntityReference((EntityReference) position.node, factory);
      case PROCESSING_INSTRUCTION_NODE ->
          factory.createProcessingInstruction(
              ((ProcessingInstruction) position.node).getTarget(),
              ((ProcessingInstruction) position.node).getData());
      case TEXT_NODE -> factory.createCharacters(((Text) position.node).getData());
      case COMMENT_NODE -> factory.createComment(((CharacterData) position.node).getData());
      case CDATA_SECTION_NODE -> factory.createCData(((CharacterData) position.node).getData());
      default -> null;
    };
  }

  public static Position firstChild(final Position position) {
    return position.node.getFirstChild() != null
        ? new Position(position.node.getFirstChild(), false, false)
        : new Position(position.node, true, false);
  }

  public static Position getNextPosition(final Position position, final Node node) {
    return position == null
        ? new Position(node, false, false)
        : Cases.<Position, Position>withValue(position)
            .or(
                p -> p.node instanceof Document d && !p.endDocument && d.getDoctype() != null,
                p -> new Position(((Document) p.node).getDoctype(), false, false))
            .or(
                p -> p.node instanceof DocumentType,
                p -> new Position(p.node.getOwnerDocument().getDocumentElement(), false, false))
            .or(
                p ->
                    (p.node instanceof Element && !p.endElement)
                        || (p.node instanceof Document && !p.endDocument),
                DOMEventReader::firstChild)
            .or(
                p -> p.node.getNextSibling() != null,
                p -> new Position(p.node.getNextSibling(), false, false))
            .or(p -> p.node.getParentNode() != null, DOMEventReader::parentDoc)
            .get()
            .orElse(null);
  }

  private static boolean isAllowedInElementOnly(final Position position) {
    return position != null
        && (position.node instanceof Element
            || (position.node instanceof CharacterData
                && position.node.getTextContent().trim().isEmpty()));
  }

  private static Position parentDoc(final Position position) {
    return position.node.getParentNode() instanceof Document
        ? new Position(position.node.getParentNode(), false, true)
        : new Position(position.node.getParentNode(), true, false);
  }

  public void close() throws XMLStreamException {
    position = end;
  }

  public String getElementText() throws XMLStreamException {
    if (position == null || !(position.node instanceof Element) || position.endElement) {
      throw new XMLStreamException("Not at START_ELEMENT.");
    }

    if (!children(position.node).allMatch(CharacterData.class::isInstance)) {
      throw new XMLStreamException("Not a text-only element.");
    }

    return position.node.getTextContent();
  }

  public Position getNextPosition() {
    return getNextPosition(position, node);
  }

  public Object getProperty(final String name) {
    return null;
  }

  public boolean hasNext() {
    return getNextPosition(position, node) != null;
  }

  public Object next() {
    try {
      return nextEvent();
    } catch (Exception e) {
      throw new NoSuchElementException();
    }
  }

  public XMLEvent nextEvent() throws XMLStreamException {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    position = getNextPosition(position, node);

    if (destruct && position.node instanceof Element && position.endElement) {
      clearNode(position.node);
    }

    return createEvent(position, factory);
  }

  public XMLEvent nextTag() throws XMLStreamException {
    if (!isAllowedInElementOnly(position)) {
      throw new XMLStreamException("Not a element-only element.");
    }

    while ((position = getNextPosition(position, node)) != null) {
      if (!isAllowedInElementOnly(position)) {
        throw new XMLStreamException("Not a element-only element.");
      }

      if (position.node instanceof Element) {
        return createEvent(position, factory);
      }
    }

    throw new XMLStreamException("No next tag.");
  }

  public XMLEvent peek() {
    return ofNullable(getNextPosition(position, node))
        .map(p -> createEvent(p, factory))
        .orElse(null);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  public static class Position {
    public final Node node;
    private final boolean endDocument;
    private final boolean endElement;

    private Position(final Node node, final boolean endElement, final boolean endDocument) {
      this.node = node;
      this.endElement = endElement;
      this.endDocument = endDocument;
    }

    public boolean equals(final Object o) {
      return o instanceof Position p
          && node == p.node
          && endElement == p.endElement
          && endDocument == p.endDocument;
    }

    public int hashCode() {
      return (node != null ? node.hashCode() : 0) + (endElement ? 1 : 0) + (endDocument ? 1 : 0);
    }
  }
}
