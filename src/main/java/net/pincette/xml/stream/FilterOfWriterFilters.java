package net.pincette.xml.stream;

import net.pincette.xml.sax.BalanceChecker;
import java.io.File;
import java.io.FileOutputStream;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import javax.xml.transform.sax.TransformerHandler;
import org.xml.sax.helpers.XMLFilterImpl;



/**
 * Connects an array of filters and encapsulates them.
 * @author Werner Donn\u00e9
 */

public class FilterOfWriterFilters extends EventWriterDelegate

{

  private XMLEventWriter	head;
  private Object		tail;



  public
  FilterOfWriterFilters(Object[] filters)
  {
    this(filters, null, false);
  }



  public
  FilterOfWriterFilters(Object[] filters, boolean debug)
  {
    this(filters, null, debug);
  }



  public
  FilterOfWriterFilters(Object[] filters, XMLEventWriter writer)
  {
    this(filters, writer, false);
  }



  /**
   * The <code>filters</code> argument allows
   * <code>EventWriterDelegate</code>,
   * <code>javax.xml.stream.EventReaderDelegate</code>,
   * <code>javax.xml.stream.StreamWriterDelegate</code>,
   * <code>javax.xml.transform.sax.TransformerHandler</code> and
   * <code>org.xml.sax.helpers.XMLFilterImpl</code>.
   */

  public
  FilterOfWriterFilters(Object[] filters, XMLEventWriter writer, boolean debug)
  {
    if (filters.length > 0)
    {
      if (debug)
      {
        filters = addDebug(filters);
      }

      Object[]	copy = new Object[filters.length];

      for (int i = 0; i < filters.length; ++i)
      {
        verify(filters[i]);

        copy[i] =
          filters[i] instanceof XMLFilterImpl ?
            new XMLFilterImplEventWriterDelegate((XMLFilterImpl) filters[i]) :
            (
              filters[i] instanceof TransformerHandler ?
                new TransformerHandlerEventWriterDelegate
                (
                  (TransformerHandler) filters[i]
                ) :
                (
                  filters[i] instanceof EventReaderDelegate ?
                    new EventReaderEventWriterDelegate
                    (
                      (EventReaderDelegate) filters[i]
                    ) : filters[i]
                )
            );
      }

      connectFilters(copy);
      tail = copy[copy.length - 1];
      setParent(writer);

      head =
        copy[0] instanceof StreamWriterDelegate ?
          (XMLEventWriter) new StreamEventWriter((XMLStreamWriter) copy[0]) :
          (XMLEventWriter) copy[0];
    }
    else
    {
      super.setParent(writer);
      head = writer;
    }
  }



  public void
  add(XMLEvent event) throws XMLStreamException
  {
    head.add(event);
  }



  private Object[]
  addDebug(Object[] filters)
  {
    try
    {
      XMLOutputFactory	factory = XMLOutputFactory.newInstance();
      Object[]		result = new Object[filters.length * 2 + 1];

      result[0] =
        new Tee
        (
          new XMLEventWriter[]
          {
            new FlushEventWriter
            (
              factory.createXMLEventWriter
              (
                new FileOutputStream(getDebugFile(toString() + "_input.xml"))
              )
            )
          }
        );

      for (int i = 0; i < filters.length; ++i)
      {
        result[i * 2 + 1] = filters[i];
        result[i * 2 + 2] =
          new Tee
          (
            new XMLEventWriter[]
            {
              new StreamEventWriter
              (
                new ContentHandlerStreamWriter
                (
                  new BalanceChecker
                  (
                    getDebugFile
                    (
                      toString() + "_" + filters[i].toString() + ".balance"
                    )
                  )
                )
              ),
              new FlushEventWriter
              (
                factory.createXMLEventWriter
                (
                  new FileOutputStream
                  (
                    getDebugFile(toString() + "_" + filters[i].toString())
                  )
                )
              )
            }
          );
      }

      return result;
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public void
  close() throws XMLStreamException
  {
    head.close();
  }



  private static void
  connectFilters(Object[] filters)
  {
    for (int i = 0; i < filters.length - 1; ++i)
    {
      if
      (
        filters[i] instanceof EventWriterDelegate	&&
        filters[i + 1] instanceof StreamWriterDelegate
      )
      {
        ((EventWriterDelegate) filters[i]).
          setParent(new StreamEventWriter((XMLStreamWriter) filters[i + 1]));
      }
      else
      {
        if
        (
          filters[i] instanceof StreamWriterDelegate	&&
          filters[i + 1] instanceof EventWriterDelegate
        )
        {
          ((StreamWriterDelegate) filters[i]).
            setParent(new EventStreamWriter((XMLEventWriter) filters[i + 1]));
        }
        else
        {
          if
          (
            filters[i] instanceof EventWriterDelegate		&&
            filters[i + 1] instanceof EventWriterDelegate
          )
          {
            ((EventWriterDelegate) filters[i]).
              setParent((XMLEventWriter) filters[i + 1]);
          }
          else
          {
            ((StreamWriterDelegate) filters[i]).
              setParent((XMLStreamWriter) filters[i + 1]);
          }
        }
      }
    }
  }



  public void
  flush() throws XMLStreamException
  {
    head.flush();
  }



  private static File
  getDebugFile(String filename)
  {
    return new File(new File(System.getProperty("java.io.tmpdir")), filename);
  }



  public String
  getPrefix(String uri) throws XMLStreamException
  {
    return head.getPrefix(uri);
  }



  public void
  setDefaultNamespace(String uri) throws XMLStreamException
  {
    head.setDefaultNamespace(uri);
  }



  public void
  setNamespaceContext(NamespaceContext context) throws XMLStreamException
  {
    head.setNamespaceContext(context);
  }



  public void
  setParent(XMLEventWriter writer)
  {
    super.setParent(writer);

    if (tail != null)
    {
      if (tail instanceof EventWriterDelegate)
      {
        ((EventWriterDelegate) tail).setParent(writer);
      }
      else
      {
        ((StreamWriterDelegate) tail).
          setParent(writer != null ? new EventStreamWriter(writer) : null);
      }
    }
    else
    {
      head = writer;
    }
  }



  public void
  setPrefix(String prefix, String uri) throws XMLStreamException
  {
    head.setPrefix(prefix, uri);
  }



  private static void
  verify(Object filter)
  {
    if
    (
      !(filter instanceof EventWriterDelegate)	&&
      !(filter instanceof EventReaderDelegate)	&&
      !(filter instanceof StreamWriterDelegate)	&&
      !(filter instanceof XMLFilterImpl)	&&
      !(filter instanceof TransformerHandler)
    )
    {
      throw new IllegalArgumentException(filter.getClass().toString());
    }
  }

} // FilterOfWriterFilters
