package net.pincette.xml.stream;

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static net.pincette.util.Util.from;
import static net.pincette.util.Util.tryToDoRethrow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * A ContentHandler wrapper around an XMLEventWriter.
 *
 * @author Werner Donn\u00e9
 */
public class EventWriterContentHandler implements ContentHandler {
  private final XMLEventFactory factory = XMLEventFactory.newFactory();
  private final Deque<Map<String, Namespace>> namespaces = new ArrayDeque<>();
  private final List<Namespace> pendingNamespaces = new ArrayList<>();
  private XMLEventWriter writer;

  public EventWriterContentHandler(final XMLEventWriter writer) {
    setWriter(writer);
  }

  public void characters(final char[] ch, final int start, final int length) {
    tryToDoRethrow(() -> writer.add(factory.createCharacters(new String(ch, start, length))));
  }

  public void endDocument() {
    tryToDoRethrow(
        () -> {
          writer.add(factory.createEndDocument());
          writer.flush();
          writer.close();
        });
  }

  public void endElement(final String namespaceURI, final String localName, final String qName) {
    tryToDoRethrow(
        () ->
            writer.add(
                factory.createEndElement(
                    qName.indexOf(':') != -1 ? qName.substring(0, qName.indexOf(':')) : "",
                    namespaceURI,
                    localName,
                    namespaces.pop().values().iterator())));
  }

  public void endPrefixMapping(final String prefix) {
    // Pending namespaces are cleared in startElement.
  }

  public XMLEventWriter getWriter() {
    return writer;
  }

  public void setWriter(final XMLEventWriter writer) {
    this.writer = writer;
  }

  public void ignorableWhitespace(final char[] ch, final int start, final int length) {
    tryToDoRethrow(() -> writer.add(factory.createIgnorableSpace(new String(ch, start, length))));
  }

  public void processingInstruction(final String target, final String data) {
    tryToDoRethrow(() -> writer.add(factory.createProcessingInstruction(target, data)));
  }

  public void setDocumentLocator(final Locator locator) {
    // Not supported.
  }

  public void skippedEntity(final String name) {
    // Not supported.
  }

  public void startDocument() {
    tryToDoRethrow(() -> writer.add(factory.createStartDocument()));
  }

  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    try {
      final List<Attribute> attributes = new ArrayList<>();
      final Map<String, Namespace> inScope = new HashMap<>();

      namespaces.push(inScope);

      for (int i = 0; i < atts.getLength(); ++i) {
        final String name = atts.getQName(i);

        if (XMLNS_ATTRIBUTE_NS_URI.equals(atts.getURI(i))) {
          from(name.indexOf(':') != -1
                  ? factory.createNamespace(name.substring(name.indexOf(':') + 1), atts.getValue(i))
                  : factory.createNamespace(atts.getValue(i)))
              .accept(n -> inScope.put(n.getPrefix(), n));
        } else {
          attributes.add(
              factory.createAttribute(
                  name.indexOf(':') != -1 ? name.substring(0, name.indexOf(':')) : "",
                  atts.getURI(i),
                  atts.getLocalName(i),
                  atts.getValue(i)));
        }
      }

      pendingNamespaces.forEach(n -> inScope.put(n.getPrefix(), n));
      pendingNamespaces.clear();

      writer.add(
          factory.createStartElement(
              qName.indexOf(':') != -1 ? qName.substring(0, qName.indexOf(':')) : "",
              namespaceURI,
              localName,
              attributes.iterator(),
              inScope.values().iterator()));
    } catch (XMLStreamException e) {
      throw new SAXException(e);
    }
  }

  public void startPrefixMapping(final String prefix, final String uri) {
    pendingNamespaces.add(
        "".equals(prefix) ? factory.createNamespace(uri) : factory.createNamespace(prefix, uri));
  }
}
