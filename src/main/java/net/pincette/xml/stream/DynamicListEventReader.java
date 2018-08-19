package net.pincette.xml.stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;



/**
 * A reader that is fed of a dynamic list of events. Consumed events are
 * removed from the list and when the list runs out of events the caller is
 * notified in order for it to replenish the list if desired.
 * @author Werner Donn\u00e9
 */

public class DynamicListEventReader implements XMLEventReader

{

  private boolean		closed;
  private List<XMLEvent>	events;
  private XMLEvent		lastEvent;
  private Notify		notify;



  public
  DynamicListEventReader(Notify notify)
  {
    this(null, notify);
  }



  public
  DynamicListEventReader(List<XMLEvent> events, Notify notify)
  {
    this.events = events != null ? events : new ArrayList<XMLEvent>();
    this.notify = notify;
  }



  public void
  close() throws XMLStreamException
  {
    closed = true;
  }



  public String
  getElementText() throws XMLStreamException
  {
    return Util.getElementText(this, lastEvent, new HashMap());
  }



  public Object
  getProperty(String name)
  {
    return null;
  }



  public boolean
  hasNext()
  {
    if (closed)
    {
      return false;
    }

    if (events.size() == 0 && notify != null)
    {
      try
      {
        notify.empty(events);
      }

      catch (XMLStreamException e)
      {
        throw new RuntimeException(e);
      }
    }

    return events.size() > 0;
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
    if (closed || events.size() == 0)
    {
      throw new NoSuchElementException();
    }

    lastEvent = events.remove(0);

    return lastEvent;
  }



  public XMLEvent
  nextTag() throws XMLStreamException
  {
    return Util.nextTag(this);
  }



  public XMLEvent
  peek() throws XMLStreamException
  {
    return !closed && events.size() > 0 ? events.get(0) : null;
  }



  public void
  remove()
  {
  }



  public interface Notify

  {

    public void	empty	(List<XMLEvent> events) throws XMLStreamException;

  } // Notify

} // DynamicListEventReader
