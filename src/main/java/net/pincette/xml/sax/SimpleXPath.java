package net.pincette.xml.sax;

import static java.lang.Long.MAX_VALUE;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparingInt;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.rangeExclusive;
import static net.pincette.util.StreamUtil.takeWhile;
import static net.pincette.util.StreamUtil.zip;
import static net.pincette.util.Triple.triple;
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
 * @author Werner Donn\u00e9
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
                        + element.getLocalName()
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
                    Optional.ofNullable(contextNode).map(Stream::of).orElseGet(Stream::empty),
                    path[0])),
            pair -> pair(pair.first + 1, getMatchingChildren(pair.second, path[pair.first + 1])),
            pair -> pair.first < path.length)
        .max(comparingInt(pair -> pair.first))
        .map(pair -> pair.second.toArray(Element[]::new))
        .orElse(new Element[0]);
  }

  private static Stream<Node> getMatchingChildren(
      final Stream<Node> nodes, final PathElement element) {
    return nodes.flatMap(
        node ->
            zip(
                    children(node).filter(n -> nameMatches(n, element)).map(n -> (Element) n),
                    rangeExclusive(0, MAX_VALUE))
                .filter(pair -> element.getPosition() == -1 || element.getPosition() == pair.second)
                .map(pair -> pair.first));
  }

  /**
   * The <code>namespacePrefixMap</code> maps prefixes to URIs, which are supposed to be strings. It
   * may be <code>null</code>. The key of the default namespace is the empty string.
   */
  public static PathElement[] getPath(
      final String expression, final Map<String, String> namespacePrefixMap) {
    return stream(expression.split("/"))
        .map(
            token ->
                triple(
                    token,
                    token.indexOf(':') != -1 ? (token.indexOf(':') + 1) : 0,
                    token.indexOf('[')))
        .map(
            triple ->
                new PathElement(
                    triple.second > 0
                        ? namespacePrefixMap.get(triple.first.substring(0, triple.second - 1))
                        : namespacePrefixMap.get(""),
                    triple.first.substring(
                        triple.second, triple.third != -1 ? triple.third : triple.first.length()),
                    triple.third != -1
                        ? Integer.parseInt(
                            triple.first.substring(triple.third + 1, triple.first.length() - 1))
                        : -1))
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
    return Optional.ofNullable(element)
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
    return Optional.of(element.getPosition())
        .filter(p -> p != -1)
        .map(p -> "[" + p + "]")
        .orElse("");
  }

  private static String getPrefix(final PathElement element, final Map<String, String> map) {
    return Optional.ofNullable(element.getNamespaceURI())
        .map(map::get)
        .filter(prefix -> !"".equals(prefix))
        .map(prefix -> prefix + ":")
        .orElse("");
  }

  private static boolean nameMatches(final Node node, final PathElement element) {
    return ("*".equals(element.getLocalName())
            || Objects.equals(node.getLocalName(), element.getLocalName()))
        && Objects.equals(node.getNamespaceURI(), element.getNamespaceURI());
  }

  public static class PathElement {
    private String localName;
    private String namespaceURI;
    private long position;

    /**
     * The <code>namespaceURI</code> may be <code>null</code>. The <code>localName</code> must not
     * be <code>null</code>. The position must be positive or -1, indicating absence.
     */
    public PathElement(final String namespaceURI, final String localName, final long position) {
      if (position < -1) {
        throw new IllegalArgumentException(String.valueOf(position));
      }

      if (localName == null) {
        throw new NullPointerException();
      }

      this.namespaceURI = namespaceURI;
      this.localName = localName;
      this.position = position;
    }

    public String getLocalName() {
      return localName;
    }

    public String getNamespaceURI() {
      return namespaceURI;
    }

    public long getPosition() {
      return position;
    }
  }
}
