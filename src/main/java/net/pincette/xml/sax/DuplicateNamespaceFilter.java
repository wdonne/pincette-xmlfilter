package net.pincette.xml.sax;

import static java.util.Optional.ofNullable;
import static net.pincette.util.StreamUtil.stream;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.xml.sax.Util.attributes;
import static net.pincette.xml.sax.Util.reduce;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Filters out namespace declarations that are already in scope and translates namespace
 * declarations in attribute form into prefix mapping events.
 *
 * @author Werner Donné
 */
public class DuplicateNamespaceFilter extends XMLFilterImpl {
  private final Deque<Element> elements = new ArrayDeque<>();

  public DuplicateNamespaceFilter() {}

  public DuplicateNamespaceFilter(final XMLReader parent) {
    super(parent);
  }

  @Override
  public void endDocument() throws SAXException {
    elements.pop();
    super.endDocument();
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    super.endElement(namespaceURI, localName, qName);
    elements
        .pop()
        .syntheticPrefixMap
        .keySet()
        .forEach(k -> tryToDoRethrow(() -> endPrefixMapping(k)));
  }

  @Override
  public void endPrefixMapping(final String prefix) throws SAXException {
    ofNullable(elements.peek())
        .filter(e -> e.prefixMap.remove(prefix) != null)
        .ifPresent(p -> tryToDoRethrow(() -> super.endPrefixMapping(prefix)));
  }

  private static boolean isNamespace(final Attribute attribute) {
    return attribute.localName.startsWith(XMLConstants.XMLNS_ATTRIBUTE);
  }

  private static String prefix(final Attribute attribute) {
    return attribute.localName.indexOf(':') != -1
        ? attribute.localName.substring(attribute.localName.indexOf(':') + 1)
        : "";
  }

  private static Attributes removeNamespaceAttributes(final Attributes atts) {
    return reduce(attributes(atts).filter(a -> !isNamespace(a)));
  }

  @Override
  public void startDocument() throws SAXException {
    elements.push(new Element());
    // An extra level because prefix mapping events come around an element.
    super.startDocument();
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    ofNullable(elements.peek())
        .map(element -> element.syntheticPrefixMap)
        .ifPresent(
            prefixMap ->
                attributes(atts)
                    .filter(DuplicateNamespaceFilter::isNamespace)
                    .forEach(a -> tryToDoRethrow(() -> startPrefixMapping(prefix(a), a.value))));

    elements.push(new Element());
    super.startElement(namespaceURI, localName, qName, removeNamespaceAttributes(atts));
  }

  @Override
  public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
    if (stream(elements.descendingIterator()).noneMatch(e -> uri.equals(e.prefixMap.get(prefix)))) {
      ofNullable(elements.peek()).ifPresent(e -> e.prefixMap.put(prefix, uri));
      super.startPrefixMapping(prefix, uri);
    }
  }

  private static class Element {
    private final Map<String, String> prefixMap = new HashMap<>();
    private final Map<String, String> syntheticPrefixMap = new HashMap<>();
  }
}
