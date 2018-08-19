package net.pincette.xml.stream;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;



/**
 * Records all events in a list.
 * @author Werner Donn\u00e9
 */

public class EventAccumulator extends DevNullEventWriter

{

  private List<XMLEvent>	list = new ArrayList<XMLEvent>();



  public void
  add(XMLEvent event) throws XMLStreamException
  {
    list.add(event);
  }



  public List<XMLEvent>
  getEvents()
  {
    return list;
  }

} // EventAccumulator
