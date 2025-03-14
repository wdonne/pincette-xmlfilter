package net.pincette.xml.stream;

import static java.util.Optional.ofNullable;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static net.pincette.util.StreamUtil.stream;

import java.util.ArrayDeque;
import java.util.Deque;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A ContentHandler wrapper.
 *
 * @author Werner Donné
 */
public class ContentHandlerEventWriter implements XMLEventWriter {
  private final Deque<Element> elements = new ArrayDeque<>();
  private ContentHandler handler;
  private NamespaceContext namespaceContext;
  private boolean pendingElement = false;

  public ContentHandlerEventWriter() {}

  public ContentHandlerEventWriter(final ContentHandler handler) {
    setContentHandler(handler);
  }

  public void add(final XMLEvent event) throws XMLStreamException {
    switch (event.getEventType()) {
      case XMLStreamConstants.ATTRIBUTE:
        writeAttribute((Attribute) event);
        break;
      case XMLStreamConstants.CDATA, XMLStreamConstants.SPACE, XMLStreamConstants.CHARACTERS:
        writeCharacters((Characters) event);
        break;
      case XMLStreamConstants.COMMENT:
        writeComment();
        break;
      case XMLStreamConstants.DTD:
        break;
      case XMLStreamConstants.END_DOCUMENT:
        writeEndDocument();
        break;
      case XMLStreamConstants.END_ELEMENT:
        writeEndElement();
        break;
      case XMLStreamConstants.ENTITY_DECLARATION:
        break;
      case XMLStreamConstants.ENTITY_REFERENCE:
        writeEntityReference((EntityReference) event);
        break;
      case XMLStreamConstants.NAMESPACE:
        writeNamespace((Namespace) event);
        break;
      case XMLStreamConstants.NOTATION_DECLARATION:
        break;
      case XMLStreamConstants.PROCESSING_INSTRUCTION:
        writeProcessingInstruction((ProcessingInstruction) event);
        break;
      case XMLStreamConstants.START_DOCUMENT:
        writeStartDocument();
        break;
      case XMLStreamConstants.START_ELEMENT:
        writeStartElement((StartElement) event);
        break;
      default:
        break;
    }
  }

  public void add(final XMLEventReader reader) throws XMLStreamException {
    while (reader.hasNext()) {
      add(reader.nextEvent());
    }
  }

  public void close() throws XMLStreamException {
    // Nothing to do.
  }

  public void flush() throws XMLStreamException {
    // Nothing to do.
  }

  private void flushPendingElement() throws SAXException {
    if (pendingElement && !elements.isEmpty()) {
      pendingElement = false;

      final Element element = elements.peek();

      handler.startElement(
          element.namespaceURI, element.localName, element.qName, element.attributes);
    }
  }

  public ContentHandler getContentHandler() {
    return handler;
  }

  public NamespaceContext getNamespaceContext() {
    return namespaceContext;
  }

  public String getPrefix(final String uri) throws XMLStreamException {
    return null;
  }

  public Object getProperty(final String name) {
    if (!"javax.xml.stream.isPrefixDefaulting".equals(name)) {
      throw new IllegalArgumentException("Property " + name + " is not supported.");
    }

    return Boolean.FALSE;
  }

  private String getQName(final QName name) {
    return ("".equals(name.getPrefix()) ? "" : (name.getPrefix() + ":")) + name.getLocalPart();
  }

  public void setContentHandler(final ContentHandler handler) {
    this.handler = handler;
  }

  public void setDefaultNamespace(final String uri) {
    // Nothing to do.
  }

  public void setNamespaceContext(final NamespaceContext context) {
    namespaceContext = context;
  }

  public void setPrefix(final String prefix, final String uri) {
    // Nothing to do.
  }

  private void writeAttribute(final Attribute event) {
    ofNullable(elements.peek())
        .ifPresent(
            e ->
                e.attributes.addAttribute(
                    event.getName().getNamespaceURI(),
                    event.getName().getLocalPart(),
                    getQName(event.getName()),
                    event.getDTDType(),
                    event.getValue()));
  }

  private void writeCharacters(final Characters event) throws XMLStreamException {
    writeCharacters(event.getData());
  }

  private void writeCharacters(final String s) throws XMLStreamException {
    try {
      flushPendingElement();

      if (!elements.isEmpty()) {
        handler.characters(s.toCharArray(), 0, s.length());
      }
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  private void writeComment() throws XMLStreamException {
    try {
      flushPendingElement();
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  private void writeEndDocument() throws XMLStreamException {
    try {
      handler.endDocument();
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  private void writeEndElement() throws XMLStreamException {
    try {
      flushPendingElement();

      final Element element = elements.pop();

      handler.endElement(element.namespaceURI, element.localName, element.qName);
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  private void writeEntityReference(final EntityReference event) throws XMLStreamException {
    writeCharacters("&" + event.getName() + ";");
  }

  private void writeNamespace(final Namespace event) {
    if (event.getPrefix() == null
        || event.getPrefix().isEmpty()
        || event.getPrefix().equals(XMLNS_ATTRIBUTE)) {
      ofNullable(elements.peek())
          .ifPresent(
              e ->
                  e.attributes.addAttribute(
                      XMLNS_ATTRIBUTE_NS_URI,
                      XMLNS_ATTRIBUTE,
                      XMLNS_ATTRIBUTE,
                      "CDATA",
                      event.getNamespaceURI()));
    } else {
      ofNullable(elements.peek())
          .ifPresent(
              e ->
                  e.attributes.addAttribute(
                      XMLNS_ATTRIBUTE_NS_URI,
                      event.getPrefix(),
                      XMLNS_ATTRIBUTE + ":" + event.getPrefix(),
                      "CDATA",
                      event.getNamespaceURI()));
    }
  }

  private void writeProcessingInstruction(final ProcessingInstruction event)
      throws XMLStreamException {
    try {
      flushPendingElement();
      handler.processingInstruction(event.getTarget(), event.getData());
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  private void writeStartDocument() throws XMLStreamException {
    try {
      handler.startDocument();
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  private void writeStartElement(final StartElement event) throws XMLStreamException {
    try {
      flushPendingElement();

      elements.push(
          new Element(
              event.getName().getNamespaceURI(),
              event.getName().getLocalPart(),
              getQName(event.getName())));

      pendingElement = true;
      stream(event.getNamespaces()).forEach(this::writeNamespace);
      stream(event.getAttributes()).forEach(this::writeAttribute);
    } catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  private static class Element {
    private final AttributesImpl attributes = new AttributesImpl();
    private final String localName;
    private final String namespaceURI;
    private final String qName;

    private Element(final String namespaceURI, final String localName, final String qName) {
      this.namespaceURI = namespaceURI;
      this.localName = localName;
      this.qName = qName;
    }
  }
}
