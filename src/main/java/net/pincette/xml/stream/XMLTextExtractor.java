package net.pincette.xml.stream;

import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;

import java.io.Reader;
import java.util.Optional;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;

/**
 * Collects all PCDATA from an XML stream.
 *
 * @author Werner Donné
 */
public class XMLTextExtractor extends Reader {
  private final StringBuilder buffer = new StringBuilder();
  private final XMLEventReader reader;
  private boolean eof;

  public XMLTextExtractor(final XMLEventReader reader) {
    this.reader = reader;
  }

  public void close() {
    tryToDoRethrow(reader::close);
  }

  @Override
  public void mark(final int readAheadLimit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int read() {
    final char[] b = new char[1];

    return read(b, 0, b.length) == -1 ? -1 : (0xffff & b[0]);
  }

  @Override
  public int read(final char[] buf) {
    return read(buf, 0, buf.length);
  }

  public int read(final char[] buf, final int off, final int len) {
    if (eof) {
      return -1;
    }

    while (!eof && len > buffer.length()) {
      if (reader.hasNext()) {
        tryToGetRethrow(reader::nextEvent)
            .filter(XMLEvent::isCharacters)
            .ifPresent(event -> buffer.append(event.asCharacters().getData()));
      } else {
        eof = true;
      }
    }

    final int result = Math.min(len, buffer.length());

    buffer.getChars(0, result, buf, off);
    buffer.delete(0, result);

    return result;
  }

  @Override
  public boolean ready() {
    return false;
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long skip(final long n) {
    return Optional.of(read(new char[(int) n])).filter(result -> result != -1).orElse(0);
  }
}
