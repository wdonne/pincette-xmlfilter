package net.pincette.xml.stream;

import net.pincette.xml.NamespacePrefixMap;
import java.util.Iterator;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;



/**
 * Records the start and end of namespace declarations in a scoped manner.
 * @author Werner Donn\u00e9
 */

public class NamespaceTrackerEventReader extends EventReaderDelegate

{

  private NamespacePrefixMap	map = new NamespacePrefixMap();



  public
  NamespaceTrackerEventReader()
  {
  }



  public
  NamespaceTrackerEventReader(XMLEventReader reader)
  {
    super(reader);
  }



  public NamespacePrefixMap
  getNamespaceContext()
  {
    return map;
  }



  public XMLEvent
  nextEvent() throws XMLStreamException
  {
    XMLEvent	event = super.nextEvent();

    if (event.isStartElement())
    {
      for (Iterator i = event.asStartElement().getNamespaces(); i.hasNext();)
      {
        Namespace	namespace = (Namespace) i.next();

        map.startPrefixMapping
        (
          namespace.getPrefix(),
          namespace.getNamespaceURI()
        );
      }
    }
    else
    {
      if (event.isEndElement())
      {
        for (Iterator i = event.asEndElement().getNamespaces(); i.hasNext();)
        {
          map.endPrefixMapping(((Namespace) i.next()).getPrefix());
        }
      }
    }

    return event;
  }

} // NamespaceTrackerEventReader
