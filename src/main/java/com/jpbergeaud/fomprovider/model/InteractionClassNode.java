package com.jpbergeaud.fomprovider.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Représente une classe d'interaction (élément {@code interactionClass}) de la FOM, avec ses
 * paramètres et ses sous-classes éventuelles (hiérarchie récursive, par exemple
 * HLAinteractionRoot → WeaponFire).
 */
public class InteractionClassNode {

    private final String name;
    private String sharing = "";
    private String transportation = "";
    private String order = "";
    private String semantics = "";

    /** Paramètres de cette classe, indexés par nom. */
    private final Map<String, ParameterInfo> parameters = new LinkedHashMap<>();

    /** Sous-classes directes, indexées par nom. */
    private final Map<String, InteractionClassNode> children = new LinkedHashMap<>();

    public InteractionClassNode(String name) {
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

    public String getTransportation() {
        return transportation;
    }

    public void setTransportation(String transportation) {
        this.transportation = transportation;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getSemantics() {
        return semantics;
    }

    public void setSemantics(String semantics) {
        this.semantics = semantics;
    }

    public Map<String, ParameterInfo> getParameters() {
        return parameters;
    }

    public Map<String, InteractionClassNode> getChildren() {
        return children;
    }

    /** Complète les champs scalaires vides à partir d'une autre définition de la même classe. */
    public void mergeFrom(InteractionClassNode other) {
        if (sharing.isEmpty()) sharing = other.sharing;
        if (transportation.isEmpty()) transportation = other.transportation;
        if (order.isEmpty()) order = other.order;
        if (semantics.isEmpty()) semantics = other.semantics;
    }

    @Override
    public String toString() {
        return name;
    }
}
