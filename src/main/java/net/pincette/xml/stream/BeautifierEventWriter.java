package net.pincette.xml.stream;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/** @author Werner Donn\u00e9 */
public class BeautifierEventWriter extends EventWriterDelegate {
  private final XMLEventFactory factory;
  private int indent = 0;
  private final Set<QName> inlineElements;
  private final Deque<Boolean> inlineSeen = new ArrayDeque<>();

  public BeautifierEventWriter(final XMLEventWriter writer) {
    this(null, writer);
  }

  public BeautifierEventWriter(final Set<QName> inlineElements, final XMLEventWriter writer) {
    super(writer);
    this.factory = XMLEventFactory.newFactory();
    this.inlineElements = inlineElements;
  }

  public void add(final XMLEvent event) throws XMLStreamException {
    if (event.isStartElement()) {
      handleStartElement(event.asStartElement());
    } else {
      if (event.isEndElement()) {
        handleEndElement(event.asEndElement());
      } else {
        if (event.isCharacters()) {
          setInlineSeen(true);
        }
      }
    }

    super.add(event);
  }

  private void addPrefix() throws XMLStreamException {
    final char[] c = new char[1 + indent];

    Arrays.fill(c, ' ');
    c[0] = '\n';
    super.add(factory.createCharacters(new String(c)));
  }

  private void handleEndElement(final EndElement event) throws XMLStreamException {
    if (inlineElements == null || !inlineElements.contains(event.getName())) {
      indent -= 2;

      if (!Optional.ofNullable(inlineSeen.peek()).orElse(false)) {
        addPrefix();
      }
    }

    inlineSeen.pop();
  }

  private void handleStartElement(final StartElement event) throws XMLStreamException {
    if (inlineElements == null || !inlineElements.contains(event.getName())) {
      addPrefix();
      indent += 2;
    } else {
      setInlineSeen(true);
    }

    inlineSeen.push(false);
  }

  private void setInlineSeen(final boolean value) {
    inlineSeen.pop();
    inlineSeen.push(value);
  }
}
