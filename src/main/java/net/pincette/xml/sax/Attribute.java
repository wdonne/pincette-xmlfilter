package net.pincette.xml.sax;

import org.xml.sax.Attributes;

/**
 * @author Werner Donné
 */
public class Attribute {
  public final String localName;
  public final String namespaceURI;
  public final String qName;
  public final String type;
  public final String value;

  public Attribute(final Attribute attribute) {
    this(
        attribute.localName,
        attribute.namespaceURI,
        attribute.qName,
        attribute.type,
        attribute.value);
  }

  public Attribute(
      final String localName,
      final String namespaceURI,
      final String qName,
      final String type,
      final String value) {
    this.localName = localName;
    this.namespaceURI = namespaceURI;
    this.qName = qName;
    this.type = type;
    this.value = value;
  }

  public Attribute(final Attributes atts, final int index) {
    this(
        atts.getLocalName(index),
        atts.getURI(index),
        atts.getQName(index),
        atts.getType(index),
        atts.getValue(index));
  }

  public Attribute withLocalName(final String localName) {
    return new Attribute(localName, namespaceURI, qName, type, value);
  }

  public Attribute withNamespaceURI(final String namespaceURI) {
    return new Attribute(localName, namespaceURI, qName, type, value);
  }

  public Attribute withQName(final String qName) {
    return new Attribute(localName, namespaceURI, qName, type, value);
  }

  public Attribute withType(final String type) {
    return new Attribute(localName, namespaceURI, qName, type, value);
  }

  public Attribute withValue(final String value) {
    return new Attribute(localName, namespaceURI, qName, type, value);
  }
}
