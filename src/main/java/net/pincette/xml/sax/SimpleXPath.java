package net.pincette.xml.sax;

import static java.lang.Integer.parseInt;
import static java.lang.Long.MAX_VALUE;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.ofNullable;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.rangeExclusive;
import static net.pincette.util.StreamUtil.takeWhile;
import static net.pincette.util.StreamUtil.zip;
import static net.pincette.xml.Util.ancestors;
import static net.pincette.xml.Util.children;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This class is able to handle abbreviated syntax XPath expressions which contain only path
 * elements consisting of a non-empty qualified name followed by an optional position specification
 * using numbers only. The expression may be relative. The local name may be "*".
 *
 * @author Werner Donn
 */
public class SimpleXPath {
  private SimpleXPath() {}

  /**
   * The <code>namespaceURIMap</code> maps URIs to prefixes. The default namespace, if any, must be
   * mapped to the empty string. The map may be <code>null</code>. If <code>relative</code> is
   * <code>false</code> the expression will start with "/".
   */
  public static String getExpression(
      final PathElement[] path, final Map<String, String> namespaceURIMap, boolean relative) {
    return (relative ? "" : "/")
        + stream(path)
            .map(
                element ->
                    getPrefix(element, namespaceURIMap)
                        + element.localName()
                        + getPositionText(element))
            .collect(Collectors.joining("/"));
  }

  public static Element[] getElementSet(
      final Node contextNode,
      final String expression,
      final Map<String, String> namespacePrefixMap) {
    return getElementSet(
        expression.charAt(0) == '/' ? contextNode.getOwnerDocument() : contextNode,
        getPath(expression, namespacePrefixMap));
  }

  /** The <code>path</code> is relative to the <code>contextNode</code>. */
  public static Element[] getElementSet(final Node contextNode, final PathElement[] path) {
    return takeWhile(
            pair(
                0,
                getMatchingChildren(
                    ofNullable(contextNode).stream()
                        .filter(Element.class::isInstance)
                        .map(n -> (Element) n),
                    path[0])),
            pair -> pair(pair.first + 1, getMatchingChildren(pair.second, path[pair.first + 1])),
            pair -> pair.first < path.length)
        .max(comparingInt(pair -> pair.first))
        .map(pair -> pair.second.toArray(Element[]::new))
        .orElse(new Element[0]);
  }

  private static Stream<Element> getMatchingChildren(
      final Stream<Element> nodes, final PathElement element) {
    return nodes.flatMap(
        node ->
            zip(
                    children(node).filter(n -> nameMatches(n, element)).map(n -> (Element) n),
                    rangeExclusive(0, MAX_VALUE))
                .filter(pair -> element.position() == -1 || element.position() == pair.second)
                .map(pair -> pair.first));
  }

  /**
   * The <code>namespacePrefixMap</code> maps prefixes to URIs, which are supposed to be strings. It
   * may be <code>null</code>. The key of the default namespace is the empty string.
   */
  public static PathElement[] getPath(
      final String expression, final Map<String, String> namespacePrefixMap) {
    return stream(expression.split("/"))
        .map(Segment::new)
        .map(
            segment ->
                new PathElement(
                    namespacePrefixMap.get(segment.prefix), segment.suffix, segment.index))
        .toArray(PathElement[]::new);
  }

  public static PathElement[] getPath(final Element element) {
    return ancestors(element)
        .map(
            ancestor ->
                new PathElement(
                    ancestor.getNamespaceURI(), ancestor.getLocalName(), getPosition(ancestor)))
        .toArray(PathElement[]::new);
  }

  private static long getPosition(final Element element) {
    return ofNullable(element)
        .map(net.pincette.xml.Util::previousSiblings)
        .map(
            s ->
                s.filter(
                        n ->
                            Objects.equals(n.getNamespaceURI(), element.getNamespaceURI())
                                && Objects.equals(n.getLocalName(), element.getLocalName()))
                    .count())
        .map(c -> c + 1)
        .orElse(0L);
  }

  private static String getPositionText(final PathElement element) {
    return Optional.of(element.position()).filter(p -> p != -1).map(p -> "[" + p + "]").orElse("");
  }

  private static String getPrefix(final PathElement element, final Map<String, String> map) {
    return ofNullable(element.namespaceURI())
        .map(map::get)
        .filter(prefix -> !prefix.isEmpty())
        .map(prefix -> prefix + ":")
        .orElse("");
  }

  private static boolean nameMatches(final Node node, final PathElement element) {
    return ("*".equals(element.localName())
            || Objects.equals(node.getLocalName(), element.localName()))
        && Objects.equals(node.getNamespaceURI(), element.namespaceURI());
  }

  public record PathElement(String namespaceURI, String localName, long position) {

    /**
     * The <code>namespaceURI</code> may be <code>null</code>. The <code>localName</code> must not
     * be <code>null</code>. The position must be positive or -1, indicating absence.
     */
    public PathElement {
      if (position < -1) {
        throw new IllegalArgumentException(String.valueOf(position));
      }

      if (localName == null) {
        throw new NullPointerException();
      }
    }
  }

  private static class Segment {
    private final int index;
    private final String prefix;
    private final String suffix;

    private Segment(final String token) {
      final int colon = token.indexOf(':');
      final int startOfIndex = token.indexOf('[');

      this.prefix = colon != -1 ? token.substring(0, colon) : "";
      this.suffix = token.substring(colon + 1, startOfIndex != -1 ? startOfIndex : token.length());
      this.index =
          startOfIndex != -1 ? parseInt(token.substring(startOfIndex + 1, token.length() - 1)) : -1;
    }
  }
}
