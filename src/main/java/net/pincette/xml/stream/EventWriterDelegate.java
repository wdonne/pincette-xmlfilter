package net.pincette.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Writes nothing.
 *
 * @author Werner Donné
 */
public class EventWriterDelegate implements XMLEventWriter {
  private XMLEventWriter writer;

  public EventWriterDelegate() {}

  public EventWriterDelegate(final XMLEventWriter writer) {
    setParent(writer);
  }

  public void add(final XMLEvent event) throws XMLStreamException {
    writer.add(event);
  }

  public void add(final XMLEventReader reader) throws XMLStreamException {
    while (reader.hasNext()) {
      add(reader.nextEvent());
    }

    reader.close();
  }

  public void close() throws XMLStreamException {
    writer.close();
  }

  public void flush() throws XMLStreamException {
    writer.flush();
  }

  public NamespaceContext getNamespaceContext() {
    return writer.getNamespaceContext();
  }

  public XMLEventWriter getParent() {
    return writer;
  }

  public String getPrefix(final String uri) throws XMLStreamException {
    return writer.getPrefix(uri);
  }

  public void setDefaultNamespace(final String uri) throws XMLStreamException {
    writer.setDefaultNamespace(uri);
  }

  public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
    writer.setNamespaceContext(context);
  }

  public void setParent(final XMLEventWriter writer) {
    this.writer = writer;
  }

  public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
    writer.setPrefix(prefix, uri);
  }
}
