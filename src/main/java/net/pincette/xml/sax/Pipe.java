package net.pincette.xml.sax;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;



/**
 * This class constructs a SAX pipe that can be used in transformation chains.
 * Use <code>getPipeContentHandler</code> to obtain a content handler which
 * can be passed to a SAX source.
 * @author Werner Donn\u00e9
 */

public class Pipe implements XMLReader

{

  private final static int	CHARACTERS = 0;
  private final static int	END_DOCUMENT = 1;
  private final static int	END_ELEMENT = 2;
  private final static int	END_PREFIX_MAPPING = 3;
  private final static int	IGNORABLE_WHITESPACE = 4;
  private final static int	PROCESSING_INSTRUCTION = 5;
  private final static int	SET_DOCUMENT_LOCATOR = 6;
  private final static int	SKIPPED_ENTITY = 7;
  private final static int	START_DOCUMENT = 8;
  private final static int	START_ELEMENT = 9;
  private final static int	START_PREFIX_MAPPING = 10;

  private List			buffer = new LinkedList();
  private int			bufferSize;
  private ContentHandler	contentHandler;
  private Set			features = new HashSet();
  private Method[]		methods;
  private PipeContentHandler	pipeContentHandler = new PipeContentHandler();
  private Map			properties = new Hashtable();



  public
  Pipe()
  {
    this(1000);
  }



  /**
   * If <code>bufferSize</code> is -1, the buffer is infinite, implying the
   * pipe can work in a single thread without blocking.
   */

  public
  Pipe(int bufferSize)
  {
    if (bufferSize < -1)
    {
      throw new IllegalArgumentException(String.valueOf(bufferSize) + " < -1");
    }

    this.bufferSize = bufferSize;
    features.add("http://xml.org/sax/features/namespaces");
    features.add("http://xml.org/sax/features/namespace-prefixes");
  }



  public ContentHandler
  getContentHandler()
  {
    return contentHandler;
  }



  public DTDHandler
  getDTDHandler()
  {
    return null;
  }



  public EntityResolver
  getEntityResolver()
  {
    return null;
  }



  public ErrorHandler
  getErrorHandler()
  {
    return null;
  }



