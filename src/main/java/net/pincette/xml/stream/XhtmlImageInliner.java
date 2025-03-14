package net.pincette.xml.stream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static net.pincette.io.StreamConnector.copy;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.stream.Util.isStart;
import static net.pincette.xml.stream.Util.setAttribute;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import net.pincette.io.Base64OutputStream;

public class XhtmlImageInliner extends EventReaderDelegateBase {
  private static final String XHTML = "http://www.w3.org/1999/xhtml";

  private final BaseURITracker tracker;

  public XhtmlImageInliner(final String baseUri, final XMLEventReader reader) {
    super(reader);
    tracker = new BaseURITracker(baseUri);
  }

  private XMLEvent inlineImage(final StartElement event) {
    return Optional.ofNullable(event.getAttributeByName(new QName("src")))
        .map(Attribute::getValue)
        .filter(value -> !value.startsWith("data:"))
        .map(this::loadImage)
        .map(src -> setAttribute(event, new QName("src"), src))
        .orElse(event);
  }

  private String loadImage(final String url) {
    return tryToGetRethrow(
            () -> {
              final ByteArrayOutputStream out = new ByteArrayOutputStream();
              final URLConnection connection =
                  new URL(new URL(tracker.getBaseURI()), url).openConnection();
              final String type = connection.getContentType();

              copy(connection.getInputStream(), new Base64OutputStream(out));

              return "data:" + type + ";base64," + out.toString(US_ASCII);
            })
        .orElse(url);
  }

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    final XMLEvent event = super.nextEvent();

    return isStart(event, XHTML, "img") ? inlineImage(event.asStartElement()) : event;
  }
}
