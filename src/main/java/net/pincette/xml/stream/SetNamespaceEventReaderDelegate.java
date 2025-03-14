package net.pincette.xml.stream;

import java.util.Optional;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * Sets the namespace of start and end events to a given value if it is not set.
 *
 * @author Werner Donné
 */
public class SetNamespaceEventReaderDelegate extends EventReaderDelegate {
  private final XMLEventFactory factory = XMLEventFactory.newFactory();
  private final String namespaceURI;

  public SetNamespaceEventReaderDelegate(final String namespaceURI, final XMLEventReader parent) {
    super(parent);
    this.namespaceURI = namespaceURI;
  }

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    return setNamespace(getParent().nextEvent());
  }

  @Override
  public XMLEvent nextTag() throws XMLStreamException {
    return setNamespace(getParent().nextTag());
  }

  @Override
  public XMLEvent peek() throws XMLStreamException {
    return setNamespace(getParent().peek());
  }

  private XMLEvent setNamespace(final XMLEvent event) {
    if (event.isStartElement()) {
      final StartElement start = event.asStartElement();

      return Optional.of(start.getName())
          .filter(name -> name.getNamespaceURI() == null || name.getNamespaceURI().isEmpty())
          .map(
              name ->
                  factory.createStartElement(
                      name.getPrefix(),
                      namespaceURI,
                      name.getLocalPart(),
                      start.getAttributes(),
                      start.getNamespaces()))
          .orElse(start);
    }

    if (event.isEndElement()) {
      final EndElement end = event.asEndElement();

      return Optional.of(end.getName())
          .filter(name -> name.getNamespaceURI() == null || name.getNamespaceURI().isEmpty())
          .map(
              name ->
                  factory.createEndElement(
                      name.getPrefix(), namespaceURI, name.getLocalPart(), end.getNamespaces()))
          .orElse(end);
    }

    return event;
  }
}
