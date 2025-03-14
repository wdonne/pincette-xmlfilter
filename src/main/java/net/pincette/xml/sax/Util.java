package net.pincette.xml.sax;

import static java.util.stream.Collectors.joining;
import static javax.xml.parsers.SAXParserFactory.newInstance;
import static net.pincette.util.StreamUtil.rangeExclusive;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToDoSilent;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.Util.secureTransformerFactory;

import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import net.pincette.xml.CatalogResolver;
import org.xml.sax.Attributes;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

/**
 * @author Werner Donné
 */
public class Util {
  private Util() {}

  /** Adds the attribute to <code>atts</code> and returns the latter. */
  public static AttributesImpl addAttribute(
      final AttributesImpl atts,
      final String namespaceURI,
      final String localName,
      final String qName,
      final String type,
      final String value) {
    atts.addAttribute(namespaceURI, localName, qName, type, value);

    return atts;
  }

  public static Stream<Attribute> attributes(final Attributes atts) {
    return rangeExclusive(0, atts.getLength()).map(i -> new Attribute(atts, i));
  }

  public static XMLReader getParser(final URL catalog, final boolean validating) {
    return getParser(newSAXParserFactory(validating), catalog);
  }

  public static XMLReader getParser(final SAXParserFactory factory, final URL catalog) {
    return tryToGetRethrow(
            () -> {
              final XMLReader parser = factory.newSAXParser().getXMLReader();

              parser.setErrorHandler(new ErrorHandler(false));

              if (catalog != null) {
                final CatalogResolver resolver = new CatalogResolver(catalog);

                parser.setEntityResolver(resolver);
                trySchemaLocation(parser, resolver);
              }

              return parser;
            })
        .orElse(null);
  }

  public static SAXParserFactory newSAXParserFactory(final boolean validating) {
    final SAXParserFactory factory = newInstance();

    factory.setNamespaceAware(true);
    factory.setValidating(validating);
    tryFactoryProperties(factory, validating);

    return factory;
  }

  public static SAXTransformerFactory newSAXTransformerFactory() {
    return (SAXTransformerFactory) secureTransformerFactory();
  }

  @SuppressWarnings("squid:S1872") // Avoid making linking Saxon mandatory.
  public static XMLFilter newTemplatesFilter(
      final Templates templates,
      final Map<String, String> parameters,
      final SAXTransformerFactory factory)
      throws TransformerConfigurationException {
    if (!"net.sf.saxon.PreparedStylesheet".equals(templates.getClass().getName())) {
      return factory.newXMLFilter(new ParameterizableTemplate(templates, parameters));
    }

    final XMLFilter result = factory.newXMLFilter(templates);

    tryToDoRethrow(
        () ->
            setTransformerParameters(
                (Transformer) result.getClass().getMethod("getTransformer").invoke(result),
                parameters));

    return result;
  }

  public static TransformerHandler newTemplatesHandler(
      final Templates templates,
      final Map<String, String> parameters,
      final SAXTransformerFactory factory)
      throws TransformerConfigurationException {
    final TransformerHandler result = factory.newTransformerHandler(templates);

    setTransformerParameters(result.getTransformer(), parameters);

    return result;
  }

  public static Attributes reduce(final Stream<Attribute> atts) {
    return reduceImpl(atts);
  }

  public static AttributesImpl reduceImpl(final Stream<Attribute> atts) {
    return atts.reduce(
        new AttributesImpl(),
        (r, a) -> {
          r.addAttribute(a.namespaceURI, a.localName, a.qName, a.type, a.value);
          return r;
        },
        (r1, r2) -> r1);
  }

  private static void setTransformerParameters(
      final Transformer transformer, final Map<String, String> parameters) {
    parameters.forEach(transformer::setParameter);
  }

  private static void tryFactoryProperties(
      final SAXParserFactory factory, final boolean validating) {
    tryToDoSilent(
        () -> factory.setFeature("http://apache.org/xml/features/validation/schema", validating));
    tryToDoSilent(
        () ->
            factory.setFeature(
                "http://apache.org/xml/features/validation/schema-full-checking", validating));
    tryToDoSilent(() -> factory.setXIncludeAware(true));
  }

  private static void trySchemaLocation(final XMLReader parser, final CatalogResolver resolver) {
    tryToDoSilent(
        () ->
            parser.setProperty(
                "http://apache.org/xml/properties/schema/external-schemaLocation",
                resolver.getSystemIdentifierMappings().keySet().stream()
                    .map(key -> key + " " + key + " ")
                    .collect(joining())));
  }

  private record ParameterizableTemplate(Templates delegate, Map<String, String> parameters)
      implements Templates {
    public Properties getOutputProperties() {
      return delegate.getOutputProperties();
    }

    public Transformer newTransformer() throws TransformerConfigurationException {
      final Transformer transformer = delegate.newTransformer();

      setTransformerParameters(transformer, parameters);

      return transformer;
    }
  }
}
