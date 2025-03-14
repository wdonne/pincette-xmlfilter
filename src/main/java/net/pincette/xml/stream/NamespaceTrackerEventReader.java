package net.pincette.xml.stream;

import static net.pincette.util.StreamUtil.stream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import net.pincette.xml.NamespacePrefixMap;

/**
 * Records the start and end of namespace declarations in a scoped manner.
 *
 * @author Werner Donné
 */
public class NamespaceTrackerEventReader extends EventReaderDelegate {
  private final NamespacePrefixMap map = new NamespacePrefixMap();

  public NamespaceTrackerEventReader() {}

  public NamespaceTrackerEventReader(final XMLEventReader reader) {
    super(reader);
  }

  public NamespacePrefixMap getNamespaceContext() {
    return map;
  }

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    final XMLEvent event = super.nextEvent();

    if (event.isStartElement()) {
      stream(event.asStartElement().getNamespaces())
          .forEach(n -> map.startPrefixMapping(n.getPrefix(), n.getNamespaceURI()));
    } else if (event.isEndElement()) {
      stream(event.asEndElement().getNamespaces())
          .forEach(n -> map.endPrefixMapping(n.getPrefix()));
    }

    return event;
  }
}
