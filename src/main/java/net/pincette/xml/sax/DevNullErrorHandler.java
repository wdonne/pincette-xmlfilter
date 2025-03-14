package net.pincette.xml.sax;

import org.xml.sax.SAXParseException;

/**
 * Reports nothing.
 *
 * @author Werner Donné
 */
public class DevNullErrorHandler implements org.xml.sax.ErrorHandler {
  public void error(final SAXParseException exception) {
    // Purpose of the class.
  }

  public void fatalError(final SAXParseException exception) {
    // Purpose of the class.
  }

  public void warning(final SAXParseException exception) {
    // Purpose of the class.
  }
}
