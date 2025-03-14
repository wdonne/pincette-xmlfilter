package net.pincette.xml.stream;

import static javax.xml.stream.XMLStreamConstants.ATTRIBUTE;
import static javax.xml.stream.XMLStreamConstants.CDATA;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.COMMENT;
import static javax.xml.stream.XMLStreamConstants.DTD;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.ENTITY_DECLARATION;
import static javax.xml.stream.XMLStreamConstants.ENTITY_REFERENCE;
import static javax.xml.stream.XMLStreamConstants.NAMESPACE;
import static javax.xml.stream.XMLStreamConstants.NOTATION_DECLARATION;
import static javax.xml.stream.XMLStreamConstants.PROCESSING_INSTRUCTION;
import static javax.xml.stream.XMLStreamConstants.SPACE;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static net.pincette.util.StreamUtil.stream;
import static net.pincette.util.Util.tryToDoRethrow;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * An XMLEventWriter wrapper around an XMLStreamWriter.
 *
 * @author Werner Donné
 */
public class StreamEventWriter implements XMLEventWriter {
  private final XMLStreamWriter stream;

  public StreamEventWriter(final XMLStreamWriter stream) {
    this.stream = stream;
  }

  public void add(final XMLEvent event) throws XMLStreamException {
    switch (event.getEventType()) {
      case ATTRIBUTE:
        writeAttribute((Attribute) event);
        break;
      case CDATA, CHARACTERS, SPACE:
        writeCharacters((Characters) event);
        break;
      case COMMENT:
        writeComment((Comment) event);
        break;
      case DTD:
        writeDTD((DTD) event);
        break;
      case END_DOCUMENT:
        writeEndDocument();
        break;
      case END_ELEMENT:
        writeEndElement();
        break;
      case ENTITY_DECLARATION:
        writeEntityDeclaration();
        break;
      case ENTITY_REFERENCE:
        writeEntityReference((EntityReference) event);
        break;
      case NAMESPACE:
        writeNamespace((Namespace) event);
        break;
      case NOTATION_DECLARATION:
        writeNotationDeclaration();
        break;
      case PROCESSING_INSTRUCTION:
        writeProcessingInstruction((ProcessingInstruction) event);
        break;
      case START_DOCUMENT:
        writeStartDocument((StartDocument) event);
        break;
      case START_ELEMENT:
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

    reader.close();
  }

  public void close() throws XMLStreamException {
    stream.close();
  }

  public void flush() throws XMLStreamException {
    stream.flush();
  }

  public NamespaceContext getNamespaceContext() {
    return stream.getNamespaceContext();
  }

  public String getPrefix(final String uri) throws XMLStreamException {
    return stream.getPrefix(uri);
  }

  public void setDefaultNamespace(final String uri) throws XMLStreamException {
    stream.setDefaultNamespace(uri);
  }

  public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
    stream.setNamespaceContext(context);
  }

  public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
    stream.setPrefix(prefix, uri);
  }

  private void writeAttribute(final Attribute event) throws XMLStreamException {
    stream.writeAttribute(
        event.getName().getPrefix(),
        event.getName().getNamespaceURI(),
        event.getName().getLocalPart(),
        event.getValue());
  }

  private void writeCharacters(final Characters event) throws XMLStreamException {
    if (event.isCData()) {
      stream.writeCData(event.getData());
    } else {
      stream.writeCharacters(event.getData());
    }
  }

  private void writeComment(final Comment event) throws XMLStreamException {
    stream.writeComment(event.getText());
  }

  private void writeDTD(final DTD event) throws XMLStreamException {
    stream.writeDTD(event.getDocumentTypeDeclaration());
  }

  private void writeEndDocument() throws XMLStreamException {
    stream.writeEndDocument();
  }

  private void writeEndElement() throws XMLStreamException {
    stream.writeEndElement();
  }

  private void writeEntityDeclaration() {
    // Nothing to do.
  }

  private void writeEntityReference(final EntityReference event) throws XMLStreamException {
    stream.writeEntityRef(event.getName());
  }

  private void writeNamespace(final Namespace event) throws XMLStreamException {
    stream.writeNamespace(event.getPrefix(), event.getNamespaceURI());
  }

  private void writeNotationDeclaration() {
    // Nothing to do.
  }

  private void writeProcessingInstruction(final ProcessingInstruction event)
      throws XMLStreamException {
    stream.writeProcessingInstruction(event.getTarget(), event.getData());
  }

  private void writeStartDocument(final StartDocument event) throws XMLStreamException {
    stream.writeStartDocument(event.getCharacterEncodingScheme(), event.getVersion());
  }

  private void writeStartElement(final StartElement event) throws XMLStreamException {
    stream.writeStartElement(event.getName().getNamespaceURI(), event.getName().getLocalPart());
    stream(event.getNamespaces()).forEach(n -> tryToDoRethrow(() -> writeNamespace(n)));
    stream(event.getAttributes()).forEach(a -> tryToDoRethrow(() -> writeAttribute(a)));
  }
}
