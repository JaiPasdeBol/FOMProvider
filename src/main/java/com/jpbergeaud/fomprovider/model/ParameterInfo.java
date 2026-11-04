package com.jpbergeaud.fomprovider.model;

/**
 * Représente un élément {@code parameter} d'une classe d'interaction (interactionClass), tel que
 * défini par le schéma HLA IEEE 1516 OMT/DIF (type {@code parameterType}).
 */
public class ParameterInfo {

    private final String name;
    private String dataType = "";
    private String semantics = "";

    public ParameterInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getSemantics() {
        return semantics;
    }

    public void setSemantics(String semantics) {
        this.semantics = semantics;
    }

    /**
     * Complète les champs actuellement vides avec les valeurs d'un autre paramètre de même nom.
     */
    public void mergeFrom(ParameterInfo other) {
        if (dataType.isEmpty()) dataType = other.dataType;
        if (semantics.isEmpty()) semantics = other.semantics;
    }

    @Override
    public String toString() {
        return name;
    }
}
