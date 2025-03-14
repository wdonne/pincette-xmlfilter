package net.pincette.xml.stream;

import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetWithRethrow;
import static net.pincette.xml.Util.children;
import static org.w3c.dom.Node.CDATA_SECTION_NODE;
import static org.w3c.dom.Node.COMMENT_NODE;
import static org.w3c.dom.Node.ELEMENT_NODE;
import static org.w3c.dom.Node.ENTITY_NODE;
import static org.w3c.dom.Node.ENTITY_REFERENCE_NODE;
import static org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE;
import static org.w3c.dom.Node.TEXT_NODE;

import java.io.StringWriter;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.pincette.function.SideEffect;
import org.w3c.dom.Entity;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

/**
 * @author Werner Donné
 */
public class EntityDeclarationEvent extends XMLEventBase
    implements javax.xml.stream.events.EntityDeclaration {
  private final Entity entity;

  public EntityDeclarationEvent(final Entity entity) {
    this.entity = entity;
  }

  private static String elementToText(final Node node) {
    return tryToGetWithRethrow(
            StringWriter::new,
            writer ->
                SideEffect.<StringWriter>run(
                        () ->
                            tryToDoRethrow(
                                () ->
                                    TransformerFactory.newInstance()
                                        .newTransformer()
                                        .transform(new DOMSource(node), new StreamResult(writer))))
                    .andThenGet(() -> writer))
        .map(StringWriter::toString)
        .orElse(null);
  }

  private static String entityToText(final Node node) {
    final StringBuilder builder = new StringBuilder();

    children(node)
        .forEach(
            n -> {
              switch (n.getNodeType()) {
                case CDATA_SECTION_NODE:
                  builder.append("<![CDATA[");
                  builder.append(n.getTextContent());
                  builder.append("]]>");
                  break;

                case COMMENT_NODE:
                  builder.append("<!--");
                  builder.append(n.getTextContent());
                  builder.append("-->");
                  break;

                case ELEMENT_NODE:
                  builder.append(elementToText(n));
                  break;

                case ENTITY_NODE, ENTITY_REFERENCE_NODE:
                  builder.append(entityToText(n));
                  break;

                case PROCESSING_INSTRUCTION_NODE:
                  builder.append("<?");
                  builder.append(((ProcessingInstruction) n).getTarget());
                  builder.append(' ');
                  builder.append(((ProcessingInstruction) n).getData());
                  builder.append("?>");
                  break;

                case TEXT_NODE:
                  builder.append(n.getTextContent());
                  break;

                default:
                  break;
              }
            });

    return builder.toString();
  }

  public String getBaseURI() {
    return null;
  }

  public String getName() {
    return entity.getNodeName();
  }

  public String getNotationName() {
    return entity.getNotationName();
  }

  public String getPublicId() {
    return entity.getPublicId();
  }

  public String getReplacementText() {
    return entity.hasChildNodes() ? entityToText(entity) : null;
  }

  public String getSystemId() {
    return entity.getSystemId();
  }
}
