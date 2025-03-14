package net.pincette.xml.stream;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Flushes each event immediately, which is useful for debugging filter chains in which exceptions
 * are thrown.
 *
 * @author Werner Donn
 */
public class FlushEventWriter extends EventWriterDelegate {
  public FlushEventWriter() {}

  public FlushEventWriter(final XMLEventWriter writer) {
    super(writer);
  }

  @Override
  public void add(final XMLEvent event) throws XMLStreamException {
    super.add(event);
    flush();
  }
}
