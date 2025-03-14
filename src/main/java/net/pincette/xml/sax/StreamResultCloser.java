package net.pincette.xml.sax;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * This utility class is for placing in front of a TransformerHandler that doesn't close the stream
 * in its StreamResult.
 *
 * @author Werner Donné
 */
public class StreamResultCloser extends XMLFilterImpl {
  private OutputStream out;
  private Writer writer;

  public StreamResultCloser(final OutputStream out) {
    this.out = out;
  }

  public StreamResultCloser(final Writer writer) {
    this.writer = writer;
  }

  @Override
  public void endDocument() throws SAXException {
    super.endDocument();

    try {
      if (out != null) {
        out.close();
      } else if (writer != null) {
        writer.close();
      }
    } catch (IOException e) {
      // Was closed already.
    }
  }
}
