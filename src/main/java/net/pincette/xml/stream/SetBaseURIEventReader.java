package net.pincette.xml.stream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Sets the xml:base attribute on the first element it encounters if <code>baseURI</code> is not
 * <code>null</code>.
 *
 * @author Werner Donné
 */
public class SetBaseURIEventReader extends EventReaderDelegateBase {
  private final String baseURI;
  private boolean firstSeen;
  private XMLEvent peeked;

  public SetBaseURIEventReader(final String baseURI) {
    this(baseURI, null);
  }

  public SetBaseURIEventReader(final String baseURI, final XMLEventReader reader) {
    super(reader);
    this.baseURI = baseURI;
  }

  @Override
  public boolean hasNext() {
    return peeked != null || super.hasNext();
  }

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    if (peeked != null) {
      final XMLEvent result = peeked;

      peeked = null;

      return result;
    }

    return process(super.nextEvent());
  }

  @Override
  public XMLEvent peek() throws XMLStreamException {
    if (peeked == null) {
      peeked = process(super.nextEvent());
    }

    return peeked;
  }

  private XMLEvent process(final XMLEvent event) {
    if (event.isStartElement() && !firstSeen) {
      firstSeen = true;

      if (baseURI != null) {
        return Util.setAttribute(
            event.asStartElement(),
            new QName(XMLConstants.XML_NS_URI, "base", XMLConstants.XML_NS_PREFIX),
            baseURI);
      }
    }

    return event;
  }
}
