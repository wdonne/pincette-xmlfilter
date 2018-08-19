package net.pincette.xml.stream;

import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetWithRethrow;

import java.io.StringWriter;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.pincette.function.SideEffect;
import org.w3c.dom.Entity;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

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

    for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
      switch (n.getNodeType()) {
        case Node.CDATA_SECTION_NODE:
          builder.append("<![CDATA[");
          builder.append(n.getTextContent());
          builder.append("]]>");
          break;

        case Node.COMMENT_NODE:
          builder.append("<!--");
          builder.append(n.getTextContent());
          builder.append("-->");
          break;

        case Node.ELEMENT_NODE:
          builder.append(elementToText(n));
          break;

        case Node.ENTITY_NODE:
        case Node.ENTITY_REFERENCE_NODE:
          builder.append(entityToText(n));
          break;

        case Node.PROCESSING_INSTRUCTION_NODE:
          builder.append("<?");
          builder.append(((ProcessingInstruction) n).getTarget());
          builder.append(' ');
          builder.append(((ProcessingInstruction) n).getData());
          builder.append("?>");
          break;

        case Node.TEXT_NODE:
          builder.append(n.getTextContent());
          break;

        default:
          break;
      }
    }

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
