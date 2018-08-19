package net.pincette.xml.stream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;



/**
 * Let you run something after the reader has been closed.
 * @author Werner Donn\u00e9
 */

public class RunAtCloseEventReader extends EventReaderDelegateBase

{

  private Runnable	run;



  public
  RunAtCloseEventReader(XMLEventReader reader, Runnable run)
  {
    super(reader);
    this.run = run;
  }



  public void
  close() throws XMLStreamException
  {
    super.close();

    if (run != null)
    {
      run.run();
    }
  }

} // RunAtCloseEventReader
