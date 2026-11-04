package com.jpbergeaud.fomprovider.ui;

import com.jpbergeaud.fomprovider.model.Federate;
import com.jpbergeaud.fomprovider.model.FomModel;
import com.jpbergeaud.fomprovider.model.InteractionClassNode;
import com.jpbergeaud.fomprovider.model.ObjectClassNode;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Construit le {@link DefaultTreeModel} du {@link javax.swing.JTree} affiché dans le panneau de
 * gauche à partir du {@link FomModel} fusionné et de la liste des fédérés chargés/créés dans
 * l'IHM. L'arborescence comporte trois sous-arbres : "Fédérés", "Objets" et "Interactions" (avec
 * leur hiérarchie d'héritage) ; les attributs et paramètres ne sont pas représentés comme des
 * nœuds mais consultés dans le panneau de détail.
 */
public final class FomTreeFactory {

    private FomTreeFactory() {
    }

    public static DefaultTreeModel buildTreeModel(FomModel model, List<Federate> federates) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("FOM");

        DefaultMutableTreeNode federatesRoot =
                new DefaultMutableTreeNode(new CategoryNode("Fédérés", federates.size()));
        for (Federate federate : federates) {
            federatesRoot.add(new DefaultMutableTreeNode(federate));
        }

        DefaultMutableTreeNode objectsRoot =
                new DefaultMutableTreeNode(new CategoryNode("Objets", model.getObjectClasses().size()));
        for (ObjectClassNode objectClass : model.getObjectClasses().values()) {
            objectsRoot.add(buildObjectClassNode(objectClass));
        }

        DefaultMutableTreeNode interactionsRoot =
                new DefaultMutableTreeNode(new CategoryNode("Interactions", model.getInteractionClasses().size()));
        for (InteractionClassNode interactionClass : model.getInteractionClasses().values()) {
            interactionsRoot.add(buildInteractionClassNode(interactionClass));
        }

        root.add(federatesRoot);
        root.add(objectsRoot);
        root.add(interactionsRoot);
        return new DefaultTreeModel(root);
    }

    private static DefaultMutableTreeNode buildObjectClassNode(ObjectClassNode objectClass) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(objectClass);
        for (ObjectClassNode child : objectClass.getChildren().values()) {
            node.add(buildObjectClassNode(child));
        }
        return node;
    }

    private static DefaultMutableTreeNode buildInteractionClassNode(InteractionClassNode interactionClass) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(interactionClass);
        for (InteractionClassNode child : interactionClass.getChildren().values()) {
            node.add(buildInteractionClassNode(child));
        }
        return node;
    }

    /**
     * Nom complet d'une classe d'objet, chemin depuis la racine de la hiérarchie jusqu'au nœud
     * donné, séparé par des points (ex. "HLAobjectRoot.PhysicalEntity.Aircraft"). Utilisé aussi
     * bien pour l'affichage des pastilles de fédéré dans l'arborescence que comme clé de
     * sélection dans {@link Federate} et comme attribut "name" écrit dans les fichiers de
     * fédéré.
     */
    public static String fullObjectName(TreeNode node) {
        List<String> parts = new ArrayList<>();
        TreeNode current = node;
        while (current instanceof DefaultMutableTreeNode dmtn
                && dmtn.getUserObject() instanceof ObjectClassNode objectClass) {
            parts.add(0, objectClass.getName());
            current = dmtn.getParent();
        }
        return String.join(".", parts);
    }

    /** Équivalent de {@link #fullObjectName} pour les classes d'interaction. */
    public static String fullInteractionName(TreeNode node) {
        List<String> parts = new ArrayList<>();
        TreeNode current = node;
        while (current instanceof DefaultMutableTreeNode dmtn
                && dmtn.getUserObject() instanceof InteractionClassNode interactionClass) {
            parts.add(0, interactionClass.getName());
            current = dmtn.getParent();
        }
        return String.join(".", parts);
    }
}
