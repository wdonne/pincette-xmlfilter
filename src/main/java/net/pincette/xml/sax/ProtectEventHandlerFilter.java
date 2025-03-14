package net.pincette.xml.sax;

import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * If the parent already has an entity resolver on it, it will not be replaced by this filter.
 *
 * @author Werner Donné
 */
public class ProtectEventHandlerFilter extends XMLFilterImpl {
  private final boolean entityResolver;
  private final boolean errorHandler;

  public ProtectEventHandlerFilter(final boolean entityResolver, final boolean errorHandler) {
    this.entityResolver = entityResolver;
    this.errorHandler = errorHandler;
  }

  public ProtectEventHandlerFilter(
      final boolean entityResolver, final boolean errorHandler, final XMLReader parent) {
    super(parent);
    this.entityResolver = entityResolver;
    this.errorHandler = errorHandler;
  }

  @Override
  public void parse(final InputSource input) throws IOException, SAXException {
    if (getParent() != null) {
      setupParser();
      getParent().parse(input);
    }
  }

  @Override
  public void parse(final String systemId) throws IOException, SAXException {
    if (getParent() != null) {
      setupParser();
      getParent().parse(systemId);
    }
  }

  private void setupParser() {
    if (!entityResolver || getParent().getEntityResolver() == null) {
      getParent().setEntityResolver(this);
    }

    getParent().setContentHandler(this);
    getParent().setDTDHandler(this);

    if (!errorHandler || getParent().getErrorHandler() == null) {
      getParent().setErrorHandler(this);
    }
  }
}
