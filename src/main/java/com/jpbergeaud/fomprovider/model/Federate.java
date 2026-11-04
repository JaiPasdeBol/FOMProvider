package com.jpbergeaud.fomprovider.model;

import java.awt.Color;
import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Représente un fédéré tel que défini par un fichier conforme au schéma
 * {@code federatesFOMProvider.xsd} : un nom, et pour chaque classe d'objet à laquelle il
 * souscrit, le sous-ensemble de ses attributs auxquels il a accès (élément {@code FOMObject}),
 * ainsi que l'ensemble des classes d'interaction auxquelles il souscrit dans leur intégralité
 * (élément {@code FOMInteraction} — la norme HLA ne permet pas de souscrire à une interaction
 * attribut par attribut, contrairement aux classes d'objet).
 *
 * <p>La couleur associée au fédéré n'existe que pour l'affichage dans l'IHM (en-têtes de colonne,
 * pastilles dans l'arborescence) : elle n'est pas définie par le schéma et n'est donc jamais
 * écrite dans le fichier XML.</p>
 */
public class Federate {

    private String name;
    private Color color = Color.GRAY;
    private File file;

    /** Attributs sélectionnés par classe d'objet, indexés par nom complet (ex. "HLAobjectRoot.
     *  PhysicalEntity.Aircraft"). Une classe sans attribut sélectionné n'a pas d'entrée ici. */
    private final Map<String, Set<String>> objectAttributes = new LinkedHashMap<>();

    /** Classes d'interaction auxquelles ce fédéré souscrit, indexées par nom complet. */
    private final Set<String> interactions = new LinkedHashSet<>();

    public Federate(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public boolean isAttributeSelected(String fullObjectName, String attributeName) {
        Set<String> attributes = objectAttributes.get(fullObjectName);
        return attributes != null && attributes.contains(attributeName);
    }

    public void setAttributeSelected(String fullObjectName, String attributeName, boolean selected) {
        if (selected) {
            objectAttributes.computeIfAbsent(fullObjectName, k -> new LinkedHashSet<>()).add(attributeName);
        } else {
            Set<String> attributes = objectAttributes.get(fullObjectName);
            if (attributes != null) {
                attributes.remove(attributeName);
                if (attributes.isEmpty()) {
                    objectAttributes.remove(fullObjectName);
                }
            }
        }
    }

    /** Vrai si ce fédéré a sélectionné au moins un attribut de la classe donnée. */
    public boolean hasAnyAttributeSelected(String fullObjectName) {
        Set<String> attributes = objectAttributes.get(fullObjectName);
        return attributes != null && !attributes.isEmpty();
    }

    public boolean isInteractionSelected(String fullInteractionName) {
        return interactions.contains(fullInteractionName);
    }

    public void setInteractionSelected(String fullInteractionName, boolean selected) {
        if (selected) {
            interactions.add(fullInteractionName);
        } else {
            interactions.remove(fullInteractionName);
        }
    }

    /** Vue en lecture seule des sélections d'attributs, pour l'écriture du fichier XML. */
    public Map<String, Set<String>> getObjectAttributesView() {
        return Collections.unmodifiableMap(objectAttributes);
    }

    /** Vue en lecture seule des interactions souscrites, pour l'écriture du fichier XML. */
    public Set<String> getInteractionsView() {
        return Collections.unmodifiableSet(interactions);
    }

    @Override
    public String toString() {
        return name;
    }
}
