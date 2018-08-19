package net.pincette.xml.stream;

import static java.lang.System.arraycopy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.autoClose;
import static net.pincette.util.Util.must;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.util.Util.tryToGetWithRethrow;
import static net.pincette.xml.Util.stream;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMSource;
import net.pincette.function.SideEffect;
import net.pincette.util.Pair;
import net.pincette.util.SetBuilder;
import net.pincette.util.StreamUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.Node;

/** @author Werner Donn\u00e9 */
public class Util {
  private static final String UNEXPECTED = "Unexpected event type";

  private Util() {}

  public static Element accumulate(final XMLEventReader reader, final StartElement currentEvent) {
    return Optional.ofNullable(newDocument())
        .flatMap(document -> tryToGetRethrow(() -> accumulate(document, reader, currentEvent)))
        .map(Document::getDocumentElement)
        .orElse(null);
  }

  private static Document accumulate(
      final Document document, final XMLEventReader reader, final StartElement currentEvent)
      throws XMLStreamException {
    final XMLEventFactory factory = XMLEventFactory.newFactory();
    final DOMEventWriter writer = new DOMEventWriter(document);

    writer.add(factory.createStartDocument());
    addElement(reader, writer, currentEvent);
    writer.add(factory.createEndDocument());
    writer.close();

    return document;
  }

  /**
   * Adds the complete element that started with <code>currentEvent</code> to <code>writer</code>.
   */
  public static void addElement(
      final XMLEventReader reader, final XMLEventWriter writer, final StartElement currentEvent)
      throws XMLStreamException {
    final Deque<XMLEvent> elements = new ArrayDeque<>();

    elements.push(currentEvent);
    writer.add(currentEvent);

    while (reader.hasNext()) {
      final XMLEvent event = reader.nextEvent();

      writer.add(event);

      if (event.isStartElement()) {
        elements.push(event);
      } else if (event.isEndElement()) {
        elements.pop();
      }

      if (elements.isEmpty()) {
        return;
      }
    }
  }

  public static void addElement(final Element element, final XMLEventWriter writer)
      throws XMLStreamException {
    new GobbleDocumentEventsWriter(writer)
        .add(XMLInputFactory.newFactory().createXMLEventReader(new DOMSource(element)));
  }

  public static void addEmptyElement(final QName name, final XMLEventWriter writer)
      throws XMLStreamException {
    final XMLEventFactory factory = XMLEventFactory.newFactory();

    writer.add(factory.createStartElement(name, null, null));
    writer.add(factory.createEndElement(name, null));
  }

  public static void addTextElement(
      final QName name, final String text, final XMLEventWriter writer) throws XMLStreamException {
    final XMLEventFactory factory = XMLEventFactory.newFactory();

    writer.add(factory.createStartElement(name, null, null));
    writer.add(factory.createCharacters(text));
    writer.add(factory.createEndElement(name, null));
  }

  public static Stream<Attribute> attributes(final XMLEvent event) {
    return Optional.of(event)
        .filter(XMLEvent::isStartElement)
        .map(XMLEvent::asStartElement)
        .map(StartElement::getAttributes)
        .map(a -> (Iterator<Attribute>) a)
        .map(StreamUtil::stream)
        .orElseGet(Stream::empty);
  }

  public static StartElement changeAttributes(
      final StartElement event, final Function<StartElement, Stream<Attribute>> newAttributes) {
    return XMLEventFactory.newFactory()
        .createStartElement(
            event.getName(),
            newAttributes.apply(event).collect(toList()).iterator(),
            event.getNamespaces());
  }

  public static StartElement changeNamespaces(
      final StartElement event, final Function<StartElement, Stream<Namespace>> newNamespaces) {
    return XMLEventFactory.newFactory()
        .createStartElement(
            event.getName(),
            event.getNamespaces(),
            newNamespaces.apply(event).collect(toList()).iterator());
  }

