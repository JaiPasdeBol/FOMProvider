package com.jpbergeaud.fomprovider.parser;

import com.jpbergeaud.fomprovider.model.AttributeInfo;
import com.jpbergeaud.fomprovider.model.FomModel;
import com.jpbergeaud.fomprovider.model.InteractionClassNode;
import com.jpbergeaud.fomprovider.model.ObjectClassNode;
import com.jpbergeaud.fomprovider.model.ParameterInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Lit un fichier FOM conforme au schéma HLA IEEE 1516 OMT/DIF (élément racine
 * {@code objectModel}) et produit un {@link FomModel} contenant les classes d'objets et
 * d'interactions qu'il définit.
 *
 * <p>Le fichier n'est jamais modifié : uniquement lu en mémoire. Le parsing est volontairement
 * tolérant sur l'espace de noms (il se base sur le nom local des éléments) afin d'accepter aussi
 * bien des fichiers DIF que des FOM complets (OMT/FDD) qui partagent la même structure
 * objects/objectClass/attribute et interactions/interactionClass/parameter.</p>
 */
public final class FomFileParser {

    private FomFileParser() {
    }

    public static FomModel parse(File file) throws FomParseException {
        Document document = loadDocument(file);
        Element root = document.getDocumentElement();

        if (!"objectModel".equals(localName(root))) {
            throw new FomParseException(
                    "élément racine <objectModel> attendu, trouvé <" + root.getNodeName() + ">");
        }

        FomModel model = new FomModel();

        for (Element objectsEl : childElements(root, "objects")) {
            for (Element classEl : childElements(objectsEl, "objectClass")) {
                ObjectClassNode node = parseObjectClass(classEl);
                model.getObjectClasses().put(node.getName(), node);
            }
        }

        for (Element interactionsEl : childElements(root, "interactions")) {
            for (Element classEl : childElements(interactionsEl, "interactionClass")) {
                InteractionClassNode node = parseInteractionClass(classEl);
                model.getInteractionClasses().put(node.getName(), node);
            }
        }

        return model;
    }

    private static Document loadDocument(File file) throws FomParseException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setIgnoringComments(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            // Durcissement contre les entités externes (XXE) : le fichier reste en lecture seule
            // et ne doit jamais provoquer d'accès réseau/disque en dehors de lui-même.
            trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
            trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            trySetFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(file);
        } catch (Exception e) {
            throw new FomParseException("fichier XML invalide (" + e.getMessage() + ")", e);
        }
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
            // Fonctionnalité non supportée par l'implémentation JAXP courante : on continue sans.
        }
    }

    private static ObjectClassNode parseObjectClass(Element classEl) {
        ObjectClassNode node = new ObjectClassNode(childText(classEl, "name"));
        node.setSharing(childText(classEl, "sharing"));
        node.setSemantics(childText(classEl, "semantics"));

        for (Element attrEl : childElements(classEl, "attribute")) {
            AttributeInfo attribute = parseAttribute(attrEl);
            node.getAttributes().put(attribute.getName(), attribute);
        }

        for (Element childClassEl : childElements(classEl, "objectClass")) {
            ObjectClassNode child = parseObjectClass(childClassEl);
            node.getChildren().put(child.getName(), child);
        }

        return node;
    }

    private static AttributeInfo parseAttribute(Element attrEl) {
        AttributeInfo attribute = new AttributeInfo(childText(attrEl, "name"));
        attribute.setDataType(childText(attrEl, "dataType"));
        attribute.setUpdateType(childText(attrEl, "updateType"));
        attribute.setUpdateCondition(childText(attrEl, "updateCondition"));
        attribute.setValueRequired(childText(attrEl, "valueRequired"));
        attribute.setOwnership(childText(attrEl, "ownership"));
        attribute.setSharing(childText(attrEl, "sharing"));
        attribute.setTransportation(childText(attrEl, "transportation"));
        attribute.setOrder(childText(attrEl, "order"));
        attribute.setSemantics(childText(attrEl, "semantics"));
        return attribute;
    }

    private static InteractionClassNode parseInteractionClass(Element classEl) {
        InteractionClassNode node = new InteractionClassNode(childText(classEl, "name"));
        node.setSharing(childText(classEl, "sharing"));
        node.setTransportation(childText(classEl, "transportation"));
        node.setOrder(childText(classEl, "order"));
        node.setSemantics(childText(classEl, "semantics"));

        for (Element paramEl : childElements(classEl, "parameter")) {
            ParameterInfo parameter = parseParameter(paramEl);
            node.getParameters().put(parameter.getName(), parameter);
        }

        for (Element childClassEl : childElements(classEl, "interactionClass")) {
            InteractionClassNode child = parseInteractionClass(childClassEl);
            node.getChildren().put(child.getName(), child);
        }

        return node;
    }

    private static ParameterInfo parseParameter(Element paramEl) {
        ParameterInfo parameter = new ParameterInfo(childText(paramEl, "name"));
        parameter.setDataType(childText(paramEl, "dataType"));
        parameter.setSemantics(childText(paramEl, "semantics"));
        return parameter;
    }

    /** Renvoie le contenu texte du premier enfant direct portant ce nom local, ou "" si absent. */
    private static String childText(Element parent, String localName) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE && localName.equals(localName(n))) {
                String text = n.getTextContent();
                return text == null ? "" : text.trim();
            }
        }
        return "";
    }

    /** Renvoie la liste des enfants directs portant ce nom local. */
    private static List<Element> childElements(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE && localName.equals(localName(n))) {
                result.add((Element) n);
            }
        }
        return result;
    }

    private static String localName(Node node) {
        String ln = node.getLocalName();
        return ln != null ? ln : node.getNodeName();
    }
}
