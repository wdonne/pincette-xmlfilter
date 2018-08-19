package net.pincette.xml.stream;

import static net.pincette.xml.Util.children;
import static net.pincette.xml.stream.Util.clearNode;
import static net.pincette.xml.stream.Util.createEndElement;
import static net.pincette.xml.stream.Util.createStartElement;

import java.util.NoSuchElementException;
import java.util.function.Supplier;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.w3c.dom.CDATASection;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

/**
 * Generates events from a DOM-node. If the <code>destruct</code> parameter is turned on written
 * elements will be removed from the DOM.
 *
 * @author Werner Donn\u00e9
 */
public class DOMEventReader implements XMLEventReader {
  private final XMLEventFactory factory = XMLEventFactory.newInstance();
  private final Node node;
  private boolean destruct;
  private Position end;
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

  private static boolean isAllowedInElementOnly(final Position position) {
    return position != null
        && (position.node instanceof Element
            || (position.node instanceof CharacterData
                && position.node.getTextContent().trim().length() == 0));
  }

  public void close() throws XMLStreamException {
    position = end;
  }

  private XMLEvent createDTD(final DocumentType documentType) {
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

  private XMLEvent createEvent(final Position position) {
    final Supplier<XMLEvent> docType =
        () ->
            position.node instanceof DocumentType ? createDTD((DocumentType) position.node) : null;
    final Supplier<XMLEvent> document =
        () -> position.endDocument ? factory.createEndDocument() : factory.createStartDocument();
    final Supplier<XMLEvent> element =
        () ->
            position.endElement
                ? createEndElement((Element) position.node, factory)
                : createStartElement((Element) position.node, factory);
    final Supplier<XMLEvent> entityRefOr =
        () ->
            position.node instanceof EntityReference
                ? Util.createEntityReference((EntityReference) position.node, factory)
                : docType.get();
    final Supplier<XMLEvent> piOr =
        () ->
            position.node instanceof ProcessingInstruction
                ? factory.createProcessingInstruction(
                    ((ProcessingInstruction) position.node).getTarget(),
                    ((ProcessingInstruction) position.node).getData())
                : entityRefOr.get();
    final Supplier<XMLEvent> textOr =
        () ->
            position.node instanceof Text
                ? factory.createCharacters(((Text) position.node).getData())
                : piOr.get();
    final Supplier<XMLEvent> commentOr =
        () ->
            position.node instanceof Comment
                ? factory.createComment(((CharacterData) position.node).getData())
                : textOr.get();
    final Supplier<XMLEvent> cdataOr =
        () ->
            position.node instanceof CDATASection
                ? factory.createCData(((CharacterData) position.node).getData())
                : commentOr.get();
    final Supplier<XMLEvent> elementOr =
        () -> position.node instanceof Element ? element.get() : cdataOr.get();

    return position.node instanceof Document ? document.get() : elementOr.get();
  }

  public String getElementText() throws XMLStreamException {
    if (position == null || !(position.node instanceof Element) || position.endElement) {
      throw new XMLStreamException("Not at START_ELEMENT.");
    }

    if (!children(position.node).allMatch(n -> n instanceof CharacterData)) {
      throw new XMLStreamException("Not a text-only element.");
    }

    return position.node.getTextContent();
  }

  private Position getNextPosition() {
    final Supplier<Position> firstChild =
        () ->
            position.node.getFirstChild() != null
                ? new Position(position.node.getFirstChild(), false, false)
                : new Position(position.node, true, false);
    final Supplier<Position> parentDoc =
        () ->
            position.node.getParentNode() instanceof Document
                ? new Position(position.node.getParentNode(), false, true)
                : new Position(position.node.getParentNode(), true, false);
    final Supplier<Position> parentOr =
        () -> position.node.getParentNode() != null ? parentDoc.get() : null;
    final Supplier<Position> nextOr =
        () ->
            position.node.getNextSibling() != null
                ? new Position(position.node.getNextSibling(), false, false)
                : parentOr.get();
    final Supplier<Position> inElementOrDocumentOr =
        () ->
            (position.node instanceof Element && !position.endElement)
                    || (position.node instanceof Document && !position.endDocument)
                ? firstChild.get()
                : nextOr.get();
    final Supplier<Position> docTypeOr =
        () ->
            position.node instanceof DocumentType
                ? new Position(position.node.getOwnerDocument().getDocumentElement(), false, false)
                : inElementOrDocumentOr.get();
    final Supplier<Position> documentOr =
        () ->
            position.node instanceof Document
                    && !position.endDocument
                    && ((Document) position.node).getDoctype() != null
                ? new Position(((Document) position.node).getDoctype(), false, false)
                : docTypeOr.get();

    return position == null ? new Position(node, false, false) : documentOr.get();
  }

  public Object getProperty(final String name) {
    return null;
  }

  public boolean hasNext() {
    return getNextPosition() != null;
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

    position = getNextPosition();

    if (destruct && position.node instanceof Element && position.endElement) {
      clearNode(position.node);
    }

    return createEvent(position);
  }

  public XMLEvent nextTag() throws XMLStreamException {
    if (!isAllowedInElementOnly(position)) {
      throw new XMLStreamException("Not a element-only element.");
    }

    while ((position = getNextPosition()) != null) {
      if (!isAllowedInElementOnly(position)) {
        throw new XMLStreamException("Not a element-only element.");
      }

      if (position.node instanceof Element) {
        return createEvent(position);
      }
    }

    throw new XMLStreamException("No next tag.");
  }

  public XMLEvent peek() {
    Position next = getNextPosition();

    return next != null ? createEvent(next) : null;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  private static class Position {

    private final boolean endDocument;
    private final boolean endElement;
    private final Node node;

    private Position(final Node node, final boolean endElement, final boolean endDocument) {
      this.node = node;
      this.endElement = endElement;
      this.endDocument = endDocument;
    }

    public boolean equals(final Object o) {
      return o instanceof Position
          && node == ((Position) o).node
          && endElement == ((Position) o).endElement
          && endDocument == ((Position) o).endDocument;
    }

    public int hashCode() {
      return (node != null ? node.hashCode() : 0) + (endElement ? 1 : 0) + (endDocument ? 1 : 0);
    }
  }
}
