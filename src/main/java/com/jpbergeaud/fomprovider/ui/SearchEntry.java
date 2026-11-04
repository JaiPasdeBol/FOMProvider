package com.jpbergeaud.fomprovider.ui;

/**
 * Entrée de l'index de recherche (menu "Édition → Rechercher...") : soit une classe d'objet, soit
 * une classe d'interaction, soit un attribut d'une classe d'objet.
 *
 * <p>Pour un attribut, {@link #getFullClassName()} renvoie le nom complet de la classe d'objet qui
 * le déclare (et pas un nom d'attribut) : c'est cette classe qui est sélectionnée dans
 * l'arborescence quand l'utilisateur choisit l'entrée, conformément au principe "si c'est un
 * attribut, c'est l'objet auquel il appartient qui est sélectionné". Les paramètres d'interaction
 * ne sont volontairement pas représentés ici (voir {@link SearchIndex}).</p>
 */
public final class SearchEntry {

    /** Nature de l'entrée, et libellé affiché devant chaque résultat dans la fenêtre de recherche. */
    public enum Kind {
        OBJECT_CLASS("Classe d'objet"),
        INTERACTION_CLASS("Interaction"),
        ATTRIBUTE("Attribut");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final Kind kind;
    private final String name;
    private final String fullClassName;
    private final boolean interaction;

    /**
     * @param kind          nature de l'entrée
     * @param name          nom simple sur lequel porte le filtrage (nom de la classe, ou nom de
     *                      l'attribut — jamais un nom complet avec points)
     * @param fullClassName nom complet, séparé par des points, de la classe à sélectionner dans
     *                      l'arborescence (la classe elle-même, ou la classe propriétaire s'il
     *                      s'agit d'un attribut)
     * @param interaction   {@code true} si {@code fullClassName} désigne une classe d'interaction,
     *                      {@code false} s'il désigne une classe d'objet
     */
    public SearchEntry(Kind kind, String name, String fullClassName, boolean interaction) {
        this.kind = kind;
        this.name = name;
        this.fullClassName = fullClassName;
        this.interaction = interaction;
    }

    public Kind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public String getFullClassName() {
        return fullClassName;
    }

    public boolean isInteraction() {
        return interaction;
    }

    /** Texte affiché pour cette entrée dans la liste de résultats de la fenêtre de recherche. */
    public String getDisplayText() {
        if (kind == Kind.ATTRIBUTE) {
            return name + "  —  attribut de " + fullClassName;
        }
        return fullClassName;
    }

    @Override
    public String toString() {
        return "[" + kind.getLabel() + "] " + getDisplayText();
    }
}
