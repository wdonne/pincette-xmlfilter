package net.pincette.xml.stream;

import static net.pincette.util.StreamUtil.rangeExclusive;
import static net.pincette.util.Util.tryToGetRethrow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

/**
 * An XMLEventReader wrapper around an XMLStreamReader.
 *
 * @author Werner Donné
 */
public class StreamEventReader implements XMLEventReader {
  private final List<XMLEvent> buffer = new ArrayList<>();
  private final XMLEventFactory factory = XMLEventFactory.newFactory();
  private final Deque<List<Namespace>> namespaces = new ArrayDeque<>();
  private final XMLStreamReader reader;

  public StreamEventReader(final XMLStreamReader reader) {
    this.reader = reader;
  }

  public void close() throws XMLStreamException {
    reader.close();
  }

  private void createAttribute() {
    rangeExclusive(0, reader.getAttributeCount())
        .forEach(
            i ->
                buffer.add(
                    factory.createAttribute(
                        reader.getAttributeName(i), reader.getAttributeValue(i))));
  }

  private void createCData() {
    buffer.add(factory.createCData(reader.getText()));
  }

  private void createCharacters() {
    buffer.add(factory.createCharacters(reader.getText()));
  }

  private void createComment() {
    buffer.add(factory.createComment(reader.getText()));
  }

  private void createDTD() {
    buffer.add(factory.createDTD(reader.getText()));
  }

  private void createEndDocument() {
    buffer.add(factory.createEndDocument());
  }

  private void createEndElement() {
    buffer.add(factory.createEndElement(reader.getName(), namespaces.pop().iterator()));
  }

  private void createEntityReference() {
    buffer.add(
        factory.createEntityReference(
            reader.getLocalName(),
            new InternalEntityDeclaration(reader.getLocalName(), reader.getText())));
  }

  private void createEvent(final int type) {
    switch (type) {
      case XMLStreamConstants.ATTRIBUTE:
        createAttribute();
        break;
      case XMLStreamConstants.CDATA:
        createCData();
        break;
      case XMLStreamConstants.CHARACTERS:
        createCharacters();
        break;
      case XMLStreamConstants.COMMENT:
        createComment();
        break;
      case XMLStreamConstants.DTD:
        createDTD();
        break;
      case XMLStreamConstants.END_DOCUMENT:
        createEndDocument();
        break;
      case XMLStreamConstants.END_ELEMENT:
        createEndElement();
        break;
      case XMLStreamConstants.ENTITY_REFERENCE:
        createEntityReference();
        break;
      case XMLStreamConstants.NAMESPACE:
        createNamespace();
        break;
      case XMLStreamConstants.PROCESSING_INSTRUCTION:
        createProcessingInstruction();
        break;
      case XMLStreamConstants.SPACE:
        createSpace();
        break;
      case XMLStreamConstants.START_DOCUMENT:
        createStartDocument();
        break;
      case XMLStreamConstants.START_ELEMENT:
        createStartElement();
        break;
      default:
        break;
    }
  }

  private void createNamespace() {
    Optional.ofNullable(namespaces.peek())
        .ifPresent(
            inScope ->
                rangeExclusive(0, reader.getNamespaceCount())
                    .map(
                        i ->
                            factory.createNamespace(
                                reader.getNamespacePrefix(i), reader.getNamespaceURI(i)))
                    .forEach(
                        n -> {
                          buffer.add(n);
                          inScope.add(n);
                        }));
  }

  private void createProcessingInstruction() {
    buffer.add(factory.createProcessingInstruction(reader.getPITarget(), reader.getPIData()));
  }

  private void createSpace() {
    buffer.add(factory.createSpace(reader.getText()));
  }

  private void createStartDocument() {
    buffer.add(
        factory.createStartDocument(
            reader.getEncoding(), reader.getVersion(), reader.isStandalone()));
  }

  private void createStartElement() {
    buffer.add(factory.createStartElement(reader.getName(), null, null));
    namespaces.push(new ArrayList<>());
    createAttribute();
    createNamespace();
  }

  public String getElementText() throws XMLStreamException {
    return reader.getElementText();
  }

  public Object getProperty(final String name) {
    return reader.getProperty(name);
  }

  public boolean hasNext() {
    return tryToGetRethrow(() -> !buffer.isEmpty() || reader.hasNext()).orElse(false);
  }

  public Object next() {
    try {
      return nextEvent();
    } catch (XMLStreamException e) {
      throw new NoSuchElementException();
    }
  }

  public XMLEvent nextEvent() throws XMLStreamException {
    final XMLEvent result = peek();

    buffer.remove(0);

    return result;
  }

  public XMLEvent nextTag() throws XMLStreamException {
    return Util.nextTag(this);
  }

  public XMLEvent peek() throws XMLStreamException {
    if (buffer.isEmpty()) {
      createEvent(reader.next());
    }

    return buffer.get(0);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
