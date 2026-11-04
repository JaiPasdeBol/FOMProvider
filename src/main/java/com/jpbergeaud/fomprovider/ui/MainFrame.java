package com.jpbergeaud.fomprovider.ui;

import com.jpbergeaud.fomprovider.model.Federate;
import com.jpbergeaud.fomprovider.model.FomModel;
import com.jpbergeaud.fomprovider.model.InteractionClassNode;
import com.jpbergeaud.fomprovider.model.ObjectClassNode;
import com.jpbergeaud.fomprovider.parser.FederateFileIO;
import com.jpbergeaud.fomprovider.parser.FomFileParser;
import com.jpbergeaud.fomprovider.parser.FomMerger;
import com.jpbergeaud.fomprovider.parser.FomParseException;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Fenêtre principale de FOM Provider.
 *
 * <p>Panneau de gauche : arborescence des fédérés chargés/créés dans l'IHM, puis de
 * l'arborescence fusionnée des classes d'objets et d'interactions de tous les fichiers FOM
 * chargés. Panneau de droite : détail (attributs/paramètres, types, sémantique, sélection par
 * fédéré) de l'élément sélectionné. Aucun des fichiers FOM chargés n'est modifié ; les fichiers
 * de fédéré (conformes à {@code federatesFOMProvider.xsd}), en revanche, sont bien ceux que cette
 * IHM a pour but de créer et modifier.</p>
 */
public class MainFrame extends JFrame {

    private final FomModel model = new FomModel();
    private final List<Federate> federates = new ArrayList<>();
    private int loadedFileCount = 0;

    /** Dossier dans lequel s'ouvrent par défaut les fenêtres de dialogue d'ouverture/enregistrement
     *  de fichier : le répertoire de travail courant de l'application (celui depuis lequel elle a
     *  été lancée, ex. via {@code mvn exec:java}), qui correspond au dossier du projet. */
    private static final File PROJECT_DIRECTORY = new File(System.getProperty("user.dir"));

    /** Taille de police de l'arborescence : valeur par défaut de Swing. */
    private static final float TREE_FONT_SIZE = 12f;

    /** Palette de couleurs associées aux fédérés, dans l'ordre de création/chargement. */
    private static final Color[] FEDERATE_PALETTE = {
            new Color(0xE6, 0x19, 0x4B), new Color(0x3C, 0xB4, 0x4B), new Color(0x43, 0x63, 0xD8),
            new Color(0xF5, 0x82, 0x31), new Color(0x91, 0x1E, 0xB4), new Color(0x00, 0x82, 0x9F),
            new Color(0xBC, 0x8B, 0x0C), new Color(0xD8, 0x1B, 0x9C), new Color(0x80, 0x80, 0x00),
            new Color(0x00, 0x80, 0x80)
    };

    private JTree tree;
    private DetailPanel detailPanel;
    private JLabel statusLabel;

    public MainFrame() {
        super("FOM Provider");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1150, 720);
        setLocationRelativeTo(null);

        List<Image> appIcons = AppIcon.windowIcons();
        if (!appIcons.isEmpty()) {
            setIconImages(appIcons);
        }

        initMenuBar();
        initContent();
        refreshStatus();
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Fichier");

        JMenuItem loadItem = new JMenuItem("Charger des fichiers FOM...");
        loadItem.addActionListener(e -> onLoadFiles());

        JMenuItem resetItem = new JMenuItem("Réinitialiser");
        resetItem.addActionListener(e -> onReset());

        JMenuItem quitItem = new JMenuItem("Quitter");
        quitItem.addActionListener(e -> dispose());

        fileMenu.add(loadItem);
        fileMenu.add(resetItem);
        fileMenu.addSeparator();
        fileMenu.add(quitItem);
        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Édition");
        JMenuItem searchItem = new JMenuItem("Rechercher...");
        searchItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        searchItem.addActionListener(e -> onSearch());
        editMenu.add(searchItem);
        menuBar.add(editMenu);

