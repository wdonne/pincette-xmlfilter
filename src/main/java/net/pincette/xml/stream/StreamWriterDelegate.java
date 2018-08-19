package net.pincette.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;



/**
 * Delegates all calls to <code>delegate</code>.
 * @author Werner Donn\u00e9
 */

public class StreamWriterDelegate implements XMLStreamWriter

{

  private XMLStreamWriter	writer;



  public
  StreamWriterDelegate()
  {
  }



  public
  StreamWriterDelegate(XMLStreamWriter writer)
  {
    setParent(writer);
  }



  public void
  close() throws XMLStreamException
  {
    writer.close();
  }



  public void
  flush() throws XMLStreamException
  {
    writer.flush();
  }



  public NamespaceContext
  getNamespaceContext()
  {
    return writer.getNamespaceContext();
  }



  public XMLStreamWriter
  getParent()
  {
    return writer;
  }



  public String
  getPrefix(String uri) throws XMLStreamException
  {
    return writer.getPrefix(uri);
  }



  public Object
  getProperty(String name)
  {
    return writer.getProperty(name);
  }



  public void
  setDefaultNamespace(String uri) throws XMLStreamException
  {
    writer.setDefaultNamespace(uri);
  }



  public void
  setNamespaceContext(NamespaceContext context) throws XMLStreamException
  {
    writer.setNamespaceContext(context);
  }



  public void
  setParent(XMLStreamWriter writer)
  {
    this.writer = writer;
  }



  public void
  setPrefix(String prefix, String uri) throws XMLStreamException
  {
    writer.setPrefix(prefix, uri);
  }



  public void
  writeAttribute(String localName, String value) throws XMLStreamException
  {
    writer.writeAttribute(localName, value);
  }



  public void
  writeAttribute(String namespaceURI, String localName, String value)
    throws XMLStreamException
  {
    writer.writeAttribute(namespaceURI, localName, value);
  }



  public void
  writeAttribute
  (
    String	prefix,
    String	namespaceURI,
    String	localName,
    String	value
  ) throws XMLStreamException
  {
    writer.writeAttribute(prefix, namespaceURI, localName, value);
  }



  public void
  writeCData(String data) throws XMLStreamException
  {
    writer.writeCData(data);
  }



  public void
  writeCharacters(char[] text, int start, int len) throws XMLStreamException
  {
    writer.writeCharacters(text, start, len);
  }



  public void
  writeCharacters(String text) throws XMLStreamException
  {
    writer.writeCharacters(text);
  }



  public void
  writeComment(String data) throws XMLStreamException
  {
    writer.writeComment(data);
  }



  public void
  writeDefaultNamespace(String namespaceURI) throws XMLStreamException
  {
    writer.writeDefaultNamespace(namespaceURI);
  }



  public void
  writeDTD(String dtd) throws XMLStreamException
  {
    writer.writeDTD(dtd);
  }



  public void
  writeEmptyElement(String localName) throws XMLStreamException
  {
    writer.writeEmptyElement(localName);
  }



  public void
  writeEmptyElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    writer.writeEmptyElement(namespaceURI, localName);
  }



  public void
  writeEmptyElement(String prefix, String localName, String namespaceURI)
    throws XMLStreamException
  {
    writer.writeEmptyElement(prefix, namespaceURI, localName);
  }



  public void
  writeEndDocument() throws XMLStreamException
  {
    writer.writeEndDocument();
  }



  public void
  writeEndElement() throws XMLStreamException
  {
    writer.writeEndElement();
  }



  public void
  writeEntityRef(String name) throws XMLStreamException
  {
    writer.writeEntityRef(name);
  }



  public void
  writeNamespace(String prefix, String namespaceURI) throws XMLStreamException
  {
    writer.writeNamespace(prefix, namespaceURI);
  }



  public void
  writeProcessingInstruction(String target) throws XMLStreamException
  {
    writer.writeProcessingInstruction(target);
  }



  public void
  writeProcessingInstruction(String target, String data)
    throws XMLStreamException
  {
    writer.writeProcessingInstruction(target, data);
  }



  public void
  writeStartDocument() throws XMLStreamException
  {
    writer.writeStartDocument();
  }



  public void
  writeStartDocument(String version) throws XMLStreamException
  {
    writer.writeStartDocument(version);
  }



  public void
  writeStartDocument(String encoding, String version) throws XMLStreamException
  {
    writer.writeStartDocument(encoding, version);
  }



  public void
  writeStartElement(String localName) throws XMLStreamException
  {
    writer.writeStartElement(localName);
  }



  public void
  writeStartElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    writer.writeStartElement(namespaceURI, localName);
  }



  public void
  writeStartElement(String prefix, String localName, String namespaceURI)
    throws XMLStreamException
  {
    writer.writeStartElement(prefix, localName, namespaceURI);
  }

} // StreamWriterDelegate
