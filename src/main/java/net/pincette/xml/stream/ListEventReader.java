package net.pincette.xml.stream;

import static net.pincette.util.Util.tryToGetRethrow;

import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * A reader that is fed of a list of events. It can be used together with the <code>EventAccumulator
 * </code>.
 *
 * @author Werner Donné
 */
public class ListEventReader implements XMLEventReader {
  private boolean closed;
  private final List<XMLEvent> events;
  private int position;

  public ListEventReader(final List<XMLEvent> events) {
    this.events = events;
  }

  public void close() throws XMLStreamException {
    closed = true;
  }

  public String getElementText() throws XMLStreamException {
    return Util.getElementText(this, events.get(position - 1), new HashMap<>());
  }

  public Object getProperty(final String name) {
    return null;
  }

  public boolean hasNext() {
    return !closed && position < events.size();
  }

  public Object next() {
    return tryToGetRethrow(this::nextEvent).orElse(null);
  }

  public XMLEvent nextEvent() throws XMLStreamException {
    if (closed || position >= events.size()) {
      throw new NoSuchElementException();
    }

    return events.get(position++);
  }

  public XMLEvent nextTag() throws XMLStreamException {
    return Util.nextTag(this);
  }

  public XMLEvent peek() throws XMLStreamException {
    return !closed && position < events.size() ? events.get(position) : null;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
