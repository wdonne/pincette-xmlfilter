package net.pincette.xml.stream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;



/**
 * Doesn't let the document events through, which enables inlining of
 * documents. It also removes all events around the document events.
 * @author Werner Donn\u00e9
 */

public class GobbleDocumentEventsReader extends EventReaderDelegate

{

  private boolean	documentOpen = false;
  private XMLEvent	pendingEvent;



  public
  GobbleDocumentEventsReader()
  {
  }



  public
  GobbleDocumentEventsReader(XMLEventReader reader)
  {
    super(reader);
  }



  public boolean
  hasNext()
  {
    try
    {
      if (pendingEvent != null)
      {
        return true;
      }

      XMLEvent	event = null;

      while (!documentOpen && super.hasNext())
      {
        event = super.nextEvent();
        documentOpen = event.isStartElement();
      }

      if (event != null && event.isStartElement())
      {
        pendingEvent = event;

        return true;
      }

      if (!documentOpen || !super.hasNext())
      {
        return false;
      }

      pendingEvent = super.nextEvent();

      if (pendingEvent.isEndDocument())
      {
        documentOpen = false;
        pendingEvent = null;

        return false;
      }

      return true;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public XMLEvent
  nextEvent() throws XMLStreamException
  {
    XMLEvent	result = pendingEvent;

    pendingEvent = null;

    return result;
  }



  public XMLEvent
  peek() throws XMLStreamException
  {
    return hasNext() ? pendingEvent : null;
  }

} // GobbleDocumentEventsReader
