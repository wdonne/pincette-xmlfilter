package net.pincette.xml.stream;

import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * An XMLReader wrapper around an XMLEventReader.
 *
 * @author Werner Donné
 */
public class EventReaderXMLReader implements XMLReader {
  private ContentHandler contentHandler;
  private DTDHandler dtdHandler;
  private EntityResolver entityResolver;
  private ErrorHandler errorHandler;
  private final Map<String, Boolean> features = new HashMap<>();
  private final Map<String, Object> properties = new HashMap<>();
  private final XMLEventReader reader;

  public EventReaderXMLReader(final XMLEventReader reader) {
    this.reader = reader;
  }

  public ContentHandler getContentHandler() {
    return contentHandler;
  }

  public DTDHandler getDTDHandler() {
    return dtdHandler;
  }

  public EntityResolver getEntityResolver() {
    return entityResolver;
  }

  public ErrorHandler getErrorHandler() {
    return errorHandler;
  }

  public boolean getFeature(final String name) {
    return features.get(name) != null && features.get(name);
  }

  public Object getProperty(final String name)
      throws SAXNotRecognizedException, SAXNotSupportedException {
    return properties.get(name);
  }

  public void parse(final InputSource input) throws SAXException {
    if (contentHandler != null) {
      try {
        new StreamEventWriter(new ContentHandlerStreamWriter(contentHandler)).add(reader);
      } catch (XMLStreamException e) {
        throw new SAXException(e);
      }
    }
  }

  public void parse(final String systemId) throws SAXException {
    parse(new InputSource(systemId));
  }

  public void setContentHandler(final ContentHandler contentHandler) {
    this.contentHandler = contentHandler;
  }

  public void setDTDHandler(final DTDHandler dtdHandler) {
    this.dtdHandler = dtdHandler;
  }

  public void setEntityResolver(final EntityResolver entityResolver) {
    this.entityResolver = entityResolver;
  }

  public void setErrorHandler(final ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public void setFeature(final String name, final boolean value) {
    features.put(name, value);
  }

  public void setProperty(final String name, final Object value) {
    properties.put(name, value);
  }
}
