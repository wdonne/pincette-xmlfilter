package net.pincette.xml.stream;

import static java.nio.file.Files.delete;
import static net.pincette.util.Util.tryToDoRethrow;

import java.io.File;
import java.io.FileInputStream;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

/**
 * An XMLEventReader wrapper around an TransformerHandler.
 *
 * @author Werner Donn\u00e9
 */
public class TransformerHandlerEventReaderDelegate extends EventReaderDelegate {
  private final XMLInputFactory factory = XMLInputFactory.newFactory();
  private final TransformerHandler handler;
  private boolean closed;
  private FileInputStream in;
  private boolean initialized;
  private File tmpFile;
  private XMLEventReader reader;

  public TransformerHandlerEventReaderDelegate(final TransformerHandler handler) {
    this(handler, null);
  }

  public TransformerHandlerEventReaderDelegate(
      final TransformerHandler handler, final XMLEventReader reader) {
    super(reader);
    this.handler = handler;
  }

  @Override
  public void close() throws XMLStreamException {
    if (!closed) {
      reader.close();

      tryToDoRethrow(
          () -> {
            in.close();
            delete(tmpFile.toPath());
          });

      closed = true;
    }
  }

  @Override
  public String getElementText() throws XMLStreamException {
    return reader.getElementText();
  }

  @Override
  public boolean hasNext() {
    if (!initialized) {
      tryToDoRethrow(() -> {
        final XMLEventWriter writer = new ContentHandlerEventWriter(handler);

        tmpFile = File.createTempFile("TransformerHandlerEventReaderDelegate.", ".xml");
        tmpFile.deleteOnExit();
        handler.setResult(new StreamResult(tmpFile));
        writer.add(getParent());
        writer.flush();
        writer.close();

        in = new FileInputStream(tmpFile);
        reader = factory.createXMLEventReader(in);
        initialized = true;
      });
    }

    return reader.hasNext();
  }

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    return reader.nextEvent();
  }

  @Override
  public XMLEvent nextTag() throws XMLStreamException {
    return reader.nextTag();
  }

  @Override
  public XMLEvent peek() throws XMLStreamException {
    return reader.peek();
  }
}
