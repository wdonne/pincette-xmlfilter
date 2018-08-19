package net.pincette.xml.stream;

import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;



/**
 * An event reader that returns no events.
 * @author Werner Donn\u00e9
 */

public class DevNullEventReader implements XMLEventReader

{

  public void
  close() throws XMLStreamException
  {
  }



  public String
  getElementText() throws XMLStreamException
  {
    throw new XMLStreamException("Not a START_ELEMENT.");
  }



  public Object
  getProperty(String name) throws IllegalArgumentException
  {
    throw
      new IllegalArgumentException
      (
        "The property " + name + " is not supported."
      );
  }



  public boolean
  hasNext()
  {
    return false;
  }



  public Object
  next()
  {
    return null;
  }



  public XMLEvent
  nextEvent() throws XMLStreamException
  {
    throw new NoSuchElementException();
  }



  public XMLEvent
  nextTag() throws XMLStreamException
  {
    throw new XMLStreamException();
  }



  public XMLEvent
  peek() throws XMLStreamException
  {
    return null;
  }



  public void
  remove()
  {
    throw new UnsupportedOperationException();
  }

} // DevNullEventReader
