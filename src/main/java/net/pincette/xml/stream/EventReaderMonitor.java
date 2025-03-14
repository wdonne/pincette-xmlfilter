package net.pincette.xml.stream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * Sends all consumed events to an XMLEventWriter.
 *
 * @author Werner Donné
 */
public class EventReaderMonitor extends EventReaderDelegate {
  private final XMLEventWriter writer;

  public EventReaderMonitor(final XMLEventWriter writer) {
    this(writer, null);
  }

  public EventReaderMonitor(final XMLEventWriter writer, final XMLEventReader reader) {
    super(reader);
    this.writer = writer;
  }

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    final XMLEvent event = getParent().nextEvent();

    writer.add(event);

    return event;
  }
}
