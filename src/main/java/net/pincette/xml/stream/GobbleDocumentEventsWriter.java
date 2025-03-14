package net.pincette.xml.stream;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Doesn't let the document events through, which enables inlining of documents. It also removes all
 * events around the document events.
 *
 * @author Werner Donné
 */
public class GobbleDocumentEventsWriter extends EventWriterDelegate {
  private boolean documentOpen = false;

  public GobbleDocumentEventsWriter() {}

  public GobbleDocumentEventsWriter(final XMLEventWriter writer) {
    super(writer);
  }

  @Override
  public void add(final XMLEvent event) throws XMLStreamException {
    if (!documentOpen && event.isStartElement()) {
      documentOpen = true;
    } else if (event.isEndDocument()) {
      documentOpen = false;
    }

    if (documentOpen) {
      super.add(event);
    }
  }
}
