package com.jpbergeaud.fomprovider.model;

/**
 * Représente un élément {@code attribute} d'une classe d'objet (objectClass), tel que défini par
 * le schéma HLA IEEE 1516 OMT/DIF (type {@code attributeType}).
 *
 * <p>Tous les champs sont de simples chaînes de caractères correspondant au contenu textuel des
 * éléments XML correspondants ; une chaîne vide signifie que l'élément était absent du fichier
 * source (la quasi-totalité des sous-éléments de {@code attribute} sont optionnels dans le
 * schéma).</p>
 */
public class AttributeInfo {

    private final String name;
    private String dataType = "";
    private String updateType = "";
    private String updateCondition = "";
    private String valueRequired = "";
    private String ownership = "";
    private String sharing = "";
    private String transportation = "";
    private String order = "";
    private String semantics = "";

    public AttributeInfo(String name) {
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

    public String getUpdateType() {
        return updateType;
    }

    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }

    public String getUpdateCondition() {
        return updateCondition;
    }

    public void setUpdateCondition(String updateCondition) {
        this.updateCondition = updateCondition;
    }

    public String getValueRequired() {
        return valueRequired;
    }

    public void setValueRequired(String valueRequired) {
        this.valueRequired = valueRequired;
    }

    public String getOwnership() {
        return ownership;
    }

    public void setOwnership(String ownership) {
        this.ownership = ownership;
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

    /**
     * Complète les champs actuellement vides avec les valeurs d'un autre attribut de même nom,
     * en provenance d'un autre fichier FOM. Ne remplace jamais une valeur déjà renseignée.
     */
    public void mergeFrom(AttributeInfo other) {
        if (dataType.isEmpty()) dataType = other.dataType;
        if (updateType.isEmpty()) updateType = other.updateType;
        if (updateCondition.isEmpty()) updateCondition = other.updateCondition;
        if (valueRequired.isEmpty()) valueRequired = other.valueRequired;
        if (ownership.isEmpty()) ownership = other.ownership;
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
