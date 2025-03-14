package net.pincette.xml.sax;

import static net.pincette.util.Util.tryToDoRethrow;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * An <code>XMLReader</code> which produces SAX events from another source than a parser.
 *
 * @author Werner Donné
 */
public class AlternateSourceReader implements XMLReader {
  private final Generate generate;
  private ContentHandler contentHandler;
  private DTDHandler dtdHandler;
  private EntityResolver entityResolver;
  private ErrorHandler errorHandler;

  public AlternateSourceReader(final Generate generate) {
    this.generate = generate;
  }

  public ContentHandler getContentHandler() {
    return contentHandler;
  }

  public void setContentHandler(final ContentHandler contentHandler) {
    this.contentHandler = contentHandler;
  }

  public DTDHandler getDTDHandler() {
    return dtdHandler;
  }

  public void setDTDHandler(final DTDHandler dtdHandler) {
    this.dtdHandler = dtdHandler;
  }

  public EntityResolver getEntityResolver() {
    return entityResolver;
  }

  public void setEntityResolver(final EntityResolver entityResolver) {
    this.entityResolver = entityResolver;
  }

  public ErrorHandler getErrorHandler() {
    return errorHandler;
  }

  public void setErrorHandler(final ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public boolean getFeature(final String name) throws SAXNotSupportedException {
    throw new SAXNotSupportedException("Features are not supported.");
  }

  public Object getProperty(final String name) throws SAXNotSupportedException {
    throw new SAXNotSupportedException("Properties are not supported.");
  }

  public void parse(final InputSource input) {
    if (generate != null) {
      tryToDoRethrow(() -> generate.generate(this));
    }
  }

  public void parse(final String systemId) {
    parse(new InputSource(systemId));
  }

  public void setFeature(final String name, final boolean value) throws SAXNotSupportedException {
    throw new SAXNotSupportedException("Features are not supported.");
  }

  public void setProperty(final String name, final Object value) throws SAXNotSupportedException {
    throw new SAXNotSupportedException("Properties are not supported.");
  }

  @FunctionalInterface
  public interface Generate {
    void generate(XMLReader reader) throws SAXException;
  }
}
