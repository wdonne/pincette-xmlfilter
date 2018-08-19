package net.pincette.xml.stream;

import static net.pincette.util.Util.replaceParameters;
import static net.pincette.xml.stream.Util.attributes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Replaces occurrences of ${name} with the value in <code>parameters</code>.
 *
 * @author Werner Donn\u00e9
 */
public class ReplaceParametersEventReader extends EventReaderDelegateBase {
  private final XMLEventFactory factory = XMLEventFactory.newInstance();
  private final Set<String> leave;
  private final Map<String, String> parameters;

  public ReplaceParametersEventReader(final Map<String, String> parameters) {
    this(parameters, new HashSet<>());
  }

  public ReplaceParametersEventReader(
      final Map<String, String> parameters, final Set<String> leave) {
    this.parameters = parameters;
    this.leave = leave;
  }

  public ReplaceParametersEventReader(
      final Map<String, String> parameters, final XMLEventReader reader) {
    this(parameters, new HashSet<>(), reader);
  }

  public ReplaceParametersEventReader(
      final Map<String, String> parameters, final Set<String> leave, final XMLEventReader reader) {
    super(reader);
    this.parameters = parameters;
    this.leave = leave;
  }

  public XMLEvent nextEvent() throws XMLStreamException {
    final XMLEvent event = super.nextEvent();
    final Supplier<XMLEvent> startElementOr =
        () -> event.isStartElement() ? replaceAttributes(event.asStartElement()) : event;

    return event.isCharacters()
        ? factory.createCharacters(
            replaceParameters(event.asCharacters().getData(), parameters, leave))
        : startElementOr.get();
  }

  private StartElement replaceAttributes(final StartElement event) {
    return factory.createStartElement(
        event.getName(),
        attributes(event)
            .map(
                a ->
                    factory.createAttribute(
                        a.getName(), replaceParameters(a.getValue(), parameters, leave)))
            .iterator(),
        event.getNamespaces());
  }
}
