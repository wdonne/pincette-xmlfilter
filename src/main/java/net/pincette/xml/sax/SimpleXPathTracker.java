package net.pincette.xml.sax;

import static java.util.Optional.ofNullable;
import static net.pincette.util.StreamUtil.stream;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A helper for XML filters. It can give a <code>SimpleXPath</code> at any position.
 *
 * @author Werner Donné
 */
public class SimpleXPathTracker {
  private final Deque<Element> elements = new ArrayDeque<>();

  public SimpleXPath.PathElement[] getXPath() {
    return stream(elements.descendingIterator())
        .map(
            element ->
                new SimpleXPath.PathElement(
                    element.namespaceURI,
                    element.localName,
                    ofNullable(element.parent)
                        .map(p -> sameSiblings(element, p.children))
                        .orElse(1L)))
        .toArray(SimpleXPath.PathElement[]::new);
  }

  public void pop() {
    elements.pop();
  }

  public void push(final String namespaceURI, final String localName) {
    elements.push(
        ofNullable(elements.peek())
            .map(parent -> new Element(namespaceURI, localName, parent))
            .orElseGet(() -> new Element(namespaceURI, localName, null)));
  }

  private static long sameSiblings(final Element element, final List<Element> list) {
    return list.stream()
        .filter(
            e ->
                element.namespaceURI.equals(e.namespaceURI)
                    && element.localName.equals(e.localName))
        .count();
  }

  private static class Element {
    private final List<Element> children = new ArrayList<>();
    private final String localName;
    private final String namespaceURI;
    private final Element parent;

    private Element(final String namespaceURI, final String localName, final Element parent) {
      this.namespaceURI = namespaceURI;
      this.localName = localName;
      this.parent = parent;

      if (parent != null) {
        parent.children.add(this);
      }
    }
  }
}
