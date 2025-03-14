package net.pincette.xml.stream;

import javax.xml.stream.XMLStreamException;

/**
 * Can be used to distinguish a parsing error and a wanted interruption of the reader.
 *
 * @author Werner Donné
 */
public class ReaderInterrupt extends XMLStreamException {
  public ReaderInterrupt(final Exception e) {
    super(e);
  }

  public ReaderInterrupt(final String message) {
    super(message);
  }

  public ReaderInterrupt(final String message, final Exception e) {
    super(message, e);
  }
}
