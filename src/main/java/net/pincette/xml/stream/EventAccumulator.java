package net.pincette.xml.stream;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Records all events in a list.
 *
 * @author Werner Donné
 */
public class EventAccumulator extends DevNullEventWriter {
  private final List<XMLEvent> list = new ArrayList<>();

  @Override
  public void add(final XMLEvent event) throws XMLStreamException {
    list.add(event);
  }

  public List<XMLEvent> getEvents() {
    return list;
  }
}
