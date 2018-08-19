package net.pincette.xml.stream;

import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Sets the xml:base attribute on the first element it encounters if <code>baseURI</code> is not
 * <code>null</code>.
 *
 * @author Werner Donn\u00e9
 */
public class SetBaseURIEventWriter extends EventWriterDelegate {
  private final String baseURI;
  private final XMLEventFactory factory = XMLEventFactory.newFactory();
  private boolean firstSeen;

  public SetBaseURIEventWriter(final String baseURI) {
    this(baseURI, null);
  }

  public SetBaseURIEventWriter(final String baseURI, final XMLEventWriter writer) {
    super(writer);
    this.baseURI = baseURI;
  }

  public void add(final XMLEvent event) throws XMLStreamException {
    if (event.isStartElement() && !firstSeen) {
      firstSeen = true;

      super.add(
          baseURI != null
              ? Util.setAttribute(
                  event.asStartElement(),
                  factory.createAttribute(XML_NS_PREFIX, XML_NS_URI, "base", baseURI))
              : event);
    } else {
      super.add(event);
    }
  }
}
