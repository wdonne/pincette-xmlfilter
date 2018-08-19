package net.pincette.xml.stream;

import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;



/**
 * A reader that is fed of a list of events. It can be used together with the
 * <code>EventAccumulator</code>.
 * @author Werner Donn\u00e9
 */

public class ListEventReader implements XMLEventReader

{

  private boolean		closed;
  private List<XMLEvent>	events;
  private int			position;



  public
  ListEventReader(List<XMLEvent> events)
  {
    this.events = events;
  }



  public void
  close() throws XMLStreamException
  {
    closed = true;
  }



  public String
  getElementText() throws XMLStreamException
  {
    return Util.getElementText(this, events.get(position - 1), new HashMap());
  }



  public Object
  getProperty(String name)
  {
    return null;
  }



  public boolean
  hasNext()
  {
    return !closed && position < events.size();
  }



  public Object
  next() throws NoSuchElementException
  {
    try
    {
      return nextEvent();
    }

    catch (XMLStreamException e)
    {
      throw new RuntimeException(e);
    }
  }



  public XMLEvent
  nextEvent() throws XMLStreamException
  {
    if (closed || position >= events.size())
    {
      throw new NoSuchElementException();
    }

    return events.get(position++);
  }



  public XMLEvent
  nextTag() throws XMLStreamException
  {
    return Util.nextTag(this);
  }



  public XMLEvent
  peek() throws XMLStreamException
  {
    return !closed && position < events.size() ? events.get(position) : null;
  }



  public void
  remove()
  {
  }

} // ListEventReader
