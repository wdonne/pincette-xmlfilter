package net.pincette.xml.stream;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

/**
 * An XMLStreamWriter wrapper around an XMLEventWriter.
 *
 * @author Werner Donn\u00e9
 */
public class EventStreamWriter implements XMLStreamWriter {
  private final Deque<Element> elements = new ArrayDeque<>();
  private final XMLEventFactory factory = XMLEventFactory.newFactory();
  private Element pendingElement;
  private XMLEventWriter writer;

  public EventStreamWriter(final XMLEventWriter writer) {
    this.writer = writer;
  }

  public void close() throws XMLStreamException {
    writer.close();
  }

  public void flush() throws XMLStreamException {
    writer.flush();
  }

  private void flushPendingStartElement() throws XMLStreamException {
    if (pendingElement != null) {
      writer.add(
          factory.createStartElement(
              pendingElement.name,
              pendingElement.attributes.iterator(),
              pendingElement.namespaces.iterator()));

      if (pendingElement.empty) {
        writer.add(
            factory.createEndElement(pendingElement.name, pendingElement.namespaces.iterator()));
      }

      pendingElement = null;
    }
  }

  public NamespaceContext getNamespaceContext() {
    return writer.getNamespaceContext();
  }

  public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
    writer.setNamespaceContext(context);
  }

  public String getPrefix(final String uri) throws XMLStreamException {
    return writer.getPrefix(uri);
  }

  public Object getProperty(final String name) {
    return null;
  }

  public void setDefaultNamespace(final String uri) throws XMLStreamException {
    writer.setDefaultNamespace(uri);
  }

  public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
    writer.setPrefix(prefix, uri);
  }

  public void writeAttribute(final String localName, final String value) {
    pendingElement.attributes.add(factory.createAttribute(localName, value));
  }

  public void writeAttribute(
      final String namespaceURI, final String localName, final String value) {
    pendingElement.attributes.add(factory.createAttribute("", namespaceURI, localName, value));
  }

  public void writeAttribute(
      final String prefix, final String namespaceURI, final String localName, final String value) {
    pendingElement.attributes.add(factory.createAttribute(prefix, namespaceURI, localName, value));
  }

  public void writeCData(final String data) throws XMLStreamException {
    flushPendingStartElement();
    writer.add(factory.createCData(data));
  }

  public void writeCharacters(final char[] text, final int start, final int len)
      throws XMLStreamException {
    flushPendingStartElement();
    writer.add(factory.createCharacters(new String(text, start, len)));
  }

  public void writeCharacters(final String text) throws XMLStreamException {
    flushPendingStartElement();
    writer.add(factory.createCharacters(text));
  }

  public void writeComment(final String data) throws XMLStreamException {
    flushPendingStartElement();
    writer.add(factory.createComment(data));
  }

  public void writeDefaultNamespace(final String namespaceURI) {
    pendingElement.namespaces.add(factory.createNamespace(namespaceURI));
  }

  public void writeDTD(final String dtd) throws XMLStreamException {
    writer.add(factory.createDTD(dtd));
  }

  public void writeEmptyElement(final String localName) throws XMLStreamException {
    flushPendingStartElement();
    pendingElement = new Element(new QName(localName));
    pendingElement.empty = true;
  }

  public void writeEmptyElement(final String namespaceURI, final String localName)
      throws XMLStreamException {
    flushPendingStartElement();
    pendingElement = new Element(new QName(namespaceURI, localName));
    pendingElement.empty = true;
  }

  public void writeEmptyElement(
      final String prefix, final String localName, final String namespaceURI)
      throws XMLStreamException {
    flushPendingStartElement();
    pendingElement = new Element(new QName(namespaceURI, localName, prefix));
    pendingElement.empty = true;
  }

  public void writeEndDocument() throws XMLStreamException {
    writer.add(factory.createEndDocument());
  }

  public void writeEndElement() throws XMLStreamException {
    flushPendingStartElement();

    final Element element = elements.pop();

    writer.add(factory.createEndElement(element.name, element.namespaces.iterator()));
  }

  public void writeEntityRef(final String name) throws XMLStreamException {
    flushPendingStartElement();
    writer.add(factory.createEntityReference(name, null));
  }

  public void writeNamespace(final String prefix, final String namespaceURI) {
    pendingElement.namespaces.add(factory.createNamespace(prefix, namespaceURI));
  }

  public void writeProcessingInstruction(final String target) throws XMLStreamException {
    flushPendingStartElement();
    writer.add(factory.createProcessingInstruction(target, null));
  }

  public void writeProcessingInstruction(final String target, final String data)
      throws XMLStreamException {
    flushPendingStartElement();
    writer.add(factory.createProcessingInstruction(target, data));
  }

  public void writeStartDocument() throws XMLStreamException {
    writer.add(factory.createStartDocument());
  }

  public void writeStartDocument(final String version) throws XMLStreamException {
    writer.add(factory.createStartDocument("UTF-8", version));
  }

  public void writeStartDocument(final String encoding, final String version)
      throws XMLStreamException {
    writer.add(factory.createStartDocument(encoding, version));
  }

  public void writeStartElement(final String localName) throws XMLStreamException {
    flushPendingStartElement();
    pendingElement = new Element(new QName(localName));
    elements.push(pendingElement);
  }

  public void writeStartElement(final String namespaceURI, final String localName)
      throws XMLStreamException {
    flushPendingStartElement();
    pendingElement = new Element(new QName(namespaceURI, localName));
    elements.push(pendingElement);
  }

  public void writeStartElement(
      final String prefix, final String localName, final String namespaceURI)
      throws XMLStreamException {
    flushPendingStartElement();
    pendingElement = new Element(new QName(namespaceURI, localName, prefix));
    elements.push(pendingElement);
  }

  private static class Element {
    private boolean empty;
    private final QName name;
    private final List<Attribute> attributes = new ArrayList<>();
    private final List<Namespace> namespaces = new ArrayList<>();

    private Element(final QName name) {
      this.name = name;
    }
  }
}
