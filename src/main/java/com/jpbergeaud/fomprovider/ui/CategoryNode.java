package com.jpbergeaud.fomprovider.ui;

/**
 * Nœud de catégorie purement visuel utilisé comme racine des sous-arbres "Objets" et
 * "Interactions" dans le {@link javax.swing.JTree}. Ne correspond à aucun élément du schéma FOM.
 */
public final class CategoryNode {

    private final String label;
    private final int count;

    public CategoryNode(String label, int count) {
        this.label = label;
        this.count = count;
    }

    @Override
    public String toString() {
        return label + " (" + count + ")";
    }
}
