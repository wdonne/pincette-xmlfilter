package net.pincette.xml.stream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

/**
 * Let you run something after the reader has been closed.
 *
 * @author Werner Donné
 */
public class RunAtCloseEventReader extends EventReaderDelegateBase {
  private final Runnable run;

  public RunAtCloseEventReader(final XMLEventReader reader, final Runnable run) {
    super(reader);
    this.run = run;
  }

  @Override
  public void close() throws XMLStreamException {
    super.close();

    if (run != null) {
      run.run();
    }
  }
}
