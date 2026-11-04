package com.jpbergeaud.fomprovider.ui;

import com.jpbergeaud.fomprovider.model.Federate;
import com.jpbergeaud.fomprovider.model.InteractionClassNode;
import com.jpbergeaud.fomprovider.model.ObjectClassNode;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.util.List;

/**
 * Rendu personnalisé de l'arborescence :
 * <ul>
 *     <li>après le nom de chaque classe d'objet, ajoute une pastille colorée par fédéré ayant
 *     sélectionné au moins un attribut de cette classe ({@link Federate#hasAnyAttributeSelected});</li>
 *     <li>après le nom de chaque classe d'interaction, ajoute de même une pastille colorée par
 *     fédéré abonné à cette interaction ({@link Federate#isInteractionSelected});</li>
 *     <li>devant le nom de chaque fédéré, ajoute une pastille de sa propre couleur.</li>
 * </ul>
 */
public class FomTreeCellRenderer extends DefaultTreeCellRenderer {

    private final List<Federate> federates;

    public FomTreeCellRenderer(List<Federate> federates) {
        this.federates = federates;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                   boolean expanded, boolean leaf, int row,
                                                   boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        if (value instanceof DefaultMutableTreeNode node) {
            Object userObject = node.getUserObject();
            if (userObject instanceof ObjectClassNode objectClass) {
                String fullName = FomTreeFactory.fullObjectName(node);
                StringBuilder dots = new StringBuilder();
                for (Federate federate : federates) {
                    if (federate.hasAnyAttributeSelected(fullName)) {
                        dots.append(" <font color='").append(toHex(federate.getColor())).append("'>&#9679;</font>");
                    }
                }
                if (dots.length() > 0) {
                    setText("<html>" + escape(objectClass.getName()) + dots + "</html>");
                }
            } else if (userObject instanceof InteractionClassNode interactionClass) {
                String fullName = FomTreeFactory.fullInteractionName(node);
                StringBuilder dots = new StringBuilder();
                for (Federate federate : federates) {
                    if (federate.isInteractionSelected(fullName)) {
                        dots.append(" <font color='").append(toHex(federate.getColor())).append("'>&#9679;</font>");
                    }
                }
                if (dots.length() > 0) {
                    setText("<html>" + escape(interactionClass.getName()) + dots + "</html>");
                }
            } else if (userObject instanceof Federate federate) {
                setText("<html><font color='" + toHex(federate.getColor()) + "'>&#9679;</font> "
                        + escape(federate.getName()) + "</html>");
            }
        }
        return this;
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
