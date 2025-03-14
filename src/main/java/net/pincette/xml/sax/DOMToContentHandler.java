package net.pincette.xml.sax;

import static java.util.Optional.ofNullable;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.from;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.xml.Util.attributes;
import static org.w3c.dom.Node.ELEMENT_NODE;
import static org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE;
import static org.w3c.dom.Node.TEXT_NODE;

import java.util.List;
import java.util.Optional;
import net.pincette.function.SideEffect;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * With this class, a DOM document can be fed into a SAX chain.
 *
 * @author Werner Donné
 */
public class DOMToContentHandler {
  private static final String XMLNS = "xmlns";

  private DOMToContentHandler() {}

  /**
   * Extracts the attributes of an element for use in a SAX-environment. It leaves out namespace
   * declarations, which are emitted as events.
   */
  public static Attributes createAttributes(final Element element) {
    return attributes(element)
        .filter(attr -> !isNamespaceDeclaration(attr))
        .reduce(
            new AttributeBuilder(),
            (b, a) ->
                b.add(
                    ofNullable(a.getNamespaceURI()).orElse(""),
                    ofNullable(a.getLocalName()).orElseGet(a::getName),
                    a.getName(),
                    a.isId() ? "ID" : "CDATA",
                    a.getValue()),
            (b1, b2) -> b1)
        .build();
  }

  /** Runs a complete DOM-document through a <code>ContentHandler</code>. */
  public static void documentToContentHandler(final Document document, final ContentHandler handler)
      throws SAXException {
    final XMLFilterImpl h = realHandler(handler);

    h.startDocument();
    elementToContentHandler(document.getDocumentElement(), h);
    h.endDocument();
  }

  /** Runs a complete DOM-element through a <code>ContentHandler</code>. */
  public static void elementToContentHandler(final Element element, final ContentHandler handler)
      throws SAXException {
    elementToContentHandler(element, realHandler(handler));
  }

  private static void elementToContentHandler(final Element element, final XMLFilterImpl handler)
      throws SAXException {
    final List<String> prefixes = startPrefixMappings(element, handler);

    startElement(element, handler);
    siblingsToContentHandler(element.getFirstChild(), handler);
    endElement(element, handler);
    prefixes.forEach(p -> tryToDoRethrow(() -> handler.endPrefixMapping(p)));
  }

  public static void endElement(final Element element, final ContentHandler handler)
      throws SAXException {
    if (element.getNamespaceURI() != null) {
      handler.endElement(
          element.getNamespaceURI(),
          element.getLocalName(),
          element.getPrefix() != null
              ? (element.getPrefix() + ":" + element.getLocalName())
              : element.getLocalName());
    } else {
      handler.endElement("", element.getTagName(), element.getTagName());
    }
  }

  private static boolean isNamespaceDeclaration(final Attr attribute) {
    return XMLNS.equals(attribute.getName()) || attribute.getName().startsWith(XMLNS + ":");
  }

  private static String prefix(final Attr attribute) {
    return Optional.of(attribute.getName())
        .filter(name -> name.startsWith(XMLNS) && name.length() > XMLNS.length())
        .map(name -> name.substring(name.indexOf(':') + 1))
        .orElse("");
  }

  /**
   * Puts an <code>XMLFilterImpl</code> in front of <code>handler</code> in order to provide the
   * possiblity to the latter to insert an <code>Accumulator</code> dynamically.
   */
  private static XMLFilterImpl realHandler(final ContentHandler handler) {
    final XMLFilterImpl result = new XMLFilterImpl();

    result.setContentHandler(handler);

    if (handler instanceof XMLFilter x) {
      x.setParent(result);
    }

    return result;
  }

  /**
   * Runs a complete sibling list through a <code>ContentHandler</code> starting with <code>node
   * </code>.
   */
  public static void siblingsToContentHandler(final Node node, final ContentHandler handler)
      throws SAXException {
    siblingsToContentHandler(node, realHandler(handler));
  }

  private static void siblingsToContentHandler(final Node node, final XMLFilterImpl handler)
      throws SAXException {
    if (node == null) {
      return;
    }

    switch (node.getNodeType()) {
      case ELEMENT_NODE:
        elementToContentHandler((Element) node, handler);
        break;
      case PROCESSING_INSTRUCTION_NODE:
        handler.processingInstruction(
            ((ProcessingInstruction) node).getTarget(), ((ProcessingInstruction) node).getData());
        break;
      case TEXT_NODE:
        from(((Text) node).getData().toCharArray()).accept(c -> handler.characters(c, 0, c.length));
        break;
      default:
        break;
    }

    siblingsToContentHandler(node.getNextSibling(), handler);
  }

  public static void startElement(final Element element, final ContentHandler handler)
      throws SAXException {
    if (element.getNamespaceURI() != null) {
      handler.startElement(
          element.getNamespaceURI(),
          element.getLocalName(),
          element.getPrefix() != null
              ? (element.getPrefix() + ":" + element.getLocalName())
              : element.getLocalName(),
          createAttributes(element));
    } else {
      handler.startElement(
          "", element.getTagName(), element.getTagName(), createAttributes(element));
    }
  }

  private static List<String> startPrefixMappings(
      final Element element, final ContentHandler handler) {
    return attributes(element)
        .filter(attr -> attr.getName().startsWith(XMLNS))
        .map(attr -> pair(attr, prefix(attr)))
        .map(
            pair ->
                SideEffect.<String>run(
                        () ->
                            tryToDoRethrow(
                                () ->
                                    handler.startPrefixMapping(pair.second, pair.first.getValue())))
                    .andThenGet(() -> pair.second))
        .toList();
  }
}
