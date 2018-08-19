package net.pincette.xml.stream;

import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.xml.stream.Util.attributes;
import static net.pincette.xml.stream.Util.escapeText;
import static net.pincette.xml.stream.Util.namespaces;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import net.pincette.util.Pair;
import net.pincette.xml.NamespacePrefixMap;

/**
 * This writer honours the <code>javax.xml.transform.disable-output-escaping</code> and <code>
 * javax.xml.transform.enable-output-escaping</code> processing instructions.
 *
 * @author Werner Donn\u00e9
 */
public class EscapePIEventWriter implements XMLEventWriter {
  private final String encoding;
  private final NamespacePrefixMap prefixMap = new NamespacePrefixMap();
  private final Deque<List<Pair<String, String>>> prefixMappings = new ArrayDeque<>();
  private final BufferedWriter writer;
  private NamespaceContext context;
  private boolean escape = true;

  public EscapePIEventWriter(final Writer writer) {
    this(writer, "UTF-8");
  }

  public EscapePIEventWriter(final Writer writer, final String encoding) {
    this.writer = new BufferedWriter(writer);
    this.encoding = encoding;
    prefixMappings.push(new ArrayList<>());
  }

  private static String getName(final QName name) {
    return (name.getPrefix() != null && !XMLConstants.DEFAULT_NS_PREFIX.equals(name.getPrefix())
            ? (name.getPrefix() + ":")
            : "")
        + name.getLocalPart();
  }

  private static String getReplacementText(final EntityReference event) {
    switch (event.getName()) {
      case "amp":
        return "&";
      case "apos":
        return "'";
      case "gt":
        return ">";
      case "lt":
        return "<";
      case "quot":
        return "\"";
      default:
        return event.getDeclaration() != null
            ? event.getDeclaration().getReplacementText()
            : ("&" + event.getName() + ";");
    }
  }

  public void add(final XMLEvent event) throws XMLStreamException {
    if (event.isProcessingInstruction()
        && "javax.xml.transform.disable-output-escaping"
            .equals(((ProcessingInstruction) event).getTarget())) {
      escape = false;
    } else {
      if (event.isProcessingInstruction()
          && "javax.xml.transform.enable-output-escaping"
              .equals(((ProcessingInstruction) event).getTarget())) {
        escape = true;
      } else {
        if (event.isStartElement()) {
          Optional.ofNullable(prefixMappings.peek())
              .ifPresent(
                  list ->
                      list.forEach(
                          mapping -> prefixMap.startPrefixMapping(mapping.first, mapping.second)));

          prefixMappings.push(new ArrayList<>());
        }

        writeEvent(event);

        if (event.isEndElement()) {
          prefixMappings.pop();

          Optional.ofNullable(prefixMappings.peek())
              .ifPresent(
                  list -> list.forEach(mapping -> prefixMap.endPrefixMapping(mapping.first)));
        }
      }
    }

    if (event.isEndDocument()) {
      flush();
    }
  }

  public void add(final XMLEventReader reader) throws XMLStreamException {
    while (reader.hasNext()) {
      add(reader.nextEvent());
    }

    reader.close();
  }

  public void close() throws XMLStreamException {
    try {
      writer.flush();
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void flush() throws XMLStreamException {
    try {
      writer.flush();
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public NamespaceContext getNamespaceContext() {
    return this.context;
  }

  public void setNamespaceContext(final NamespaceContext context) {
    this.context = context;
  }

  public String getPrefix(final String uri) {
    return Optional.ofNullable(prefixMap.getPrefix(uri))
        .orElseGet(() -> context != null ? context.getPrefix(uri) : null);
  }

  public void setDefaultNamespace(final String uri) {
    Optional.ofNullable(prefixMappings.peek())
        .ifPresent(list -> list.add(pair(XMLConstants.DEFAULT_NS_PREFIX, uri)));
  }

  public void setPrefix(final String prefix, final String uri) {
    Optional.ofNullable(prefixMappings.peek()).ifPresent(list -> list.add(pair(prefix, uri)));
  }

  private void writeAttribute(final Attribute attribute) throws IOException {
    writer.write(
        getName(attribute.getName())
            + "=\""
            + attribute.getValue().replaceAll("\"", "&quot;")
            + "\"");
  }

  private void writeCharacters(final Characters event) throws IOException {
    if (event.isCData()) {
      writer.write("<![CDATA[");
      writer.write(event.getData());
      writer.write("]]>");
    } else {
      writer.write(escape ? escapeText(event.getData()) : event.getData());
    }
  }

  private void writeComment(final Comment event) throws IOException {
    writer.write("<!--");
    writer.write(event.getText());
    writer.write("-->");
  }

  private void writeDTD(final DTD event) throws IOException {
    writer.write(event.getDocumentTypeDeclaration());
  }

  private void writeEndElement(final EndElement event) throws IOException {
    writer.write("</" + getName(event.getName()) + ">");
  }

  private void writeEntityReference(final EntityReference event) throws IOException {
    writer.write(!escape ? getReplacementText(event) : ("&" + event.getName() + ";"));
  }

  private void writeEvent(final XMLEvent event) throws XMLStreamException {
    try {
      switch (event.getEventType()) {
        case XMLStreamConstants.CDATA:
        case XMLStreamConstants.CHARACTERS:
        case XMLStreamConstants.SPACE:
          writeCharacters(event.asCharacters());
          break;

        case XMLStreamConstants.COMMENT:
          writeComment((Comment) event);
          break;

        case XMLStreamConstants.DTD:
          writeDTD((DTD) event);
          break;

        case XMLStreamConstants.END_ELEMENT:
          writeEndElement(event.asEndElement());
          break;

        case XMLStreamConstants.ENTITY_REFERENCE:
          writeEntityReference((EntityReference) event);
          break;

        case XMLStreamConstants.PROCESSING_INSTRUCTION:
          writeProcessingInstruction((ProcessingInstruction) event);
          break;

        case XMLStreamConstants.START_DOCUMENT:
          writeStartDocument();
          break;

        case XMLStreamConstants.START_ELEMENT:
          writeStartElement(event.asStartElement());
          break;

        default:
          break;
      }
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  private void writeNamespace(final Namespace namespace) throws IOException {
    writer.write(
        XMLConstants.XMLNS_ATTRIBUTE
            + (namespace.isDefaultNamespaceDeclaration() ? "" : (":" + namespace.getPrefix()))
            + "=\""
            + namespace.getNamespaceURI().replaceAll("\"", "&quot;")
            + "\"");
  }

  private void writeProcessingInstruction(final ProcessingInstruction event) throws IOException {
    writer.write("<?" + event.getTarget() + " " + event.getData() + "?>");
  }

  private void writeStartDocument() throws IOException {
    writer.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
  }

  private void writeStartElement(final StartElement event) throws IOException {
    writer.write("<" + getName(event.getName()));

    namespaces(event)
        .forEach(
            n ->
                tryToDoRethrow(
                    () -> {
                      writer.write(" ");
                      writeNamespace(n);
                    }));

    attributes(event)
        .forEach(
            n ->
                tryToDoRethrow(
                    () -> {
                      writer.write(" ");
                      writeAttribute(n);
                    }));

    writer.write(">");
  }
}
