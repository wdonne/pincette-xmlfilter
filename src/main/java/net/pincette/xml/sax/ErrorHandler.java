package net.pincette.xml.sax;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getGlobal;

import java.util.logging.Logger;
import org.xml.sax.SAXParseException;

/**
 * @author Werner Donné
 */
public class ErrorHandler implements org.xml.sax.ErrorHandler {
  private static final Logger LOGGER = getGlobal();

  private final boolean warnings;

  public ErrorHandler(final boolean warnings) {
    this.warnings = warnings;
  }

  public void error(final SAXParseException exception) {
    LOGGER.log(SEVERE, exception, () -> "Error: " + getMessage(exception));
  }

  public void fatalError(final SAXParseException exception) {
    LOGGER.log(SEVERE, exception, () -> "Fatal error: " + getMessage(exception));
  }

  private String getMessage(final SAXParseException e) {
    return (e.getPublicId() != null ? (e.getPublicId() + ": ") : "")
        + (e.getSystemId() != null ? (e.getSystemId() + ": ") : "")
        + "Line "
        + e.getLineNumber()
        + ": "
        + e.getMessage();
  }

  public void warning(final SAXParseException exception) {
    if (warnings) {
      LOGGER.warning(() -> "Warning: " + getMessage(exception));
    }
  }
}
