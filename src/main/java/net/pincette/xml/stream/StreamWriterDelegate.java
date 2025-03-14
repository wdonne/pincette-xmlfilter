package net.pincette.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Delegates all calls to <code>delegate</code>.
 *
 * @author Werner Donné
 */
public class StreamWriterDelegate implements XMLStreamWriter {
  protected XMLStreamWriter delegate;

  public StreamWriterDelegate() {}

  public StreamWriterDelegate(final XMLStreamWriter writer) {
    setParent(writer);
  }

  public void close() throws XMLStreamException {
    delegate.close();
  }

  public void flush() throws XMLStreamException {
    delegate.flush();
  }

  public NamespaceContext getNamespaceContext() {
    return delegate.getNamespaceContext();
  }

  public XMLStreamWriter getParent() {
    return delegate;
  }

  public String getPrefix(final String uri) throws XMLStreamException {
    return delegate.getPrefix(uri);
  }

  public Object getProperty(final String name) {
    return delegate.getProperty(name);
  }

  public void setDefaultNamespace(final String uri) throws XMLStreamException {
    delegate.setDefaultNamespace(uri);
  }

  public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
    delegate.setNamespaceContext(context);
  }

  public void setParent(final XMLStreamWriter writer) {
    this.delegate = writer;
  }

  public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
    delegate.setPrefix(prefix, uri);
  }

  public void writeAttribute(final String localName, final String value) throws XMLStreamException {
    delegate.writeAttribute(localName, value);
  }

  public void writeAttribute(final String namespaceURI, final String localName, final String value)
      throws XMLStreamException {
    delegate.writeAttribute(namespaceURI, localName, value);
  }

  public void writeAttribute(
      final String prefix, final String namespaceURI, final String localName, final String value)
      throws XMLStreamException {
    delegate.writeAttribute(prefix, namespaceURI, localName, value);
  }

  public void writeCData(final String data) throws XMLStreamException {
    delegate.writeCData(data);
  }

  public void writeCharacters(final char[] text, final int start, final int len)
      throws XMLStreamException {
    delegate.writeCharacters(text, start, len);
  }

  public void writeCharacters(final String text) throws XMLStreamException {
    delegate.writeCharacters(text);
  }

  public void writeComment(final String data) throws XMLStreamException {
    delegate.writeComment(data);
  }

  public void writeDefaultNamespace(final String namespaceURI) throws XMLStreamException {
    delegate.writeDefaultNamespace(namespaceURI);
  }

  public void writeDTD(final String dtd) throws XMLStreamException {
    delegate.writeDTD(dtd);
  }

  public void writeEmptyElement(final String localName) throws XMLStreamException {
    delegate.writeEmptyElement(localName);
  }

  public void writeEmptyElement(final String namespaceURI, final String localName)
      throws XMLStreamException {
    delegate.writeEmptyElement(namespaceURI, localName);
  }

  public void writeEmptyElement(
      final String prefix, final String localName, final String namespaceURI)
      throws XMLStreamException {
    delegate.writeEmptyElement(prefix, namespaceURI, localName);
  }

  public void writeEndDocument() throws XMLStreamException {
    delegate.writeEndDocument();
  }

  public void writeEndElement() throws XMLStreamException {
    delegate.writeEndElement();
  }

  public void writeEntityRef(final String name) throws XMLStreamException {
    delegate.writeEntityRef(name);
  }

  public void writeNamespace(final String prefix, final String namespaceURI)
      throws XMLStreamException {
    delegate.writeNamespace(prefix, namespaceURI);
  }

  public void writeProcessingInstruction(final String target) throws XMLStreamException {
    delegate.writeProcessingInstruction(target);
  }

  public void writeProcessingInstruction(final String target, final String data)
      throws XMLStreamException {
    delegate.writeProcessingInstruction(target, data);
  }

  public void writeStartDocument() throws XMLStreamException {
    delegate.writeStartDocument();
  }

  public void writeStartDocument(final String version) throws XMLStreamException {
    delegate.writeStartDocument(version);
  }

  public void writeStartDocument(final String encoding, final String version)
      throws XMLStreamException {
    delegate.writeStartDocument(encoding, version);
  }

  public void writeStartElement(final String localName) throws XMLStreamException {
    delegate.writeStartElement(localName);
  }

  public void writeStartElement(final String namespaceURI, final String localName)
      throws XMLStreamException {
    delegate.writeStartElement(namespaceURI, localName);
  }

  public void writeStartElement(
      final String prefix, final String localName, final String namespaceURI)
      throws XMLStreamException {
    delegate.writeStartElement(prefix, localName, namespaceURI);
  }
}
