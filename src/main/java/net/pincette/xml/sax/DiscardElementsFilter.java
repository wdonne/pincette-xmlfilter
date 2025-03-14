package net.pincette.xml.sax;

import static java.util.Arrays.asList;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Drops the given elements from the stream.
 *
 * @author Werner Donné
 */
public class DiscardElementsFilter extends XMLFilterImpl {
  private final Deque<QName> stack = new ArrayDeque<>();
  private final Set<QName> names;

  public DiscardElementsFilter(final QName[] names) {
    this(names, null);
  }

  public DiscardElementsFilter(final QName[] names, final XMLReader parent) {
    super(parent);
    this.names = new HashSet<>(asList(names));
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException {
    if (stack.isEmpty()) {
      super.characters(ch, start, length);
    }
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    if (!stack.isEmpty()) {
      stack.pop();
    } else {
      super.endElement(namespaceURI, localName, qName);
    }
  }

  @Override
  public void endPrefixMapping(final String prefix) throws SAXException {
    if (stack.isEmpty()) {
      super.endPrefixMapping(prefix);
    }
  }

  @Override
  public void ignorableWhitespace(final char[] ch, final int start, final int length)
      throws SAXException {
    if (stack.isEmpty()) {
      super.ignorableWhitespace(ch, start, length);
    }
  }

  @Override
  public void processingInstruction(final String target, final String data) throws SAXException {
    if (stack.isEmpty()) {
      super.processingInstruction(target, data);
    }
  }

  @Override
  public void skippedEntity(final String name) throws SAXException {
    if (stack.isEmpty()) {
      super.skippedEntity(name);
    }
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    if (!stack.isEmpty() || names.contains(new QName(namespaceURI, localName))) {
      stack.push(new QName(namespaceURI, localName));
    } else {
      super.startElement(namespaceURI, localName, qName, atts);
    }
  }

  @Override
  public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
    if (stack.isEmpty()) {
      super.startPrefixMapping(prefix, uri);
    }
  }
}
