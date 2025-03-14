package net.pincette.xml.stream;

import static java.util.stream.Collectors.toMap;
import static net.pincette.util.Collections.set;
import static net.pincette.xml.stream.Util.attributes;
import static net.pincette.xml.stream.Util.namespaces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import net.pincette.xml.NamespacePrefixMap;

/**
 * An XMLStreamReader wrapper around an XMLEventReader.
 *
 * @author Werner Donné
 */
public class EventStreamReader implements XMLStreamReader {
  private static final Set<Integer> ATTRIBUTE_TYPES = set(ATTRIBUTE, START_ELEMENT);
  private static final Set<Integer> DOCUMENT_TYPES = set(START_DOCUMENT);
  private static final Set<Integer> EVENT_TYPES =
      set(CDATA, CHARACTERS, COMMENT, DTD, ENTITY_REFERENCE, SPACE);
  private static final Set<Integer> LOCAL_NAME_TYPES =
      set(ENTITY_REFERENCE, START_ELEMENT, END_ELEMENT);
  private static final Set<Integer> NAMESPACE_TYPES = set(NAMESPACE, START_ELEMENT, END_ELEMENT);
  private static final Set<Integer> NAME_TYPES = set(END_ELEMENT, START_ELEMENT);
  private static final Set<Integer> PROCESSING_INSTRUCTION_TYPES = set(PROCESSING_INSTRUCTION);
  private static final Set<Integer> TEXT_CHAR_TYPES = set(CDATA, CHARACTERS, SPACE);

  private Map<String, String> entityDeclarations = new HashMap<>();
  private List<Namespace> currentNamespaces = new ArrayList<>();
  private List<Attribute> currentAttributes = new ArrayList<>();
  private XMLEvent currentEvent;
  private final NamespacePrefixMap prefixMap = new NamespacePrefixMap();
  private final XMLEventReader reader;

  public EventStreamReader(final XMLEventReader reader) {
    this.reader =
        new EventReaderDelegate(reader) {
          @Override
          public XMLEvent nextEvent() throws XMLStreamException {
            currentEvent = getParent().nextEvent();

            return currentEvent;
          }
        };
  }

  private void checkAttributeState() {
    checkState(ATTRIBUTE_TYPES);
  }

  private void checkNamespaceState() {
    checkState(NAMESPACE_TYPES);
  }

  private void checkState(final Set<Integer> allowedTypes) {
    if (currentEvent == null || !allowedTypes.contains(currentEvent.getEventType())) {
      throw new IllegalStateException();
    }
  }

  private void checkTextState() {
    if (!hasText()) {
      throw new IllegalStateException();
    }
  }

  private void clearNamespaces() {
    currentNamespaces.clear();

    namespaces(currentEvent).forEach(n -> prefixMap.endPrefixMapping(n.getPrefix()));
  }

  public void close() throws XMLStreamException {
    reader.close();
  }

  private Map<String, String> createEntityDeclarations(final List<EntityDeclaration> entities) {
    return entities.stream()
        .collect(toMap(EntityDeclaration::getName, EntityDeclaration::getReplacementText));
  }

  private Attribute getAttribute(final int index) {
    return currentEvent.isAttribute() ? (Attribute) currentEvent : currentAttributes.get(index);
  }

  public int getAttributeCount() {
    checkAttributeState();

    return currentEvent.isAttribute() ? 1 : currentAttributes.size();
  }

  public String getAttributeLocalName(final int index) {
    checkAttributeState();

    return getAttribute(index).getName().getLocalPart();
  }

  public QName getAttributeName(final int index) {
    checkAttributeState();

    return getAttribute(index).getName();
  }

  public String getAttributeNamespace(final int index) {
    checkAttributeState();

    return getAttribute(index).getName().getNamespaceURI();
  }

  public String getAttributePrefix(final int index) {
    checkAttributeState();

    return getAttribute(index).getName().getPrefix();
  }

  public String getAttributeType(final int index) {
    checkAttributeState();

    return getAttribute(index).getDTDType();
  }

  public String getAttributeValue(final int index) {
    checkAttributeState();

    return getAttribute(index).getValue();
  }

