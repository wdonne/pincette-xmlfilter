package net.pincette.xml.stream;

import javax.xml.stream.XMLStreamException;

/**
 * Can be used to indicate a wanted interruption of the writer.
 *
 * @author Werner Donné
 */
public class WriterInterrupt extends XMLStreamException {
  public WriterInterrupt(final Exception e) {
    super(e);
  }

  public WriterInterrupt(final String message) {
    super(message);
  }

  public WriterInterrupt(final String message, final Exception e) {
    super(message, e);
  }
}
