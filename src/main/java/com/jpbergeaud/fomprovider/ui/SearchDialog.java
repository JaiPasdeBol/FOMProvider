package com.jpbergeaud.fomprovider.ui;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Fenêtre modale de recherche (menu "Édition → Rechercher...").
 *
 * <p>Une zone de texte filtre, à chaque caractère saisi, la liste des classes d'objet, classes
 * d'interaction et attributs d'objet dont le <strong>nom</strong> (pas le nom complet avec points)
 * contient la chaîne tapée, sans tenir compte de la casse ; la liste se réduit au fur et à mesure
 * que l'utilisateur complète sa saisie. L'utilisateur peut ensuite choisir une ligne (double-clic,
 * ou sélection puis Entrée) : l'entrée choisie est transmise au callback fourni par
 * {@link MainFrame}, qui se charge de sélectionner l'élément correspondant dans l'arborescence et
 * d'en afficher le détail — cette fenêtre ne connaît rien, elle, de l'arborescence Swing.</p>
 */
public final class SearchDialog extends JDialog {

    /** Au-delà de ce nombre de correspondances, seules les premières sont affichées (le compteur
     *  en bas de fenêtre indique alors le nombre total réel). */
    private static final int MAX_RESULTS = 200;

    private final List<SearchEntry> allEntries;
    private final Consumer<SearchEntry> onSelect;

    private final JTextField searchField = new JTextField();
    private final DefaultListModel<SearchEntry> listModel = new DefaultListModel<>();
    private final JList<SearchEntry> resultList = new JList<>(listModel);
    private final JLabel countLabel = new JLabel();

    public SearchDialog(Frame owner, List<SearchEntry> allEntries, Consumer<SearchEntry> onSelect) {
        super(owner, "Rechercher", true);
        this.allEntries = allEntries;
        this.onSelect = onSelect;
        buildUi();
        applyFilter("");
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(4, 4));
        topPanel.add(new JLabel("Nom d'une classe d'objet, d'une interaction ou d'un attribut :"),
                BorderLayout.NORTH);
        topPanel.add(searchField, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        resultList.setCellRenderer(new SearchResultRenderer());
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroll = new JScrollPane(resultList);
        listScroll.setPreferredSize(new Dimension(560, 360));
        add(listScroll, BorderLayout.CENTER);

        countLabel.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));
        add(countLabel, BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter(searchField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter(searchField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter(searchField.getText());
            }
        });

        // Permet de naviguer dans la liste de résultats (flèches, Entrée) sans quitter le champ
        // de texte, pour pouvoir filtrer puis valider sans changer de composant avec Tab.
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN -> {
                        moveSelection(1);
                        e.consume();
                    }
                    case KeyEvent.VK_UP -> {
                        moveSelection(-1);
                        e.consume();
                    }
                    case KeyEvent.VK_ENTER -> confirmSelection();
                    case KeyEvent.VK_ESCAPE -> dispose();
                    default -> {
                        // rien de particulier : la saisie continue normalement
                    }
                }
            }
        });

        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    confirmSelection();
                }
            }
        });

        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmSelection();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                }
            }
        });

        setSize(620, 480);
        setLocationRelativeTo(getOwner());
    }

    private void moveSelection(int delta) {
        int size = listModel.getSize();
        if (size == 0) {
            return;
        }
        int current = resultList.getSelectedIndex();
        int next = current < 0 ? 0 : Math.min(Math.max(current + delta, 0), size - 1);
        resultList.setSelectedIndex(next);
        resultList.ensureIndexIsVisible(next);
    }

    private void confirmSelection() {
        SearchEntry selected = resultList.getSelectedValue();
        if (selected == null && !listModel.isEmpty()) {
            selected = listModel.getElementAt(0);
        }
        if (selected != null) {
            dispose();
            onSelect.accept(selected);
        }
    }

    /**
     * Filtre {@link #allEntries} sur le nom simple de chaque entrée (jamais sur son nom complet
     * avec points) et peuple {@link #listModel} avec les correspondances, dans la limite de
     * {@link #MAX_RESULTS}.
     */
    private void applyFilter(String rawQuery) {
        String query = rawQuery.strip().toLowerCase();
        listModel.clear();
        if (query.isEmpty()) {
            countLabel.setText(allEntries.size() + " élément(s) indexé(s) — tapez pour filtrer");
            return;
        }

        List<SearchEntry> matches = allEntries.stream()
                .filter(entry -> entry.getName().toLowerCase().contains(query))
                .collect(Collectors.toList());

        int shown = Math.min(matches.size(), MAX_RESULTS);
        for (int i = 0; i < shown; i++) {
            listModel.addElement(matches.get(i));
        }
        if (shown > 0) {
            resultList.setSelectedIndex(0);
        }

        if (matches.size() > MAX_RESULTS) {
            countLabel.setText(matches.size() + " résultat(s) — affichage limité aux " + MAX_RESULTS + " premiers");
        } else {
            countLabel.setText(matches.size() + " résultat(s)");
        }
    }

    /** Rendu d'une ligne de résultat : le type entre crochets, puis le nom et le contexte. */
    private static final class SearchResultRenderer extends JLabel implements ListCellRenderer<SearchEntry> {

        private SearchResultRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends SearchEntry> list, SearchEntry value,
                                                        int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.toString());
            setFont(list.getFont());
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            return this;
        }
    }
}