  static Node clearNode(final Node node) {
    while (node.hasChildNodes()) {
      node.removeChild(node.getFirstChild());
    }

    return node;
  }

  /**
   * This method is much less memory intensive for large PCDATA sections.
   *
   * @param entityDeclarations a map from entity names to replacement texts.
   */
  private static String coalesceText(
      final List<XMLEvent> events, final Map<String, String> entityDeclarations) {
    final Function<EntityReference, String> tryEntity =
        e ->
            entityDeclarations != null && entityDeclarations.get(e.getName()) != null
                ? entityDeclarations.get(e.getName())
                : ("&" + e.getName() + ";");

    return new String(
        events
            .stream()
            .map(
                e ->
                    e.isCharacters()
                        ? e.asCharacters().getData()
                        : tryEntity.apply((EntityReference) e))
            .reduce(
                pair(new char[getLength(events, entityDeclarations)], 0),
                (result, text) ->
                    SideEffect.<Pair<char[], Integer>>run(
                            () ->
                                arraycopy(
                                    text.toCharArray(),
                                    0,
                                    result.first,
                                    result.second,
                                    text.length()))
                        .andThenGet(() -> pair(result.first, result.second + text.length())),
                (r1, r2) -> r1)
            .first);
  }

  public static Attribute createAttribute(final Attr attribute) {
    return createAttribute(attribute, XMLEventFactory.newFactory());
  }

  public static Attribute createAttribute(final Attr attribute, final XMLEventFactory factory) {
    return factory.createAttribute(getQName(attribute), attribute.getValue());
  }

  public static EndElement createEndElement(final Element element) {
    return createEndElement(element, XMLEventFactory.newFactory());
  }

  public static EndElement createEndElement(final Element element, final XMLEventFactory factory) {
    return factory.createEndElement(
        getQName(element),
        net.pincette.xml.Util.attributes(element)
            .filter(Util::isNamescape)
            .map(a -> createNamespace(a, factory))
            .collect(toList())
            .iterator());
  }

  public static EntityReference createEntityReference(final org.w3c.dom.EntityReference ref) {
    return createEntityReference(ref, XMLEventFactory.newFactory());
  }

  public static EntityReference createEntityReference(
      final org.w3c.dom.EntityReference ref, final XMLEventFactory factory) {
    return Optional.ofNullable(ref.getOwnerDocument().getDoctype())
        .flatMap(
            docType ->
                stream(docType.getEntities())
                    .filter(e -> e.getNodeName().equals(ref.getNodeName()))
                    .map(e -> (Entity) e)
                    .findFirst())
        .map(e -> factory.createEntityReference(ref.getNodeName(), new EntityDeclarationEvent(e)))
        .orElse(null);
  }

  public static Namespace createNamespace(final Attr attribute) {
    return createNamespace(attribute, XMLEventFactory.newFactory());
  }

  public static Namespace createNamespace(final Attr attribute, final XMLEventFactory factory) {
    return attribute.getName().startsWith("xmlns:")
        ? factory.createNamespace(
            attribute.getName().substring("xmlns:".length()), attribute.getValue())
        : factory.createNamespace(attribute.getValue());
  }

  public static StartElement createStartElement(final Element element) {
    return createStartElement(element, XMLEventFactory.newFactory());
  }

  public static StartElement createStartElement(
      final Element element, final XMLEventFactory factory) {
    final Pair<SetBuilder<Attribute>, SetBuilder<Namespace>> split =
        net.pincette.xml.Util.attributes(element)
            .reduce(
                pair(new SetBuilder<>(new HashSet<>()), new SetBuilder<>(new HashSet<>())),
                (pair, attr) ->
                    pair(
                        !isNamescape(attr)
                            ? pair.first.add(createAttribute(attr, factory))
                            : pair.first,
                        isNamescape(attr)
                            ? pair.second.add(createNamespace(attr, factory))
                            : pair.second),
                (p1, p2) -> p1);

    return factory.createStartElement(
        getQName(element), split.first.build().iterator(), split.second.build().iterator());
  }

