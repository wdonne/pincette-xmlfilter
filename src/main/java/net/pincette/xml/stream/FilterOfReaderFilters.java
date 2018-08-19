package net.pincette.xml.stream;

import net.pincette.xml.sax.BalanceChecker;
import java.io.File;
import java.io.FileOutputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.transform.sax.TransformerHandler;
import org.xml.sax.XMLFilter;



/**
 * Connects an array of filters and encapsulates them.
 * @author Werner Donn\u00e9
 */

public class FilterOfReaderFilters extends EventReaderDelegate

{

  private Object		head;
  private XMLEventReader	tail;



  public
  FilterOfReaderFilters(Object[] filters)
  {
    this(filters, null, false);
  }



  public
  FilterOfReaderFilters(Object[] filters, boolean debug)
  {
    this(filters, null, debug);
  }



  public
  FilterOfReaderFilters(Object[] filters, XMLEventReader reader)
  {
    this(filters, reader, false);
  }



  /**
   * The <code>filters</code> argument allows
   * <code>javax.xml.stream.EventReaderDelegate</code>,
   * <code>javax.xml.stream.StreamReaderDelegate</code>,
   * <code>EventWriterDelegate</code>,
   * <code>javax.xml.transform.sax.TransformerHandler</code> and
   * <code>org.xml.sax.XMLFilter</code>.
   */

  public
  FilterOfReaderFilters(Object[] filters, XMLEventReader reader, boolean debug)
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
          filters[i] instanceof XMLFilter ?
            new XMLFilterEventReaderDelegate((XMLFilter) filters[i]) :
            (
              filters[i] instanceof TransformerHandler ?
                new TransformerHandlerEventReaderDelegate
                (
                  (TransformerHandler) filters[i]
                ) :
                (
                  filters[i] instanceof EventWriterDelegate ?
                    new EventWriterEventReaderDelegate
                    (
                      (EventWriterDelegate) filters[i]
                    ) : filters[i]
                )
            );
      }

      connectFilters(copy);
      head = copy[0];
      setParent(reader);

      tail =
        copy[copy.length - 1] instanceof StreamReaderDelegate ?
          (XMLEventReader)
            new StreamEventReader((XMLStreamReader) copy[copy.length - 1]) :
          (XMLEventReader) copy[copy.length - 1];
    }
    else
    {
      super.setParent(reader);
      tail = reader;
    }
  }



  private Object[]
  addDebug(Object[] filters)
  {
    try
    {
      XMLOutputFactory	factory = XMLOutputFactory.newInstance();
      Object[]		result = new Object[filters.length * 2 + 1];

      result[0] =
        new EventWriterEventReaderDelegate
        (
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
          )
        );

      for (int i = 0; i < filters.length; ++i)
      {
        result[i * 2 + 1] = filters[i];
        result[i * 2 + 2] =
          new EventWriterEventReaderDelegate
          (
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
            )
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
    tail.close();
  }



  private static void
  connectFilters(Object[] filters)
  {
    for (int i = 0; i < filters.length - 1; ++i)
    {
      if
      (
        filters[i] instanceof StreamReaderDelegate	&&
        filters[i + 1] instanceof EventReaderDelegate
      )
      {
        ((EventReaderDelegate) filters[i + 1]).
          setParent(new StreamEventReader((XMLStreamReader) filters[i]));
      }
      else
      {
        if
        (
          filters[i] instanceof EventReaderDelegate	&&
          filters[i + 1] instanceof StreamReaderDelegate
        )
        {
          ((StreamReaderDelegate) filters[i + 1]).
            setParent(new EventStreamReader((XMLEventReader) filters[i]));
        }
        else
        {
          if
          (
            filters[i] instanceof EventReaderDelegate	&&
            filters[i + 1] instanceof EventReaderDelegate
          )
          {
            ((EventReaderDelegate) filters[i + 1]).
              setParent((XMLEventReader) filters[i]);
          }
          else
          {
            ((StreamReaderDelegate) filters[i + 1]).
              setParent((XMLStreamReader) filters[i]);
          }
        }
      }
    }
  }



  private static File
  getDebugFile(String filename)
  {
    return new File(new File(System.getProperty("java.io.tmpdir")), filename);
  }



  public String
  getElementText() throws XMLStreamException
  {
    return tail.getElementText();
  }



  public Object
  getProperty(String name)
  {
    return tail.getProperty(name);
  }



  public boolean
  hasNext()
  {
    return tail.hasNext();
  }



  public XMLEvent
  nextEvent() throws XMLStreamException
  {
    return tail.nextEvent();
  }



  public XMLEvent
  nextTag() throws XMLStreamException
  {
    return tail.nextTag();
  }



  public XMLEvent
  peek() throws XMLStreamException
  {
    return tail.peek();
  }



  public void
  setParent(XMLEventReader reader)
  {
    super.setParent(reader);

    if (head != null)
    {
      if (head instanceof EventReaderDelegate)
      {
        ((EventReaderDelegate) head).setParent(reader);
      }
      else
      {
        ((StreamReaderDelegate) head).
          setParent(reader != null ? new EventStreamReader(reader) : null);
      }
    }
    else
    {
      tail = reader;
    }
  }



  private static void
  verify(Object filter)
  {
    if
    (
      !(filter instanceof EventReaderDelegate)	&&
      !(filter instanceof EventWriterDelegate)	&&
      !(filter instanceof StreamReaderDelegate)	&&
      !(filter instanceof XMLFilter)		&&
      !(filter instanceof TransformerHandler)
    )
    {
      throw new IllegalArgumentException(filter.getClass().toString());
    }
  }

} // FilterOfReaderFilters
