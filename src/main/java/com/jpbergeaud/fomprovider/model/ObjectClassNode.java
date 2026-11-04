package com.jpbergeaud.fomprovider.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Représente une classe d'objet (élément {@code objectClass}) de la FOM, avec ses attributs et
 * ses sous-classes éventuelles (le schéma HLA autorise l'imbrication récursive d'objectClass pour
 * représenter la hiérarchie d'héritage, par exemple HLAobjectRoot → PhysicalEntity → Platform →
 * Aircraft).
 *
 * <p>Cette classe sert à la fois de résultat de parsing pour un fichier unique et de nœud dans le
 * modèle fusionné : les fichiers successifs sont combinés dans les mêmes instances via
 * {@link com.jpbergeaud.fomprovider.parser.FomMerger}, sans conserver de trace de leur fichier
 * d'origine.</p>
 */
public class ObjectClassNode {

    private final String name;
    private String sharing = "";
    private String semantics = "";

    /** Attributs de cette classe, indexés par nom pour permettre la fusion et un accès direct. */
    private final Map<String, AttributeInfo> attributes = new LinkedHashMap<>();

    /** Sous-classes directes, indexées par nom. */
    private final Map<String, ObjectClassNode> children = new LinkedHashMap<>();

    public ObjectClassNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getSharing() {
        return sharing;
    }

    public void setSharing(String sharing) {
        this.sharing = sharing;
    }

    public String getSemantics() {
        return semantics;
    }

    public void setSemantics(String semantics) {
        this.semantics = semantics;
    }

    public Map<String, AttributeInfo> getAttributes() {
        return attributes;
    }

    public Map<String, ObjectClassNode> getChildren() {
        return children;
    }

    /** Complète les champs scalaires vides à partir d'une autre définition de la même classe. */
    public void mergeFrom(ObjectClassNode other) {
        if (sharing.isEmpty()) sharing = other.sharing;
        if (semantics.isEmpty()) semantics = other.semantics;
    }

    @Override
    public String toString() {
        return name;
    }
}
