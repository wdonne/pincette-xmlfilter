package net.pincette.xml.stream;


/**
 * Writes nothing.
 *
 * @author Werner Donné
 */
public class DevNullStreamWriter extends StreamWriterDelegate {
  public DevNullStreamWriter() {
    super(new EventStreamWriter(new DevNullEventWriter()));
  }
}
