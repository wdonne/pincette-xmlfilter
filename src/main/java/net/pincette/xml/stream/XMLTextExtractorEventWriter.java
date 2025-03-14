package net.pincette.xml.stream;

import java.io.IOException;
import java.io.Writer;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Writes the extracted text to the given writer.
 *
 * @author Werner Donné
 */
public class XMLTextExtractorEventWriter implements XMLEventWriter {
  private NamespaceContext context;
  private final Writer writer;

  public XMLTextExtractorEventWriter(final Writer writer) {
    this.writer = writer;
  }

  public void add(final XMLEvent event) throws XMLStreamException {
    if (event.isCharacters()) {
      try {
        writer.write(event.asCharacters().getData());
      } catch (IOException e) {
        throw new XMLStreamException(e);
      }
    }
  }

  public void add(final XMLEventReader reader) throws XMLStreamException {
    while (reader.hasNext()) {
      add(reader.nextEvent());
    }

    reader.close();
  }

  public void close() throws XMLStreamException {
    try {
      writer.close();
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void flush() throws XMLStreamException {
    try {
      writer.flush();
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public NamespaceContext getNamespaceContext() {
    return context;
  }

  public String getPrefix(final String uri) throws XMLStreamException {
    return context.getPrefix(uri);
  }

  public void setDefaultNamespace(final String uri) throws XMLStreamException {
    // Nothing to do.
  }

  public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
    this.context = context;
  }

  public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
    // Nothing to do.
  }
}
