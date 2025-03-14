package net.pincette.xml.stream;

import static java.util.Optional.ofNullable;
import static javax.xml.stream.XMLStreamConstants.ENTITY_DECLARATION;
import static net.pincette.util.StreamUtil.last;
import static net.pincette.util.StreamUtil.takeWhile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * Wraps an EventReaderDelegate in a EventWriterDelegate for using existing filters in an output
 * chain.
 *
 * @author Werner Donné
 */
public class EventReaderEventWriterDelegate extends EventWriterDelegate {
  private final List<XMLEvent> buffer = new ArrayList<>();
  private final Map<String, String> entityDeclarations = new HashMap<>();
  private final EventReaderDelegate reader;

  public EventReaderEventWriterDelegate(final EventReaderDelegate reader) {
    this(reader, null);
  }

  public EventReaderEventWriterDelegate(
      final EventReaderDelegate reader, final XMLEventWriter writer) {
    super(writer);
    this.reader = reader;

    last(takeWhile(
            reader,
            rd ->
                (EventReaderDelegate)
                    ofNullable(rd.getParent())
                        .filter(EventReaderDelegate.class::isInstance)
                        .orElse(null),
            Objects::nonNull))
        .ifPresent(r -> r.setParent(new BufferReader()));
  }

  @Override
  public void add(final XMLEvent event) throws XMLStreamException {
    if (event.getEventType() == ENTITY_DECLARATION
        && ((EntityDeclaration) event).getReplacementText() != null) {
      entityDeclarations.put(
          ((EntityDeclaration) event).getName(), ((EntityDeclaration) event).getReplacementText());
    }

    buffer.add(event);
    forward();
  }

  @Override
  public void close() throws XMLStreamException {
    forward();
    reader.close();
    super.close();
  }

  @Override
  public void flush() throws XMLStreamException {
    forward();
    super.flush();
  }

  private void forward() throws XMLStreamException {
    while (reader.hasNext()) {
      getParent().add(reader.nextEvent());
    }
  }

  private class BufferReader implements XMLEventReader {
    public void close() {
      // Nothing to do.
    }

    public String getElementText() throws XMLStreamException {
      return Util.getElementText(this, buffer.get(0), entityDeclarations);
    }

    public Object getProperty(final String name) {
      throw new IllegalArgumentException();
    }

    public boolean hasNext() {
      return !buffer.isEmpty();
    }

    public Object next() {
      return nextEvent();
    }

    public XMLEvent nextEvent() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      return buffer.remove(0);
    }

    public XMLEvent nextTag() throws XMLStreamException {
      return Util.nextTag(this);
    }

    public XMLEvent peek() {
      return hasNext() ? buffer.get(0) : null;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
