package net.pincette.xml.stream;

import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * An XMLEventReader wrapper around an XMLFilter.
 *
 * @author Werner Donné
 */
public class XMLFilterEventReaderDelegate extends EventReaderDelegate {
  private final List<XMLEvent> buffer = new ArrayList<>();
  private final XMLEventWriter filterWriter;
  private final XMLReader nop =
      new XMLFilterImpl() {
        @Override
        public void parse(final InputSource input) {
          // Purpose.
        }

        @Override
        public void parse(final String systemId) {
          // Purpose.
        }

        @Override
        public void setFeature(final String name, final boolean value) {
          // Purpose.
        }

        @Override
        public void setProperty(final String name, final Object value) {
          // Purpose.
        }
      };

  public XMLFilterEventReaderDelegate(final XMLFilter filter) {
    this(filter, null);
  }

  public XMLFilterEventReaderDelegate(final XMLFilter filter, final XMLEventReader reader) {
    super(reader);
    replaceParser(filter);
    tryToDoRethrow(() -> filter.parse((String) null));
    // Make sure the set-up is done in the filter chain.

    filterWriter = new StreamEventWriter(new ContentHandlerStreamWriter(nop.getContentHandler()));

    filter.setContentHandler(
        new EventWriterContentHandler(
            new DevNullEventWriter() {
              @Override
              public void add(XMLEvent event) {
                buffer.add(event);
              }
            }));
  }

  @Override
  public String getElementText() throws XMLStreamException {
    return Util.getElementText(this, buffer.get(0), null);
  }

  @Override
  public boolean hasNext() {
    return tryToGetRethrow(() -> !buffer.isEmpty() || readNext()).orElse(false);
  }

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    return buffer.remove(0);
  }

  @Override
  public XMLEvent nextTag() throws XMLStreamException {
    return Util.nextTag(this);
  }

  @Override
  public XMLEvent peek() throws XMLStreamException {
    return !hasNext() ? null : buffer.get(0);
  }

  private boolean readNext() throws XMLStreamException {
    while (buffer.isEmpty()) {
      if (!getParent().hasNext()) {
        return false;
      }

      filterWriter.add(getParent().nextEvent());
    }

    return true;
  }

  private void replaceParser(final XMLFilter filter) {
    XMLFilter previous = null;

    for (XMLFilter i = filter;
        i.getParent() instanceof XMLFilter;
        previous = i, i = (XMLFilter) i.getParent())
      ;

    if (previous != null) {
      if (previous.getParent() instanceof XMLFilter p) {
        p.setParent(nop);
      } else {
        previous.setParent(nop);
      }
    } else {
      filter.setParent(nop);
    }
  }
}
