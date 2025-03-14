package net.pincette.xml.sax;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Blocks all ContentHandler events except the document events.
 *
 * @author Werner Donné
 */
public class DevNullFilter extends XMLFilterImpl {
  @Override
  public void characters(final char[] ch, final int start, final int length) {
    // Purpose of the class.
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName) {
    // Purpose of the class.
  }

  @Override
  public void endPrefixMapping(final String prefix) {
    // Purpose of the class.
  }

  @Override
  public void ignorableWhitespace(final char[] ch, final int start, final int length) {
    // Purpose of the class.
  }

  @Override
  public void processingInstruction(final String target, final String data) {
    // Purpose of the class.
  }

  @Override
  public void skippedEntity(final String name) {
    // Purpose of the class.
  }

  @Override
  public void startElement(
      final String namespaceURI,
      final String localName,
      final String qName,
      final Attributes atts) {
    // Purpose of the class.
  }

  @Override
  public void startPrefixMapping(final String prefix, final String uri) {
    // Purpose of the class.
  }
}