  public String getAttributeValue(final String namespaceURI, final String localName) {
    checkAttributeState();

    final QName name = new QName(namespaceURI == null ? "" : namespaceURI, localName);
    final Function<Attribute, String> tryAttribute =
        a -> name.equals(a.getName()) ? a.getValue() : null;

    return currentEvent.isAttribute()
        ? tryAttribute.apply((Attribute) currentEvent)
        : currentAttributes.stream()
            .filter(a -> name.equals(a.getName()))
            .map(Attribute::getValue)
            .findFirst()
            .orElse(null);
  }

  private List<Attribute> getAttributes() {
    return attributes(currentEvent).toList();
  }

  public String getCharacterEncodingScheme() {
    checkState(DOCUMENT_TYPES);

    return ((StartDocument) currentEvent).getCharacterEncodingScheme();
  }

  public String getElementText() throws XMLStreamException {
    return Util.getElementText(reader, currentEvent, entityDeclarations);
  }

  public String getEncoding() {
    return getCharacterEncodingScheme();
  }

  public int getEventType() {
    return currentEvent != null ? currentEvent.getEventType() : -1;
  }

  public String getLocalName() {
    checkState(LOCAL_NAME_TYPES);

    final Supplier<String> tryEndElement =
        () ->
            currentEvent.isEndElement()
                ? currentEvent.asEndElement().getName().getLocalPart()
                : ((EntityReference) currentEvent).getName();

    return currentEvent.isStartElement()
        ? currentEvent.asStartElement().getName().getLocalPart()
        : tryEndElement.get();
  }

  public Location getLocation() {
    return currentEvent.getLocation();
  }

  public QName getName() {
    checkState(NAME_TYPES);

    return currentEvent.isStartElement()
        ? currentEvent.asStartElement().getName()
        : currentEvent.asEndElement().getName();
  }

  private Namespace getNamespace(final int index) {
    return currentEvent.isNamespace() ? (Namespace) currentEvent : currentNamespaces.get(index);
  }

  public NamespaceContext getNamespaceContext() {
    return new PrefixMapContext();
  }

  public int getNamespaceCount() {
    checkNamespaceState();

    return currentEvent.isNamespace() ? 1 : currentNamespaces.size();
  }

  public String getNamespacePrefix(final int index) {
    checkNamespaceState();

    return getNamespace(index).getPrefix();
  }

  public String getNamespaceURI() {
    return "".equals(getName().getNamespaceURI()) ? null : getName().getNamespaceURI();
  }

  public String getNamespaceURI(final int index) {
    checkNamespaceState();

    return getNamespace(index).getNamespaceURI();
  }

  public String getNamespaceURI(final String prefix) {
    return prefixMap.getNamespacePrefix(prefix);
  }

  private List<Namespace> getNamespaces() {
    return namespaces(currentEvent)
        .peek(n -> prefixMap.startPrefixMapping(n.getPrefix(), n.getNamespaceURI()))
        .toList();
  }

  public String getPIData() {
    checkState(PROCESSING_INSTRUCTION_TYPES);

    return ((ProcessingInstruction) currentEvent).getData();
  }

  public String getPITarget() {
    checkState(PROCESSING_INSTRUCTION_TYPES);

    return ((ProcessingInstruction) currentEvent).getTarget();
  }

  public String getPrefix() {
    return "".equals(getName().getPrefix()) ? null : getName().getPrefix();
  }

  public Object getProperty(final String name) {
    if (currentEvent != null && currentEvent.getEventType() == DTD) {
      if ("javax.xml.stream.entities".equals(name)) {
        final List<EntityDeclaration> result = ((DTD) currentEvent).getEntities();

        entityDeclarations = createEntityDeclarations(result);

        return result;
      }

      if ("javax.xml.stream.notations".equals(name)) {
        return ((DTD) currentEvent).getNotations();
      }
    }

    return reader.getProperty(name);
  }