        JMenu federateMenu = new JMenu("Fédérés");

        JMenuItem newFederateItem = new JMenuItem("Nouveau fédéré...");
        newFederateItem.addActionListener(e -> onNewFederate());

        JMenuItem loadFederateItem = new JMenuItem("Charger des fédérés (XML)...");
        loadFederateItem.addActionListener(e -> onLoadFederates());

        JMenuItem renameFederateItem = new JMenuItem("Renommer le fédéré sélectionné...");
        renameFederateItem.addActionListener(e -> onRenameFederate());

        JMenuItem saveFederateItem = new JMenuItem("Enregistrer le fédéré sélectionné");
        saveFederateItem.addActionListener(e -> onSaveFederate(false));

        JMenuItem saveFederateAsItem = new JMenuItem("Enregistrer le fédéré sélectionné sous...");
        saveFederateAsItem.addActionListener(e -> onSaveFederate(true));

        JMenuItem updateAllFederatesItem = new JMenuItem("Mettre à jour tous les fédérés (fichiers existants)");
        updateAllFederatesItem.addActionListener(e -> onUpdateAllFederates());

        JMenuItem unloadFederateItem = new JMenuItem("Décharger le fédéré sélectionné");
        unloadFederateItem.addActionListener(e -> onUnloadFederate());

        federateMenu.add(newFederateItem);
        federateMenu.add(loadFederateItem);
        federateMenu.addSeparator();
        federateMenu.add(renameFederateItem);
        federateMenu.add(saveFederateItem);
        federateMenu.add(saveFederateAsItem);
        federateMenu.add(updateAllFederatesItem);
        federateMenu.addSeparator();
        federateMenu.add(unloadFederateItem);
        menuBar.add(federateMenu);

