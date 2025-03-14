package net.pincette.xml.stream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import net.pincette.util.Util.GeneralException;

/**
 * Doesn't let the document events through, which enables inlining of documents. It also removes all
 * events around the document events.
 *
 * @author Werner Donné
 */
public class GobbleDocumentEventsReader extends EventReaderDelegate {
  private boolean documentOpen = false;
  private XMLEvent pendingEvent;

  public GobbleDocumentEventsReader() {}

  public GobbleDocumentEventsReader(final XMLEventReader reader) {
    super(reader);
  }

  @Override
  public boolean hasNext() {
    try {
      if (pendingEvent != null) {
        return true;
      }

      XMLEvent event = null;

      while (!documentOpen && super.hasNext()) {
        event = super.nextEvent();
        documentOpen = event.isStartElement();
      }

      if (event != null && event.isStartElement()) {
        pendingEvent = event;

        return true;
      }

      if (!documentOpen || !super.hasNext()) {
        return false;
      }

      pendingEvent = super.nextEvent();

      if (pendingEvent.isEndDocument()) {
        documentOpen = false;
        pendingEvent = null;

        return false;
      }

      return true;
    } catch (Exception e) {
      throw new GeneralException(e);
    }
  }

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    final XMLEvent result = pendingEvent;

    pendingEvent = null;

    return result;
  }

  @Override
  public XMLEvent peek() throws XMLStreamException {
    return hasNext() ? pendingEvent : null;
  }
}
