package com.jpbergeaud.fomprovider;

import com.jpbergeaud.fomprovider.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Point d'entrée de l'application FOM Provider.
 *
 * <p>FOM Provider est une IHM Java Swing permettant de charger un ou plusieurs fichiers FOM
 * (Federation Object Model, format IEEE 1516 OMT / DIF), de fusionner leurs classes d'objets et
 * d'interactions dans une arborescence unique, et de consulter le détail (attributs, paramètres,
 * types, etc.) de chaque classe sélectionnée. Aucun fichier source n'est modifié : l'application
 * ne fait que lire et fusionner en mémoire.</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        // Look and feel du système d'exploitation, sans bloquer le lancement en cas d'échec.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Look and feel par défaut de Swing en secours.
        }

        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
