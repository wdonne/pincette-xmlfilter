package net.pincette.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Replicates the output to additional writers.
 *
 * @author Werner Donné
 */
public class Tee extends EventWriterDelegate {
  private final XMLEventWriter[] writers;

  public Tee(final XMLEventWriter[] writers) {
    this(writers, null);
  }

  public Tee(final XMLEventWriter[] writers, final XMLEventWriter writer) {
    super(writer);
    this.writers = writers;
  }

  @Override
  public void add(final XMLEvent event) throws XMLStreamException {
    super.add(event);

    for (final XMLEventWriter writer : writers) {
      writer.add(event);
    }
  }

  @Override
  public void close() throws XMLStreamException {
    super.close();

    for (final XMLEventWriter writer : writers) {
      writer.close();
    }
  }

  @Override
  public void flush() throws XMLStreamException {
    super.flush();

    for (final XMLEventWriter writer : writers) {
      writer.flush();
    }
  }

  @Override
  public void setDefaultNamespace(final String uri) throws XMLStreamException {
    super.setDefaultNamespace(uri);

    for (final XMLEventWriter writer : writers) {
      writer.setDefaultNamespace(uri);
    }
  }

  @Override
  public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
    super.setNamespaceContext(context);

    for (final XMLEventWriter writer : writers) {
      writer.setNamespaceContext(context);
    }
  }

  @Override
  public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
    super.setPrefix(prefix, uri);

    for (final XMLEventWriter writer : writers) {
      writer.setPrefix(prefix, uri);
    }
  }
}
