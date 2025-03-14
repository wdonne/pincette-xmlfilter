package net.pincette.xml.stream;

import org.xml.sax.ContentHandler;

/**
 * A ContentHandler wrapper.
 *
 * @author Werner Donné
 */
public class ContentHandlerStreamWriter extends StreamWriterDelegate {
  public ContentHandlerStreamWriter() {
    this(null);
  }

  public ContentHandlerStreamWriter(final ContentHandler handler) {
    super(new EventStreamWriter(new ContentHandlerEventWriter(handler)));
  }

  public ContentHandler getContentHandler() {
    return ((ContentHandlerEventWriter) super.delegate).getContentHandler();
  }

  public void setContentHandler(final ContentHandler handler) {
    ((ContentHandlerEventWriter) super.delegate).setContentHandler(handler);
  }
}
