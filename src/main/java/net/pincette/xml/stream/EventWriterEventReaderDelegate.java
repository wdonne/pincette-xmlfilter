package net.pincette.xml.stream;

import static net.pincette.util.Util.tryToGetRethrow;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Wraps an EventWriterDelegate in a EventReaderDelegate for using existing filters in an input
 * chain.
 *
 * @author Werner Donné
 */
public class EventWriterEventReaderDelegate extends EventReaderDelegateBase {
  private final List<XMLEvent> buffer = new ArrayList<>();
  private final EventWriterDelegate writer;

  public EventWriterEventReaderDelegate(final EventWriterDelegate writer) {
    this(writer, null);
  }

  public EventWriterEventReaderDelegate(
      final EventWriterDelegate writer, final XMLEventReader reader) {
    super(reader);
    this.writer = writer;

    writer.setParent(
        new DevNullEventWriter() {
          @Override
          public void add(final XMLEvent event) {
            buffer.add(event);
          }
        });
  }

  @Override
  public boolean hasNext() {
    return !buffer.isEmpty() || tryToGetRethrow(this::readNext).orElse(false);
  }

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    final XMLEvent event = buffer.remove(0);

    setCurrentEvent(event);

    return event;
  }

  @Override
  public XMLEvent peek() throws XMLStreamException {
    return !hasNext() ? null : buffer.get(0);
  }

  private boolean readNext() throws XMLStreamException {
    while (buffer.isEmpty()) {
      if (!getParent().hasNext()) {
        return false;
      }

      writer.add(getParent().nextEvent());
    }

    return true;
  }
}
