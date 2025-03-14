package net.pincette.xml.stream;

import java.io.IOException;
import java.io.Writer;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityDeclaration;

/**
 * @author Werner Donné
 */
public class InternalEntityDeclaration extends XMLEventBase implements EntityDeclaration {
  private final String name;
  private final String text;

  public InternalEntityDeclaration(final String name, final String text) {
    this.name = name;
    this.text = text;
  }

  private static String escapeDoubleQuotes(final String s) {
    return s.replace("\"", "&quot;");
  }

  public String getBaseURI() {
    return null;
  }

  @Override
  public int getEventType() {
    return XMLStreamConstants.ENTITY_DECLARATION;
  }

  public String getName() {
    return name;
  }

  public String getNotationName() {
    return null;
  }

  public String getPublicId() {
    return null;
  }

  public String getReplacementText() {
    return text;
  }

  public String getSystemId() {
    return null;
  }

  @Override
  public void writeAsEncodedUnicode(final Writer writer) throws XMLStreamException {
    try {
      writer.write(
          "<!ENTITY " + getName() + " \"" + escapeDoubleQuotes(getReplacementText()) + "\">");
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }
}
