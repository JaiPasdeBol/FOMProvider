package com.jpbergeaud.fomprovider.parser;

import com.jpbergeaud.fomprovider.model.Federate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lit et écrit les fichiers de fédéré conformes au schéma {@code federatesFOMProvider.xsd}
 * (élément racine {@code FederateFOMProvider}, espace de noms
 * {@code http://tempuri.org/FederateFOMProvider.xsd}) : pour chaque fédéré, ses classes d'objet
 * souscrites avec le sous-ensemble d'attributs sélectionné ({@code FOMObject}/{@code attribute}),
 * et ses classes d'interaction souscrites dans leur intégralité ({@code FOMInteraction}).
 *
 * <p>Le nom complet d'une classe (attribut {@code name} de {@code FOMObject}/{@code
 * FOMInteraction}) est le chemin depuis la racine de la hiérarchie jusqu'à la classe, séparé par
 * des points (ex. {@code HLAobjectRoot.PhysicalEntity.Aircraft}), tel que produit par
 * {@code FomTreeFactory.fullObjectName}/{@code fullInteractionName}.</p>
 */
public final class FederateFileIO {

    private static final String NAMESPACE = "http://tempuri.org/FederateFOMProvider.xsd";

    private FederateFileIO() {
    }

    public static Federate parse(File file) throws FomParseException {
        Document document = loadDocument(file);
        Element root = document.getDocumentElement();

        if (!"FederateFOMProvider".equals(localName(root))) {
            throw new FomParseException(
                    "élément racine <FederateFOMProvider> attendu, trouvé <" + root.getNodeName() + ">");
        }

        String name = root.getAttribute("FederateName");
        if (name == null || name.isBlank()) {
            name = stripExtension(file.getName());
        }

        Federate federate = new Federate(name);
        federate.setFile(file);

        for (Element objectEl : childElements(root, "FOMObject")) {
            String objectName = objectEl.getAttribute("name");
            if (objectName == null || objectName.isBlank()) {
                continue;
            }
            for (Element attributeEl : childElements(objectEl, "attribute")) {
                String attributeName = attributeEl.getTextContent();
                if (attributeName != null && !attributeName.isBlank()) {
                    federate.setAttributeSelected(objectName, attributeName.trim(), true);
                }
            }
        }

        for (Element interactionEl : childElements(root, "FOMInteraction")) {
            String interactionName = interactionEl.getAttribute("name");
            if (interactionName != null && !interactionName.isBlank()) {
                federate.setInteractionSelected(interactionName, true);
            }
        }

        return federate;
    }

    public static void write(Federate federate, File file) throws FomParseException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            Element root = document.createElementNS(NAMESPACE, "FederateFOMProvider");
            if (federate.getName() != null) {
                root.setAttribute("FederateName", federate.getName());
            }
            document.appendChild(root);

            for (Map.Entry<String, Set<String>> entry : federate.getObjectAttributesView().entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                Element objectEl = document.createElementNS(NAMESPACE, "FOMObject");
                objectEl.setAttribute("name", entry.getKey());
                for (String attributeName : entry.getValue()) {
                    Element attributeEl = document.createElementNS(NAMESPACE, "attribute");
                    attributeEl.setTextContent(attributeName);
                    objectEl.appendChild(attributeEl);
                }
                root.appendChild(objectEl);
            }

            for (String interactionName : federate.getInteractionsView()) {
                Element interactionEl = document.createElementNS(NAMESPACE, "FOMInteraction");
                interactionEl.setAttribute("name", interactionName);
                root.appendChild(interactionEl);
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(document), new StreamResult(file));
        } catch (Exception e) {
            throw new FomParseException("impossible d'enregistrer le fichier (" + e.getMessage() + ")", e);
        }
    }

    private static Document loadDocument(File file) throws FomParseException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setIgnoringComments(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
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

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
