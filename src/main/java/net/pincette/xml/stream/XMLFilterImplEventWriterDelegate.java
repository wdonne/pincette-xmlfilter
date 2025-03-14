package net.pincette.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * An XMLEventWriter wrapper around an XMLFilterImpl. This implementation class has been chosen
 * because it both implements <code>org.xml.sax.ContentHandler</code> and <code>
 * org.xml.sax.XMLReader</code>.
 *
 * @author Werner Donné
 */
public class XMLFilterImplEventWriterDelegate extends EventWriterDelegate {
  private final XMLFilterImpl filter;
  private final XMLEventWriter wrapper;

  public XMLFilterImplEventWriterDelegate(final XMLFilterImpl filter) {
    this(filter, null);
  }

  public XMLFilterImplEventWriterDelegate(final XMLFilterImpl filter, final XMLEventWriter writer) {
    this.filter = filter;
    setParent(writer);

    final ContentHandlerStreamWriter delegate = new ContentHandlerStreamWriter();

    wrapper = new StreamEventWriter(delegate);
    filter.setParent(new ReaderStub(delegate));
    // Give the filter a chance to do some set-up.

    if (delegate.getContentHandler() == null) {
      delegate.setContentHandler(filter);
    }
  }

  @Override
  public void add(final XMLEvent event) throws XMLStreamException {
    wrapper.add(event);
  }

  @Override
  public void close() throws XMLStreamException {
    wrapper.close();
  }

  @Override
  public void flush() throws XMLStreamException {
    wrapper.flush();
  }

  @Override
  public String getPrefix(final String uri) throws XMLStreamException {
    return wrapper.getPrefix(uri);
  }

  @Override
  public void setDefaultNamespace(final String uri) throws XMLStreamException {
    wrapper.setDefaultNamespace(uri);
  }

  @Override
  public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
    wrapper.setNamespaceContext(context);
  }

  @Override
  public void setParent(final XMLEventWriter writer) {
    super.setParent(writer);
    filter.setContentHandler(new EventWriterContentHandler(getParent()));
  }

  @Override
  public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
    wrapper.setPrefix(prefix, uri);
  }

  private static class ReaderStub implements XMLReader {
    private DTDHandler dtdHandler;
    private EntityResolver entityResolver;
    private ErrorHandler errorHandler;
    private final ContentHandlerStreamWriter writer;

    public ReaderStub(final ContentHandlerStreamWriter writer) {
      this.writer = writer;
    }

    public ContentHandler getContentHandler() {
      return writer.getContentHandler();
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
      return false;
    }

    public Object getProperty(final String name) {
      return null;
    }

    public void parse(final InputSource input) {
      // Nothing to do.
    }

    public void parse(final String systemId) {
      // Nothing to do.
    }

    public void setContentHandler(final ContentHandler contentHandler) {
      writer.setContentHandler(contentHandler);
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
      // Nothing to do.
    }

    public void setProperty(final String name, final Object value) {
      // Nothing to do.
    }
  }
}