  public boolean
  getFeature(String name)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
    return features.contains(name);
  }



  /**
   * This content handler can be passed to a SAX source.
   */

  public ContentHandler
  getPipeContentHandler()
  {
    return pipeContentHandler;
  }



  public Object
  getProperty(String name)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
    Object	result = properties.get(name);

    if (result == null)
    {
      throw new SAXNotRecognizedException(name);
    }

    return result;
  }



  public void
  parse(InputSource input) throws IOException, SAXException
  {
    parse();
  }



  public void
  parse(String systemId) throws IOException, SAXException
  {
    parse();
  }



  private void
  parse() throws SAXException
  {
    if (contentHandler == null)
    {
      return;
    }

    boolean	end = false;

    try
    {
      while (!end)
      {
        Entry	entry;

        synchronized (buffer)
        {
          if (buffer.isEmpty())
          {
            buffer.notifyAll();
            buffer.wait();
          }

          entry = (Entry) buffer.remove(0);
        }

        methods[entry.method].invoke(getContentHandler(), entry.parameters);

        if (entry.method == END_DOCUMENT)
        {
          end = true;
        }
      }
    }

    catch (Exception e)
    {
      throw new SAXException(e);
    }
  }



  public void
  setContentHandler(ContentHandler handler)
  {
    contentHandler = handler;

    try
    {
      methods =
        new Method[]
        {
          handler.getClass().getMethod
          (
            "characters",
            new Class[] {char[].class, int.class, int.class}
          ),
          handler.getClass().getMethod("endDocument", new Class[0]),
          handler.getClass().getMethod
          (
            "endElement",
            new Class[] {String.class, String.class, String.class}
          ),
          handler.getClass().
            getMethod("endPrefixMapping", new Class[] {String.class}),
          handler.getClass().getMethod
          (
            "ignorableWhitespace",
            new Class[] {char[].class, int.class, int.class}
          ),
          handler.getClass().getMethod
          (
            "processingInstruction",
            new Class[] {String.class, String.class}
          ),
          handler.getClass().
            getMethod("setDocumentLocator", new Class[] {Locator.class}),
          handler.getClass().
            getMethod("skippedEntity", new Class[] {String.class}),
          handler.getClass().getMethod("startDocument", new Class[0]),
          handler.getClass().getMethod
          (
            "startElement",
            new Class[]
              {String.class, String.class, String.class, Attributes.class}
          ),
          handler.getClass().getMethod
          (
            "startPrefixMapping",
            new Class[] {String.class, String.class}
          )
        };
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  /**
   * This method does nothing.
   */

  public void
  setDTDHandler(DTDHandler handler)
  {
  }



  /**
   * This method does nothing.
   */

  public void
  setEntityResolver(EntityResolver resolver)
  {
  }



  /**
   * This method does nothing.
   */

  public void
  setErrorHandler(ErrorHandler handler)
  {
  }



  public void
  setFeature(String name, boolean value)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
    if (value)
    {
      features.add(name);
    }
  }



  public void
  setProperty(String name, Object value)
    throws SAXNotRecognizedException, SAXNotSupportedException
  {
    properties.put(name, value);
  }



  private class Entry

  {

    private int		method;
    private Object[]	parameters;



    private
    Entry(int method, Object[] parameters)
    {
      this.method = method;
      this.parameters = parameters;
    }

  } // Entry



  private class PipeContentHandler implements ContentHandler

  {

    public void
    characters(char[] ch, int start, int length) throws SAXException
    {
      char[]	copy = new char[length];

      System.arraycopy(ch, start, copy, 0, length);

      synchronized (buffer)
      {
        buffer.add
        (
          new Entry
          (
            CHARACTERS,
            new Object[] {copy, new Integer(start), new Integer(length)}
          )
        );

        release();
      }
    }



    public void
    endDocument() throws SAXException
    {
      synchronized (buffer)
      {
        buffer.add(new Entry(END_DOCUMENT, new Object[0]));
        buffer.notifyAll();
      }
    }



    public void
    endElement(String namespaceURI, String localName, String qName)
      throws SAXException
    {
      synchronized (buffer)
      {
        buffer.add
        (
          new Entry(END_ELEMENT, new Object[] {namespaceURI, localName, qName})
        );

        release();
      }
    }



    public void
    endPrefixMapping(String prefix) throws SAXException
    {
      synchronized (buffer)
      {
        buffer.add(new Entry(END_PREFIX_MAPPING, new Object[] {prefix}));
        release();
      }
    }



    public void
    ignorableWhitespace(char[] ch, int start, int length) throws SAXException
    {
      char[]	copy = new char[length];

      System.arraycopy(ch, start, copy, 0, length);

      synchronized (buffer)
      {
        buffer.add
        (
          new Entry
          (
            IGNORABLE_WHITESPACE,
            new Object[] {copy, new Integer(start), new Integer(length)}
          )
        );

        release();
      }
    }



    public void
    processingInstruction(String target, String data) throws SAXException
    {
      synchronized (buffer)
      {
        buffer.
          add(new Entry(PROCESSING_INSTRUCTION, new Object[] {target, data}));
        release();
      }
    }



    private void
    release()
    {
      if (bufferSize != -1 && buffer.size() >= bufferSize)
      {
        buffer.notifyAll();

        try
        {
          buffer.wait();
        }

        catch (InterruptedException e)
        {
          throw new RuntimeException(e);
        }
      }
    }



    public void
    setDocumentLocator(Locator locator)
    {
      synchronized (buffer)
      {
        buffer.add
        (
          new Entry
          (
            SET_DOCUMENT_LOCATOR,
            new Object[] {new LocatorImpl(locator)}
          )
        );

        release();
      }
    }



    public void
    skippedEntity(String name) throws SAXException
    {
      synchronized (buffer)
      {
        buffer.add(new Entry(SKIPPED_ENTITY, new Object[] {name}));
        release();
      }
    }



    public void
    startDocument() throws SAXException
    {
      synchronized (buffer)
      {
        buffer.add(new Entry(START_DOCUMENT, new Object[0]));
        release();
      }
    }



    public void
    startElement
    (
      String		namespaceURI,
      String		localName,
      String		qName,
      Attributes	atts
    ) throws SAXException
    {
      synchronized (buffer)
      {
        buffer.add
        (
          new Entry
          (
            START_ELEMENT,
            new Object[]
              {namespaceURI, localName, qName, new AttributesImpl(atts)}
          )
        );

        release();
      }
    }



    public void
    startPrefixMapping(String prefix, String uri) throws SAXException
    {
      synchronized (buffer)
      {
        buffer.add(new Entry(START_PREFIX_MAPPING, new Object[] {prefix, uri}));
        release();
      }
    }

  } // PipeContentHandler

} // Pipe
