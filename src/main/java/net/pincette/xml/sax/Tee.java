package net.pincette.xml.sax;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * This XML filter delegates everything to multiple other handlers.
 *
 * @author Werner Donné
 */
public class Tee extends XMLFilterImpl {
  private final ContentHandler[] tubes;

  public Tee(final ContentHandler[] tubes) {
    this.tubes = tubes;
  }

  public Tee(final ContentHandler[] tubes, final XMLReader parent) {
    super(parent);
    this.tubes = tubes;
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException {
    for (final ContentHandler tube : tubes) {
      tube.characters(ch, start, length);
    }

    super.characters(ch, start, length);
  }

  @Override
  public void endDocument() throws SAXException {
    for (final ContentHandler tube : tubes) {
      tube.endDocument();
    }

    super.endDocument();
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    for (final ContentHandler tube : tubes) {
      tube.endElement(namespaceURI, localName, qName);
    }

    super.endElement(namespaceURI, localName, qName);
  }

  @Override
  public void endPrefixMapping(final String prefix) throws SAXException {
    for (final ContentHandler tube : tubes) {
      tube.endPrefixMapping(prefix);
    }

    super.endPrefixMapping(prefix);
  }

  @Override
  public void ignorableWhitespace(final char[] ch, final int start, final int length)
      throws SAXException {
    for (final ContentHandler tube : tubes) {
      tube.ignorableWhitespace(ch, start, length);
    }

    super.ignorableWhitespace(ch, start, length);
  }

  @Override
  public void processingInstruction(final String target, final String data) throws SAXException {
    for (final ContentHandler tube : tubes) {
      tube.processingInstruction(target, data);
    }

    super.processingInstruction(target, data);
  }

  @Override
  public void setDocumentLocator(final Locator locator) {
    for (final ContentHandler tube : tubes) {
      tube.setDocumentLocator(locator);
    }

    super.setDocumentLocator(locator);
  }

  @Override
  public void skippedEntity(final String name) throws SAXException {
    for (final ContentHandler tube : tubes) {
      tube.skippedEntity(name);
    }

    super.skippedEntity(name);
  }

  @Override
  public void startDocument() throws SAXException {
    for (final ContentHandler tube : tubes) {
      tube.startDocument();
    }

    super.startDocument();
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    for (final ContentHandler tube : tubes) {
      tube.startElement(namespaceURI, localName, qName, atts);
    }

    super.startElement(namespaceURI, localName, qName, atts);
  }

  @Override
  public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
    for (final ContentHandler tube : tubes) {
      tube.startPrefixMapping(prefix, uri);
    }

    super.startPrefixMapping(prefix, uri);
  }
}
