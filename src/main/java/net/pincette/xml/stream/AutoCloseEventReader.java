package net.pincette.xml.stream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Before returning the <code>EndDocument</code> event the <code>close</code> method is called. This
 * is for situations where the reader is passed to some other logic which doesn't necessarily closes
 * the reader.
 *
 * @author Werner Donné
 */
public class AutoCloseEventReader extends EventReaderDelegateBase {
  private boolean closed;

  public AutoCloseEventReader() {}

  public AutoCloseEventReader(final XMLEventReader reader) {
    super(reader);
  }

  @Override
  public void close() throws XMLStreamException {
    if (!closed) {
      super.close();
      closed = true;
    }
  }

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    XMLEvent event = super.nextEvent();

    if (event.isEndDocument()) {
      close();
    }

    return event;
  }
}
