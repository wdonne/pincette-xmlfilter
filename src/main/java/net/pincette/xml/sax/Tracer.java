package net.pincette.xml.sax;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * This SAX handler writes out the events.
 *
 * @author Werner Donné
 */
public class Tracer extends XMLFilterImpl {
  private final PrintWriter out;
  private int indent = 0;

  public Tracer(final OutputStream out) {
    this.out = new PrintWriter(out);
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException {
    out.println(getIndent() + "characters: " + new String(ch, start, length));
  }

  @Override
  public void endDocument() throws SAXException {
    indent -= 2;
    out.println(getIndent() + "endDocument");
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    indent -= 2;
    out.println(getIndent() + "endElement: " + namespaceURI + ", " + localName + ", " + qName);
  }

  @Override
  public void endPrefixMapping(final String prefix) throws SAXException {
    out.println(getIndent() + "endPrefixMapping: " + prefix);
  }

  private String getIndent() {
    final char[] buffer = new char[indent];

    Arrays.fill(buffer, ' ');

    return new String(buffer);
  }

  @Override
  public void ignorableWhitespace(final char[] ch, final int start, final int length) {
    out.println(getIndent() + "ignorableWhitespace: " + new String(ch, start, length));
  }

  @Override
  public void processingInstruction(final String target, final String data) {
    out.println(getIndent() + "processingInstruction: " + target + ", " + data);
  }

  @Override
  public void setDocumentLocator(final Locator locator) {
    out.println(
        getIndent()
            + "setDocumentLocator: "
            + locator.getPublicId()
            + ", "
            + locator.getSystemId()
            + ", "
            + locator.getLineNumber()
            + ", "
            + locator.getColumnNumber());
  }

  @Override
  public void skippedEntity(final String name) {
    out.println(getIndent() + "skippedEntity: " + name);
  }

  @Override
  public void startDocument() throws SAXException {
    out.println(getIndent() + "startDocument");
    indent += 2;
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    out.print(
        getIndent()
            + "startElement: "
            + namespaceURI
            + ", "
            + localName
            + ", "
            + qName
            + ", atts:");

    for (int i = 0; i < atts.getLength(); ++i) {
      out.print(" " + atts.getQName(i) + "=\"" + atts.getValue(i) + "\"");
    }

    out.println();
    indent += 2;
  }

  @Override
  public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
    out.println(getIndent() + "startPrefixMapping: " + prefix + ", " + uri);
  }
}
