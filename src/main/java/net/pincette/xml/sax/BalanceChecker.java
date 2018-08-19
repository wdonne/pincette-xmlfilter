package net.pincette.xml.sax;

import static net.pincette.util.Util.tryToGetRethrow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import javax.xml.namespace.QName;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

public class BalanceChecker extends XMLFilterImpl {
  private final Deque<QName> elements = new ArrayDeque<>();
  private final File file;
  private int indent = 0;
  private PrintStream out;

  public BalanceChecker(final File file) {
    this.file = file;
  }

  public BalanceChecker(final File file, final XMLReader parent) {
    super(parent);
    this.file = file;
  }

  @Override
  public void endDocument() throws SAXException {
    while (!elements.isEmpty()) {
      write("Element " + elements.pop().toString() + " is not closed.");
    }

    out.close();
    super.endDocument();
  }

  @Override
  public void endElement(final String namespaceURI, final String localName, final String qName)
      throws SAXException {
    final QName element = new QName(namespaceURI, localName);

    if (elements.isEmpty()) {
      write("Closing " + element.toString() + " while no open elements are left.");
    } else {
      final QName name = elements.pop();

      if (!name.equals(element)) {
        write("Closing " + element.toString() + " while expecting " + name.toString() + ".");
      }
    }

    indent -= 2;
    write("</" + element.toString() + ">");
    super.endElement(namespaceURI, localName, qName);
  }

  private void openWriter() {
    if (out == null) {
        out = tryToGetRethrow(() -> new PrintStream(new FileOutputStream(file))).orElse(null);
    }
  }

  @Override
  public void startDocument() throws SAXException {
    openWriter();
    super.startDocument();
  }

  @Override
  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    openWriter();

    final QName name = new QName(namespaceURI, localName);

    write("<" + name.toString() + ">");
    indent += 2;
    elements.push(name);
    super.startElement(namespaceURI, localName, qName, atts);
  }

  private void write(final String s) {
    if (indent > 0) {
      final char[] c = new char[indent];

      Arrays.fill(c, ' ');
      out.print(c);
    }

    out.println(s);
    out.flush();
  }
}
