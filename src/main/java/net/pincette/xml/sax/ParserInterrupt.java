package net.pincette.xml.sax;

import org.xml.sax.SAXException;

/**
 * Can be used to distinguish a parsing error and a wanted interruption of the parser.
 *
 * @author Werner Donné
 */
public class ParserInterrupt extends SAXException {

  public ParserInterrupt(final Exception e) {
    super(e);
  }

  public ParserInterrupt(final String message) {
    super(message);
  }

  public ParserInterrupt(final String message, final Exception e) {
    super(message, e);
  }
}
