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
public class DevNullEventWriter implements XMLEventWriter {
  public void add(final XMLEvent event) throws XMLStreamException {
    // Nothing to do.
  }

  public void add(final XMLEventReader reader) throws XMLStreamException {
    while (reader.hasNext()) {
      add(reader.nextEvent());
    }

    reader.close();
  }

  public void close() throws XMLStreamException {
    // Nothing to do.
  }

  public void flush() throws XMLStreamException {
    // Nothing to do.
  }

  public NamespaceContext getNamespaceContext() {
    return null;
  }

  public String getPrefix(final String uri) throws XMLStreamException {
    return null;
  }

  public void setDefaultNamespace(final String uri) throws XMLStreamException {
    // Nothing to do.
  }

  public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
    // Nothing to do.
  }

  public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
    // Nothing to do.
  }
}
