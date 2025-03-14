package net.pincette.xml.stream;

import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * A reader that is fed of a dynamic list of events. Consumed events are removed from the list and
 * when the list runs out of events, the caller is notified in order for it to replenish the list if
 * desired.
 *
 * @author Werner Donné
 */
public class DynamicListEventReader implements XMLEventReader {
  private boolean closed;
  private final List<XMLEvent> events;
  private XMLEvent lastEvent;
  private final Notify notify;

  public DynamicListEventReader(final Notify notify) {
    this(null, notify);
  }

  public DynamicListEventReader(final List<XMLEvent> events, final Notify notify) {
    this.events = events != null ? events : new ArrayList<>();
    this.notify = notify;
  }

  public void close() throws XMLStreamException {
    closed = true;
  }

  public String getElementText() throws XMLStreamException {
    return Util.getElementText(this, lastEvent, new HashMap<>());
  }

  public Object getProperty(final String name) {
    return null;
  }

  public boolean hasNext() {
    if (closed) {
      return false;
    }

    if (events.isEmpty() && notify != null) {
      tryToDoRethrow(() -> notify.empty(events));
    }

    return !events.isEmpty();
  }

  public Object next() throws NoSuchElementException {
    return tryToGetRethrow(this::nextEvent);
  }

  public XMLEvent nextEvent() throws XMLStreamException {
    if (closed || events.isEmpty()) {
      throw new NoSuchElementException();
    }

    lastEvent = events.remove(0);

    return lastEvent;
  }

  public XMLEvent nextTag() throws XMLStreamException {
    return Util.nextTag(this);
  }

  public XMLEvent peek() throws XMLStreamException {
    return !closed && !events.isEmpty() ? events.get(0) : null;
  }

  @Override
  public void remove() {
    // Nothing to do.
  }

  public interface Notify {
    void empty(List<XMLEvent> events) throws XMLStreamException;
  }
}
