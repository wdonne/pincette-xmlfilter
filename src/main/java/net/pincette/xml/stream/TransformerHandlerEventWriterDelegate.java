package net.pincette.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;

/**
 * An XMLEventWriter wrapper around an TransformerHandler.
 *
 * @author Werner Donné
 */
public class TransformerHandlerEventWriterDelegate extends EventWriterDelegate {
  private final TransformerHandler handler;
  private final XMLEventWriter wrapper;

  public TransformerHandlerEventWriterDelegate(final TransformerHandler handler) {
    this(handler, null);
  }

  public TransformerHandlerEventWriterDelegate(
      final TransformerHandler handler, final XMLEventWriter writer) {
    this.handler = handler;
    setParent(writer);
    wrapper = new ContentHandlerEventWriter(handler);
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
    if (writer != null) {
      super.setParent(writer);
      handler.setResult(new SAXResult(new EventWriterContentHandler(getParent())));
    }
  }

  @Override
  public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
    wrapper.setPrefix(prefix, uri);
  }
}
