package net.pincette.xml.stream;

import static javax.xml.XMLConstants.XML_NS_URI;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

/**
 * Removes the xml:base attribute if <code>baseURI</code> is not <code>null</code>.
 *
 * @author Werner Donné
 */
public class RemoveBaseURIEventReader extends EventReaderDelegateBase {
  private final String baseURI;
  private XMLEvent peeked;

  public RemoveBaseURIEventReader(final String baseURI) {
    this(baseURI, null);
  }

  public RemoveBaseURIEventReader(final String baseURI, final XMLEventReader reader) {
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
    if (baseURI != null && event.isStartElement()) {
      final Attribute attribute =
          event.asStartElement().getAttributeByName(new QName(XML_NS_URI, "base"));

      if (attribute != null && baseURI.equals(attribute.getValue())) {
        return Util.removeAttribute(event.asStartElement(), new QName(XML_NS_URI, "base"));
      }
    }

    return event;
  }
}