  public static void discardElement(final XMLEventReader reader, final StartElement currentEvent)
      throws XMLStreamException {
    addElement(reader, new DevNullEventWriter(), currentEvent);
  }

  static String escapeText(final String value) {
    return escapeText(value, true);
  }

  private static String escapeText(final String value, final boolean minimum) {
    final StringBuilder builder = new StringBuilder((int) (value.length() * 1.1));

    for (int i = 0; i < value.length(); ++i) {
      if (value.charAt(i) == '&') {
        builder.append("&amp;");
      } else if (value.charAt(i) == '\'' && !minimum) {
        builder.append("&apos;");
      } else if (value.charAt(i) == '>' && !minimum) {
        builder.append("&gt;");
      } else if (value.charAt(i) == '<') {
        builder.append("&lt;");
      } else if (value.charAt(i) == '"' && !minimum) {
        builder.append("&quot;");
      } else {
        builder.append(value.charAt(i));
      }
    }

    return builder.toString().replaceAll("\\]\\]>", "&#x5d;&#x5d;&gt;");
  }

  public static Stream<XMLEvent> events(final XMLEventReader reader) {
    return net.pincette.util.StreamUtil.stream(
        new Iterator<XMLEvent>() {
          private boolean another;

          @Override
          public boolean hasNext() {
            another = reader.hasNext();

            return another;
          }

          @Override
          public XMLEvent next() {
            if (!another) {
              throw new NoSuchElementException();
            }

            return tryToGetRethrow(reader::nextEvent).orElse(null);
          }
        });
  }

  public static Optional<String> getAttribute(final StartElement event, final QName name) {
    return Optional.ofNullable(event.getAttributeByName(name)).map(Attribute::getValue);
  }

  public static XMLEventReader getCloseInputStreamEventReader(
      final XMLEventReader reader, final InputStream in) {
    return new RunAtCloseEventReader(reader, () -> tryToDoRethrow(in::close));
  }

  public static String getDocumentElementNamespaceURI(final InputStream in) {
    return tryToGetWithRethrow(
            autoClose(
                () -> XMLInputFactory.newFactory().createXMLEventReader(in), XMLEventReader::close),
            reader ->
                events(reader)
                    .filter(XMLEvent::isStartElement)
                    .map(e -> e.asStartElement().getName().getNamespaceURI())
                    .findFirst()
                    .orElse(null))
        .orElse(null);
  }

  public static String getElementText(
      final XMLEventReader reader,
      final XMLEvent currentEvent,
      final Map<String, String> entityDeclarations)
      throws XMLStreamException {
    if (!currentEvent.isStartElement()) {
      throw new XMLStreamException(UNEXPECTED);
    }

    return coalesceText(
        events(reader)
            .map(
                event ->
                    must(
                        event,
                        e ->
                            e.isCharacters()
                                || e.isEntityReference()
                                || e.isProcessingInstruction()
                                || e.getEventType() == XMLStreamConstants.COMMENT))
            .filter(e -> e.isCharacters() || e.isEntityReference())
            .collect(toList()),
        entityDeclarations);
  }

  private static int getLength(
      final List<XMLEvent> events, final Map<String, String> entityDeclarations) {
    final Function<EntityReference, Integer> tryEntity =
        e ->
            entityDeclarations != null && entityDeclarations.get(e.getName()) != null
                ? (entityDeclarations.get(e.getName())).length()
                : (e.getName().length() + 2);

    return events
        .stream()
        .mapToInt(
            e ->
                e.isCharacters()
                    ? e.asCharacters().getData().length()
                    : tryEntity.apply((EntityReference) e))
        .sum();
  }

  public static QName getQName(final Node node) {
    final Supplier<QName> withoutPrefix =
        () ->
            node.getNamespaceURI() != null && node.getLocalName() != null
                ? new QName(node.getNamespaceURI(), node.getLocalName())
                : new QName(node.getNodeName());

    return node.getPrefix() != null
        ? new QName(node.getNamespaceURI(), node.getLocalName(), node.getPrefix())
        : withoutPrefix.get();
  }

