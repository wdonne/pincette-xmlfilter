package net.pincette.xml.sax;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static net.pincette.util.Collections.reverse;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.last;
import static net.pincette.util.StreamUtil.rangeExclusive;
import static net.pincette.util.StreamUtil.takeWhile;
import static net.pincette.util.StreamUtil.zip;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import net.pincette.util.Pair;
import net.pincette.util.StreamUtil;

/** @author Werner Donn\u00e9 */
public class PathMatch {
  private final boolean absolute;
  private final Map<QName, Entry> map;
  private final Deque<QName> pathElements = new ArrayDeque<>();

  public PathMatch(final QName[][] paths, final boolean absolute) {
    map = createPathMap(paths);
    this.absolute = absolute;
  }

  private static void addPath(final Map<QName, Entry> map, final Stream<QName> path) {
    last(takeWhile(
            path,
            name -> map.computeIfAbsent(name, n -> new Entry()),
            (entry, name) -> entry.map.computeIfAbsent(name, n -> new Entry()),
            m -> true))
        .ifPresent(entry -> entry.accept = true);
  }

  private static Map<QName, Entry> createPathMap(final QName[][] paths) {
    final Map<QName, Entry> result = new HashMap<>();

    // Paths are reversed because they are matched backwards.
    stream(paths).forEach(path -> addPath(result, StreamUtil.stream(reverse(asList(path)))));

    return result;
  }

  private static Stream<Pair<Integer, QName>> numberedPath(final Stream<QName> path) {
    return zip(rangeExclusive(0, MAX_VALUE), path);
  }

  /** Returns <code>true</code> if any of the given paths match, <code>false</code> otherwise. */
  public boolean match() {
    return last(takeWhile(
            numberedPath(pathElements.stream()),
            numberedName -> pair(numberedName.first, map.get(numberedName.second)),
            (numberedEntry, numberedName) ->
                pair(numberedName.first, numberedEntry.second.map.get(numberedName.second)),
            numberedEntry -> numberedEntry.second != null))
        .map(
            numberedEntry ->
                numberedEntry.second.accept
                    && (!absolute || numberedEntry.first == pathElements.size() - 1))
        .orElse(false);
  }

  public void pop() {
    pathElements.pop();
  }

  public void push(final QName pathElement) {
    pathElements.push(pathElement);
  }

  private static class Entry {
    private boolean accept;
    private Map<QName, Entry> map = new HashMap<>();
  }
}