        setJMenuBar(menuBar);
    }

    private void initContent() {
        tree = new JTree(FomTreeFactory.buildTreeModel(model, federates));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new FomTreeCellRenderer(federates));
        tree.addTreeSelectionListener(this::onTreeSelectionChanged);
        applyTreeFont();

        detailPanel = new DetailPanel(federates);
        detailPanel.setSelectionChangeListener(this::onFederateSelectionChanged);

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setPreferredSize(new Dimension(360, 600));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailPanel);
        splitPane.setDividerLocation(360);
        splitPane.setOneTouchExpandable(true);

        statusLabel = new JLabel();
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(statusLabel, BorderLayout.SOUTH);
    }

    private void onTreeSelectionChanged(TreeSelectionEvent event) {
        refreshDetailForSelection();
    }

    private void refreshDetailForSelection() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) {
            detailPanel.showEmpty();
            return;
        }
        Object userObject = node.getUserObject();
        if (userObject instanceof ObjectClassNode objectClass) {
            detailPanel.showObjectClass(objectClass, collectObjectAncestors(node));
        } else if (userObject instanceof InteractionClassNode interactionClass) {
            detailPanel.showInteractionClass(interactionClass, collectInteractionAncestors(node));
        } else if (userObject instanceof Federate federate) {
            detailPanel.showFederate(federate);
        } else {
            detailPanel.showEmpty();
        }
    }

    /**
     * Appelé quand une case à cocher change d'état dans le panneau de détail (sélection d'un
     * attribut ou d'une interaction pour un fédéré). La pastille de couleur associée au fédéré,
     * dans l'arborescence, dépend de cet état mais n'est pas pilotée par le modèle de l'arbre : un
     * simple {@code tree.repaint()} redessine le nœud avec son contenu à jour, mais ne recalcule
     * pas la largeur déjà mise en cache par l'arbre pour cette ligne, si bien que la pastille
     * ajoutée pouvait rester invisible (hors de la zone recalculée) jusqu'à ce qu'un événement de
     * structure (ex. reconstruction de l'arbre lors de l'ajout d'un autre fédéré) force ce
     * recalcul. On informe donc explicitement le modèle que le nœud actuellement sélectionné a
     * changé d'apparence, via {@link DefaultTreeModel#nodeChanged}, ce qui force l'arbre à
     * recalculer sa taille avant de le redessiner.
     */
    private void onFederateSelectionChanged() {
        Object selected = tree.getLastSelectedPathComponent();
        if (selected instanceof DefaultMutableTreeNode node && tree.getModel() instanceof DefaultTreeModel treeModel) {
            treeModel.nodeChanged(node);
        } else {
            tree.repaint();
        }
    }

    /**
     * Remonte l'arborescence depuis le nœud sélectionné pour récupérer la chaîne de ses classes
     * d'objet parentes, de la plus générale (racine de la hiérarchie) à la plus proche (parent
     * direct). Le panneau de détail s'en sert pour afficher, en plus des attributs propres à la
     * classe, ceux hérités de chacune de ces classes parentes.
     */
    private List<ObjectClassNode> collectObjectAncestors(DefaultMutableTreeNode node) {
        List<ObjectClassNode> ancestors = new ArrayList<>();
        TreeNode parent = node.getParent();
        while (parent instanceof DefaultMutableTreeNode parentNode
                && parentNode.getUserObject() instanceof ObjectClassNode ancestorClass) {
            ancestors.add(0, ancestorClass);
            parent = parentNode.getParent();
        }
        return ancestors;
    }

    /** Équivalent de {@link #collectObjectAncestors} pour les classes d'interaction. */
    private List<InteractionClassNode> collectInteractionAncestors(DefaultMutableTreeNode node) {
        List<InteractionClassNode> ancestors = new ArrayList<>();
        TreeNode parent = node.getParent();
        while (parent instanceof DefaultMutableTreeNode parentNode
                && parentNode.getUserObject() instanceof InteractionClassNode ancestorClass) {
            ancestors.add(0, ancestorClass);
            parent = parentNode.getParent();
        }
        return ancestors;
    }

    private void onLoadFiles() {
        JFileChooser chooser = new JFileChooser(PROJECT_DIRECTORY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Sélectionner un ou plusieurs fichiers FOM (XML)");
        chooser.setFileFilter(new FileNameExtensionFilter("Fichiers FOM (*.xml)", "xml"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File[] files = chooser.getSelectedFiles();
        if (files.length == 0) {
            return;
        }

        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (File file : files) {
            try {
                FomModel parsed = FomFileParser.parse(file);
                FomMerger.mergeInto(model, parsed);
                successCount++;
            } catch (Exception ex) {
                errors.add(file.getName() + " : " + ex.getMessage());
            }
        }

        loadedFileCount += successCount;
        rebuildTree();
        refreshStatus();

        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Certains fichiers n'ont pas pu être chargés :\n" + String.join("\n", errors),
                    "Erreurs de chargement", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Ouvre la fenêtre de recherche (menu "Édition → Rechercher..."), qui liste les classes
     * d'objet, les classes d'interaction et les attributs d'objet du modèle actuellement chargé,
     * filtrés au fur et à mesure de la saisie. Sélectionner une ligne sélectionne l'élément
     * correspondant dans l'arborescence (la classe d'objet propriétaire, dans le cas d'un
     * attribut) et affiche son détail, via {@link #onSearchEntrySelected}.
     */
    private void onSearch() {
        List<SearchEntry> entries = SearchIndex.build(model);
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Rien à rechercher : chargez d'abord un ou plusieurs fichiers FOM.",
                    "Rechercher", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SearchDialog dialog = new SearchDialog(this, entries, this::onSearchEntrySelected);
        dialog.setVisible(true);
    }

    /**
     * Sélectionne dans l'arborescence l'élément visé par l'entrée de recherche choisie (la classe
     * d'objet ou d'interaction elle-même, ou la classe d'objet propriétaire si c'est un attribut
     * qui a été choisi), ce qui déclenche l'affichage de son détail via le
     * {@code TreeSelectionListener} déjà installé sur {@link #tree}.
     */
    private void onSearchEntrySelected(SearchEntry entry) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        DefaultMutableTreeNode target = entry.isInteraction()
                ? findInteractionClassNode(root, entry.getFullClassName())
                : findObjectClassNode(root, entry.getFullClassName());
        if (target == null) {
            JOptionPane.showMessageDialog(this,
                    "Impossible de localiser \"" + entry.getFullClassName() + "\" dans l'arborescence actuelle.",
                    "Rechercher", JOptionPane.WARNING_MESSAGE);
            return;
        }
        TreePath path = new TreePath(target.getPath());
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
        tree.requestFocusInWindow();
    }

    /** Recherche en profondeur, dans le sous-arbre "Objets", le nœud de nom complet donné. */
    private DefaultMutableTreeNode findObjectClassNode(DefaultMutableTreeNode node, String fullName) {
        if (node.getUserObject() instanceof ObjectClassNode
                && FomTreeFactory.fullObjectName(node).equals(fullName)) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode found =
                    findObjectClassNode((DefaultMutableTreeNode) node.getChildAt(i), fullName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Équivalent de {@link #findObjectClassNode} pour le sous-arbre "Interactions". */
    private DefaultMutableTreeNode findInteractionClassNode(DefaultMutableTreeNode node, String fullName) {
        if (node.getUserObject() instanceof InteractionClassNode
                && FomTreeFactory.fullInteractionName(node).equals(fullName)) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode found =
                    findInteractionClassNode((DefaultMutableTreeNode) node.getChildAt(i), fullName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void onReset() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Vider l'arborescence fusionnée et repartir de zéro ?",
                "Réinitialiser", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        model.clear();
        loadedFileCount = 0;
        rebuildTree();
        detailPanel.showEmpty();
        refreshStatus();
    }

    private void onNewFederate() {
        String name = JOptionPane.showInputDialog(this, "Nom du nouveau fédéré :",
                "Nouveau fédéré", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) {
            return;
        }
        Federate federate = new Federate(name.trim());
        federate.setColor(nextFederateColor());
        federates.add(federate);
        rebuildTree();
        refreshStatus();
    }

    private void onLoadFederates() {
        JFileChooser chooser = new JFileChooser(PROJECT_DIRECTORY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Sélectionner un ou plusieurs fichiers de fédéré (XML)");
        chooser.setFileFilter(new FileNameExtensionFilter("Fichiers de fédéré (*.xml)", "xml"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File[] files = chooser.getSelectedFiles();
        if (files.length == 0) {
            return;
        }

        List<String> errors = new ArrayList<>();
        for (File file : files) {
            try {
                Federate federate = FederateFileIO.parse(file);
                federate.setColor(nextFederateColor());
                federates.add(federate);
            } catch (FomParseException ex) {
                errors.add(file.getName() + " : " + ex.getMessage());
            }
        }

        rebuildTree();
        refreshStatus();

        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Certains fédérés n'ont pas pu être chargés :\n" + String.join("\n", errors),
                    "Erreurs de chargement", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onRenameFederate() {
        Federate federate = selectedFederate();
        if (federate == null) {
            showNoFederateSelected();
            return;
        }
        String name = JOptionPane.showInputDialog(this, "Nouveau nom du fédéré :",
                federate.getName());
        if (name == null || name.isBlank()) {
            return;
        }
        federate.setName(name.trim());
        rebuildTree();
        refreshDetailForSelection();
    }

    private void onSaveFederate(boolean forceChooser) {
        Federate federate = selectedFederate();
        if (federate == null) {
            showNoFederateSelected();
            return;
        }
        File targetFile = federate.getFile();
        if (forceChooser || targetFile == null) {
            JFileChooser chooser = new JFileChooser(PROJECT_DIRECTORY);
            chooser.setDialogTitle("Enregistrer le fédéré \"" + federate.getName() + "\"");
            chooser.setFileFilter(new FileNameExtensionFilter("Fichiers de fédéré (*.xml)", "xml"));
            chooser.setSelectedFile(new File(federate.getName() + ".xml"));
            int result = chooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            targetFile = chooser.getSelectedFile();
            if (!targetFile.getName().toLowerCase().endsWith(".xml")) {
                targetFile = new File(targetFile.getParentFile(), targetFile.getName() + ".xml");
            }
        }

        try {
            FederateFileIO.write(federate, targetFile);
            federate.setFile(targetFile);
            refreshDetailForSelection();
            statusLabel.setText("Fédéré \"" + federate.getName() + "\" enregistré dans "
                    + targetFile.getAbsolutePath());
        } catch (FomParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Impossible d'enregistrer le fédéré : " + ex.getMessage(),
                    "Erreur d'enregistrement", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Réenregistre tous les fédérés déjà associés à un fichier (chargés depuis un fichier, ou
     * déjà enregistrés au moins une fois) avec leur état courant. Les fédérés jamais enregistrés
     * sont ignorés puisqu'aucun emplacement n'est connu pour eux (utiliser "Enregistrer sous..."
     * pour ceux-là).
     */
    private void onUpdateAllFederates() {
        List<Federate> toUpdate = new ArrayList<>();
        for (Federate federate : federates) {
            if (federate.getFile() != null) {
                toUpdate.add(federate);
            }
        }
        if (toUpdate.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Aucun fédéré n'est associé à un fichier existant.",
                    "Mise à jour des fédérés", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        for (Federate federate : toUpdate) {
            try {
                FederateFileIO.write(federate, federate.getFile());
                successCount++;
            } catch (FomParseException ex) {
                errors.add(federate.getName() + " : " + ex.getMessage());
            }
        }

        refreshDetailForSelection();
        statusLabel.setText(successCount + " fédéré(s) mis à jour sur " + toUpdate.size());

        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Certains fédérés n'ont pas pu être mis à jour :\n" + String.join("\n", errors),
                    "Erreurs de mise à jour", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Retire le fédéré sélectionné de l'IHM sans toucher à son éventuel fichier XML déjà
     * enregistré (il pourra être rechargé plus tard via "Charger des fédérés...").
     */
    private void onUnloadFederate() {
        Federate federate = selectedFederate();
        if (federate == null) {
            showNoFederateSelected();
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Décharger le fédéré \"" + federate.getName() + "\" de l'IHM "
                        + "(le fichier XML éventuellement déjà enregistré n'est pas supprimé, mais "
                        + "toute modification non enregistrée sera perdue) ?",
                "Décharger le fédéré", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        federates.remove(federate);
        rebuildTree();
        detailPanel.showEmpty();
        refreshStatus();
    }

    private Federate selectedFederate() {
        Object node = tree.getLastSelectedPathComponent();
        if (node instanceof DefaultMutableTreeNode dmtn
                && dmtn.getUserObject() instanceof Federate federate) {
            return federate;
        }
        return null;
    }

    private void showNoFederateSelected() {
        JOptionPane.showMessageDialog(this,
                "Sélectionnez d'abord un fédéré dans l'arborescence.",
                "Aucun fédéré sélectionné", JOptionPane.WARNING_MESSAGE);
    }

    private Color nextFederateColor() {
        return FEDERATE_PALETTE[federates.size() % FEDERATE_PALETTE.length];
    }

    private void applyTreeFont() {
        Font font = tree.getFont().deriveFont(TREE_FONT_SIZE);
        tree.setFont(font);
        if (tree.getCellRenderer() instanceof DefaultTreeCellRenderer renderer) {
            renderer.setFont(font);
        }
        tree.setRowHeight(tree.getFontMetrics(font).getHeight() + 8);
    }

    private void rebuildTree() {
        tree.setModel(FomTreeFactory.buildTreeModel(model, federates));
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void refreshStatus() {
        statusLabel.setText(String.format(
                "%d fichier(s) FOM chargé(s) — %d classe(s) d'objet racine, %d classe(s) "
                        + "d'interaction racine — %d fédéré(s)",
                loadedFileCount, model.getObjectClasses().size(), model.getInteractionClasses().size(),
                federates.size()));
    }
}