  public String getText() {
    checkTextState();

    final Supplier<String> dtdOr =
        () ->
            currentEvent.getEventType() == DTD
                ? ((DTD) currentEvent).getDocumentTypeDeclaration()
                : ((EntityReference) currentEvent).getDeclaration().getReplacementText();
    final Supplier<String> commentOr =
        () ->
            currentEvent.getEventType() == COMMENT
                ? ((Comment) currentEvent).getText()
                : dtdOr.get();

    return currentEvent.isCharacters() ? currentEvent.asCharacters().getData() : commentOr.get();
  }

  public char[] getTextCharacters() {
    return getText().toCharArray();
  }

  public int getTextCharacters(
      final int sourceStart, final char[] target, final int targetStart, final int length) {
    checkState(TEXT_CHAR_TYPES);

    final char[] chars = getTextCharacters();

    if (sourceStart < 0
        || sourceStart > chars.length
        || targetStart < 0
        || targetStart >= target.length
        || length < 0
        || targetStart + length > target.length) {
      throw new IndexOutOfBoundsException();
    }

    final int result = Math.min(length, chars.length - sourceStart);

    System.arraycopy(chars, sourceStart, target, targetStart, result);

    return result;
  }

  public int getTextLength() {
    return getText().length();
  }

  public int getTextStart() {
    checkTextState();

    return 0;
  }

  public String getVersion() {
    checkState(DOCUMENT_TYPES);

    return ((StartDocument) currentEvent).getVersion();
  }

  public boolean hasName() {
    return currentEvent.isStartElement() || currentEvent.isEndElement();
  }

  public boolean hasNext() {
    return reader.hasNext();
  }

  public boolean hasText() {
    return EVENT_TYPES.contains(currentEvent.getEventType());
  }

  public boolean isAttributeSpecified(final int index) {
    checkAttributeState();

    return getAttribute(index).isSpecified();
  }

  public boolean isCharacters() {
    return currentEvent != null && currentEvent.isCharacters();
  }

  public boolean isEndElement() {
    return currentEvent != null && currentEvent.isEndElement();
  }

  public boolean isStandalone() {
    checkState(DOCUMENT_TYPES);

    return ((StartDocument) currentEvent).isStandalone();
  }

  public boolean isStartElement() {
    return currentEvent != null && currentEvent.isStartElement();
  }

  public boolean isWhiteSpace() {
    return currentEvent != null
        && currentEvent.isCharacters()
        && (currentEvent.asCharacters().isIgnorableWhiteSpace()
            || currentEvent.asCharacters().isWhiteSpace());
  }

  public int next() throws XMLStreamException {
    currentEvent = reader.nextEvent();

    if (currentEvent.isStartElement() || currentEvent.isEndElement()) {
      currentNamespaces = getNamespaces();

      if (currentEvent.isStartElement()) {
        currentAttributes = getAttributes();
      } else {
        clearNamespaces();
      }
    } else if (currentEvent.isNamespace()) {
      prefixMap.startPrefixMapping(
          ((Namespace) currentEvent).getPrefix(), ((Namespace) currentEvent).getNamespaceURI());
    }

    return currentEvent.getEventType();
  }

  public int nextTag() throws XMLStreamException {
    currentEvent = Util.nextTag(reader);

    return currentEvent.getEventType();
  }

  public void require(final int type, final String namespaceURI, final String localName)
      throws XMLStreamException {
    if (currentEvent == null
        || type != currentEvent.getEventType()
        || (namespaceURI != null && !namespaceURI.equals(getNamespaceURI()))
            && (localName != null && !localName.equals(getLocalName()))) {
      throw new XMLStreamException("Require type " + type);
    }
  }

  public boolean standaloneSet() {
    return isStandalone();
  }

  private class PrefixMapContext implements NamespaceContext {

    public String getNamespaceURI(final String prefix) {
      return prefixMap.getNamespaceURI(prefix);
    }

    public String getPrefix(final String namespaceURI) {
      return prefixMap.getNamespacePrefix(namespaceURI);
    }

    public Iterator<String> getPrefixes(final String namespaceURI) {
      return prefixMap.getCurrentPrefixMap().entrySet().stream()
          .filter(e -> namespaceURI.equals(e.getValue()))
          .map(Map.Entry::getKey)
          .toList()
          .iterator();
    }
  }
}