  public static boolean ignorable(final XMLEvent event) {
    return (event.isCharacters()
            && (event.asCharacters().isIgnorableWhiteSpace()
                || event.asCharacters().isWhiteSpace()))
        || event.isProcessingInstruction()
        || event.getEventType() == XMLStreamConstants.COMMENT;
  }

  public static boolean isEnd(
      final XMLEvent event, final String namespaceURI, final String localName) {
    return event.isEndElement()
        && (namespaceURI == null
            || namespaceURI.equals(event.asEndElement().getName().getNamespaceURI()))
        && localName.equals(event.asEndElement().getName().getLocalPart());
  }

  public static boolean isNamescape(final Attr attribute) {
    return XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI());
  }

  public static boolean isStart(
      final XMLEvent event, final String namespaceURI, final String localName) {
    return event.isStartElement()
        && (namespaceURI == null
            || namespaceURI.equals(event.asStartElement().getName().getNamespaceURI()))
        && localName.equals(event.asStartElement().getName().getLocalPart());
  }

  public static Stream<Namespace> namespaces(final XMLEvent event) {
    return Optional.of(event)
        .filter(e -> e.isStartElement() || e.isEndElement())
        .map(
            e ->
                e.isStartElement()
                    ? e.asStartElement().getNamespaces()
                    : e.asEndElement().getNamespaces())
        .map(a -> (Iterator<Namespace>) a)
        .map(StreamUtil::stream)
        .orElseGet(Stream::empty);
  }

  public static Document newDocument() {
    return tryToGetRethrow(
            () -> DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument())
        .orElse(null);
  }

  public static XMLInputFactory newInputFactory(final boolean validating, final boolean expanding) {
    final XMLInputFactory result = XMLInputFactory.newFactory();

    result.setProperty(XMLInputFactory.SUPPORT_DTD, true);
    result.setProperty(XMLInputFactory.IS_VALIDATING, validating);
    result.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    result.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, expanding);
    result.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true);

    return result;
  }

  public static XMLEvent nextTag(final XMLEventReader reader) throws XMLStreamException {
    return events(reader)
        .filter(Util::ignorable)
        .map(event -> must(event, e -> e.isStartElement() || e.isEndElement()))
        .findFirst()
        .orElseThrow(() -> new XMLStreamException(UNEXPECTED));
  }

  public static StartElement removeAttribute(final StartElement event, final QName name) {
    return changeAttributes(event, e -> withoutAttribute(e, name));
  }

  public static StartElement setAttribute(
      final StartElement event, final QName name, final String value) {
    return setAttribute(event, XMLEventFactory.newFactory().createAttribute(name, value));
  }

  public static StartElement setAttribute(final StartElement event, final Attribute attribute) {
    return changeAttributes(
        event, e -> concat(withoutAttribute(e, attribute.getName()), of(attribute)));
  }

  public static StartElement setNamespace(
      final StartElement event, final String prefix, final String namespaceURI) {
    return setNamespace(
        event,
        prefix == null
            ? XMLEventFactory.newFactory().createNamespace(namespaceURI)
            : XMLEventFactory.newFactory().createNamespace(prefix, namespaceURI));
  }

  public static StartElement setNamespace(final StartElement event, final Namespace namespace) {
    return changeNamespaces(
        event, e -> concat(withoutNamespace(e, namespace.getName()), of(namespace)));
  }

  public static Stream<Attribute> withoutAttribute(final StartElement event, final QName name) {
    return attributes(event).filter(a -> !name.equals(a.getName()));
  }

  public static Stream<Namespace> withoutNamespace(final StartElement event, final QName name) {
    return attributes(event)
        .filter(a -> a instanceof Namespace)
        .filter(a -> !name.equals(a.getName()))
        .map(a -> (Namespace) a);
  }
}
