package net.pincette.xml.stream;

import static net.pincette.util.Util.isUri;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.Util.resolveSystemId;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;
import javax.xml.stream.XMLResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Delegates the operation to a SAX entity resolver.
 *
 * @author Werner Donné
 */
public class EntityResolverWrapper implements XMLResolver {
  private final EntityResolver resolver;

  public EntityResolverWrapper(final EntityResolver resolver) {
    this.resolver = resolver;
  }

  private static InputStream openEntityStream(final String systemId) {
    return Optional.ofNullable(systemId)
        .flatMap(
            id ->
                tryToGetRethrow(
                    () -> isUri(id) ? new URL(id).openStream() : new FileInputStream(id)))
        .orElse(null);
  }

  public Object resolveEntity(
      final String publicID, final String systemID, final String baseURI, final String namespace) {
    final Function<InputSource, String> justSource =
        source -> source != null ? source.getSystemId() : null;
    return tryToGetRethrow(() -> resolver.resolveEntity(publicID, systemID))
        .map(
            source ->
                openEntityStream(
                    baseURI != null && source.getSystemId() != null
                        ? resolveSystemId(baseURI, source.getSystemId())
                        : justSource.apply(source)))
        .orElse(null);
  }
}
