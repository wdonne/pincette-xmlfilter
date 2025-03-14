package net.pincette.xml.sax;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Blocks the document events, which is useful for inlining another SAX source in a SAX result.
 *
 * @author Werner Donné
 */
public class GobbleDocumentEvents extends XMLFilterImpl {
  public GobbleDocumentEvents() {}

  public GobbleDocumentEvents(final XMLReader parent) {
    super(parent);
  }

  @Override
  public void endDocument() throws SAXException {
    // Purpose of the class.
  }

  @Override
  public void startDocument() throws SAXException {
    // Purpose of the class.
  }
}
