package net.pincette.xml.stream;

import static javax.xml.XMLConstants.XML_NS_URI;
import static net.pincette.xml.stream.Util.attributes;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

/**
 * Removes the xml:base attribute if <code>baseURI</code> is not <code>null</code>.
 *
 * @author Werner Donn
 */
public class RemoveBaseURIEventWriter extends EventWriterDelegate {
  private final String baseURI;
  private final XMLEventFactory factory = XMLEventFactory.newFactory();

  public RemoveBaseURIEventWriter(final String baseURI) {
    this(baseURI, null);
  }

  public RemoveBaseURIEventWriter(final String baseURI, final XMLEventWriter writer) {
    super(writer);
    this.baseURI = baseURI;
  }

  private static boolean shouldRemove(final Attribute event, final String baseURI) {
    return event.getName().equals(new QName(XML_NS_URI, "base"))
        && baseURI.equals(event.getValue());
  }

  @Override
  public void add(final XMLEvent event) throws XMLStreamException {
    if (baseURI == null) {
      getParent().add(event);
    } else if (event.isStartElement()) {
      getParent()
          .add(
              factory.createStartElement(
                  event.asStartElement().getName(),
                  attributes(event).filter(a -> !shouldRemove(a, baseURI)).iterator(),
                  event.asStartElement().getNamespaces()));
    } else if (!event.isAttribute() || !shouldRemove((Attribute) event, baseURI)) {
      getParent().add(event);
    }
  }
}
