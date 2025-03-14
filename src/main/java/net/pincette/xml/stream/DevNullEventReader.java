package net.pincette.xml.stream;

import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * An event reader that returns no events.
 *
 * @author Werner Donné
 */
public class DevNullEventReader implements XMLEventReader {
  public void close() throws XMLStreamException {
    // Nothing to do.
  }

  public String getElementText() throws XMLStreamException {
    throw new XMLStreamException("Not a START_ELEMENT.");
  }

  public Object getProperty(final String name) throws IllegalArgumentException {
    throw new IllegalArgumentException("The property " + name + " is not supported.");
  }

  public boolean hasNext() {
    return false;
  }

  public Object next() {
    throw new NoSuchElementException();
  }

  public XMLEvent nextEvent() throws XMLStreamException {
    throw new NoSuchElementException();
  }

  public XMLEvent nextTag() throws XMLStreamException {
    throw new XMLStreamException();
  }

  public XMLEvent peek() throws XMLStreamException {
    return null;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
