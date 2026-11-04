package com.jpbergeaud.fomprovider.ui;

import com.jpbergeaud.fomprovider.model.AttributeInfo;
import com.jpbergeaud.fomprovider.model.FomModel;
import com.jpbergeaud.fomprovider.model.InteractionClassNode;
import com.jpbergeaud.fomprovider.model.ObjectClassNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit, à partir du {@link FomModel} fusionné, la liste exhaustive des entrées que la
 * fenêtre de recherche (menu "Édition → Rechercher...") peut proposer : chaque classe d'objet,
 * chaque classe d'interaction (hiérarchie d'héritage comprise), et chaque attribut de chaque
 * classe d'objet.
 *
 * <p>Les paramètres des interactions ne sont volontairement pas indexés : la norme HLA n'autorise
 * de s'abonner qu'à une interaction dans son intégralité (voir le panneau de détail des
 * interactions, {@link DetailPanel}), si bien qu'un paramètre isolé n'aurait aucune cible de
 * sélection pertinente dans l'arborescence.</p>
 *
 * <p>L'index est reconstruit à chaque ouverture de la fenêtre de recherche plutôt que maintenu à
 * jour en continu, pour rester trivialement cohérent avec le modèle même après un chargement ou
 * une réinitialisation ; le modèle ne contenant typiquement que quelques milliers d'éléments au
 * plus, ce coût est négligeable face à une saisie utilisateur.</p>
 */
public final class SearchIndex {

    private SearchIndex() {
    }

    public static List<SearchEntry> build(FomModel model) {
        List<SearchEntry> entries = new ArrayList<>();
        for (ObjectClassNode root : model.getObjectClasses().values()) {
            collectObjectEntries(root, "", entries);
        }
        for (InteractionClassNode root : model.getInteractionClasses().values()) {
            collectInteractionEntries(root, "", entries);
        }
        return entries;
    }

    private static void collectObjectEntries(ObjectClassNode node, String parentPath, List<SearchEntry> entries) {
        String fullName = parentPath.isEmpty() ? node.getName() : parentPath + "." + node.getName();
        entries.add(new SearchEntry(SearchEntry.Kind.OBJECT_CLASS, node.getName(), fullName, false));
        for (AttributeInfo attribute : node.getAttributes().values()) {
            entries.add(new SearchEntry(SearchEntry.Kind.ATTRIBUTE, attribute.getName(), fullName, false));
        }
        for (ObjectClassNode child : node.getChildren().values()) {
            collectObjectEntries(child, fullName, entries);
        }
    }

    private static void collectInteractionEntries(InteractionClassNode node, String parentPath,
                                                    List<SearchEntry> entries) {
        String fullName = parentPath.isEmpty() ? node.getName() : parentPath + "." + node.getName();
        entries.add(new SearchEntry(SearchEntry.Kind.INTERACTION_CLASS, node.getName(), fullName, true));
        for (InteractionClassNode child : node.getChildren().values()) {
            collectInteractionEntries(child, fullName, entries);
        }
    }
}
