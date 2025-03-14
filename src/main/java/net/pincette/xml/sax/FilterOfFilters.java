package net.pincette.xml.sax;

import static java.util.Arrays.stream;
import static net.pincette.util.Util.tryToGetRethrow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import net.pincette.io.FlushOutputStream;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * The given array of filters is connected into a filter-chain and encapsulated in this filter, so
 * you can insert the whole in a chain as one filter. The wiring of the event chains will be
 * interrupted for those filters that don't implement the corresponding handler interfaces.
 *
 * @author Werner Donné
 */
public class FilterOfFilters extends XMLFilterImpl {
  private XMLFilter first;
  private XMLFilter last;

  public FilterOfFilters(final XMLFilter[] filters) {
    this(filters, false);
  }

  public FilterOfFilters(final XMLFilter[] filters, final boolean debug) {
    this(filters, debug, (Set<String>) null);
  }

  public FilterOfFilters(
      final XMLFilter[] filters, final boolean debug, final Set<String> includeClassNames) {
    setupChain(debug ? addDebug(filters, includeClassNames) : filters);
  }

  public FilterOfFilters(final XMLFilter[] filters, final XMLReader parent) {
    this(filters, false, parent);
  }

  public FilterOfFilters(final XMLFilter[] filters, final boolean debug, final XMLReader parent) {
    this(filters, debug, null, parent);
  }

  public FilterOfFilters(
      final XMLFilter[] filters,
      final boolean debug,
      final Set<String> includeClassNames,
      final XMLReader parent) {
    setupChain(debug ? addDebug(filters, includeClassNames) : filters);
    setParent(parent);
  }

  private static boolean anyFilter(final XMLFilter[] filters, final Set<String> includeClassNames) {
    return stream(filters)
        .map(filter -> filter.getClass().getName())
        .anyMatch(includeClassNames::contains);
  }

  private static void connectFilters(final XMLFilter first, final XMLFilter second) {
    first.setParent(second);

    // The following connections make it work also when this filter is
    // inserted in a chain that is already running, i.e., for which parse is
    // already called.

    if (first instanceof ContentHandler h) {
      second.setContentHandler(h);
    }

    if (first instanceof DTDHandler h) {
      second.setDTDHandler(h);
    }

    if (first instanceof EntityResolver h) {
      second.setEntityResolver(h);
    }

    if (first instanceof ErrorHandler h) {
      second.setErrorHandler(h);
    }
  }

  private static ContentHandler outputHandler(final String filename) {
    return tryToGetRethrow(
            () -> {
              final TransformerHandler handler =
                  Util.newSAXTransformerFactory().newTransformerHandler();

              handler.setResult(
                  new StreamResult(new FlushOutputStream(new FileOutputStream(filename))));

              return handler;
            })
        .orElse(null);
  }

  private XMLFilter[] addDebug(final XMLFilter[] filters, final Set<String> includeClassNames) {
    final XMLFilter[] result = new XMLFilter[filters.length * 2 + 1];

    result[0] =
        includeClassNames == null || anyFilter(filters, includeClassNames)
            ? new Tee(new ContentHandler[] {outputHandler(toString() + "_input.xml")})
            : new XMLFilterImpl();

    for (int i = 0; i < filters.length; ++i) {
      result[i * 2 + 1] = filters[i];
      result[i * 2 + 2] =
          includeClassNames == null || includeClassNames.contains(filters[i].getClass().getName())
              ? new Tee(
                  new ContentHandler[] {
                    new BalanceChecker(new File(this + "_" + filters[i].toString() + ".balance")),
                    outputHandler(this + "_" + filters[i].toString())
                  })
              : new XMLFilterImpl();
    }

    return result;
  }

  @Override
  public boolean getFeature(final String name)
      throws SAXNotRecognizedException, SAXNotSupportedException {
    return first != null ? first.getFeature(name) : super.getFeature(name);
  }

  @Override
  public XMLReader getParent() {
    return first != null ? first.getParent() : super.getParent();
  }

  @Override
  public void setParent(final XMLReader parent) {
    if (first != null) {
      first.setParent(parent);
    } else {
      super.setParent(parent);
    }
  }

  @Override
  public Object getProperty(final String name)
      throws SAXNotRecognizedException, SAXNotSupportedException {
    return first != null ? first.getProperty(name) : super.getProperty(name);
  }

  private void linkFirst() {
    if (first instanceof ContentHandler h) {
      super.setContentHandler(h);
    }

    if (first instanceof DTDHandler h) {
      super.setDTDHandler(h);
    }

    if (first instanceof EntityResolver r) {
      super.setEntityResolver(r);
    }

    if (first instanceof ErrorHandler h) {
      super.setErrorHandler(h);
    }
  }

  @Override
  public void parse(final InputSource input) throws IOException, SAXException {
    if (last != null) {
      last.parse(input);
    } else {
      super.parse(input);
    }
  }

  @Override
  public void setContentHandler(final ContentHandler handler) {
    if (last != null) {
      last.setContentHandler(handler);
    } else {
      super.setContentHandler(handler);
    }
  }

  @Override
  public void setDTDHandler(final DTDHandler handler) {
    if (last != null) {
      last.setDTDHandler(handler);
    } else {
      super.setDTDHandler(handler);
    }
  }

  @Override
  public void setEntityResolver(final EntityResolver resolver) {
    if (last != null) {
      last.setEntityResolver(resolver);
    } else {
      super.setEntityResolver(resolver);
    }
  }

  @Override
  public void setErrorHandler(final ErrorHandler handler) {
    if (last != null) {
      last.setErrorHandler(handler);
    } else {
      super.setErrorHandler(handler);
    }
  }

  @Override
  public void setFeature(final String name, final boolean value)
      throws SAXNotRecognizedException, SAXNotSupportedException {
    if (first != null) {
      first.setFeature(name, value);
    } else {
      super.setFeature(name, value);
    }
  }

  @Override
  public void setProperty(final String name, final Object value)
      throws SAXNotRecognizedException, SAXNotSupportedException {
    if (first != null) {
      first.setProperty(name, value);
    } else {
      super.setProperty(name, value);
    }
  }

  private void setupChain(final XMLFilter[] filters) {
    if (filters.length > 0) {
      for (int i = filters.length - 1; i > 0; --i) {
        connectFilters(filters[i], filters[i - 1]);
      }

      first = filters[0];
      last = filters[filters.length - 1];
      linkFirst();
    }
  }
}
