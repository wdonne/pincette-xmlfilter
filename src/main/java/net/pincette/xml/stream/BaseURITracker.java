package net.pincette.xml.stream;

import static net.pincette.util.Util.isUri;
import static net.pincette.util.Util.tryToGetRethrow;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

/**
 * Determines the base URI in scope. Filters should send it all of their events.
 *
 * @author Werner Donn\u00e9
 */
public class BaseURITracker {
  private final Deque<String> baseURIs = new ArrayDeque<>();
  private final String baseURI;

  public BaseURITracker() {
    this(null);
  }

  /**
   * @param baseURI the base URI of the document. If it is <code>null</code> the document element
   *     should have an <code>xml:base</code> attribute with an absolute URI.
   */
  public BaseURITracker(final String baseURI) {
    this.baseURI = baseURI;
  }

  public void add(final XMLEvent event) {
    if (event.isStartElement()) {
      baseURIs.push(
          Optional.ofNullable(
                  event
                      .asStartElement()
                      .getAttributeByName(
                          new QName(XMLConstants.XML_NS_URI, "base", XMLConstants.XML_NS_PREFIX)))
              .map(Attribute::getValue)
              .map(this::resolveURI)
              .orElse(getBaseURI()));
    } else {
      if (event.isEndElement()) {
        baseURIs.pop();
      }
    }
  }

  public String getBaseURI() {
    return baseURIs.isEmpty() ? baseURI : baseURIs.peek();
  }

  private String resolveURI(final String uri) {
    return tryToGetRethrow(() -> isUri(uri) ? uri : new URL(new URL(getBaseURI()), uri).toString())
        .orElse(null);
  }
}
