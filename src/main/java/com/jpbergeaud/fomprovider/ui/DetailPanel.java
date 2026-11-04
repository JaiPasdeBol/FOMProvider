package com.jpbergeaud.fomprovider.ui;

import com.jpbergeaud.fomprovider.model.AttributeInfo;
import com.jpbergeaud.fomprovider.model.Federate;
import com.jpbergeaud.fomprovider.model.InteractionClassNode;
import com.jpbergeaud.fomprovider.model.ObjectClassNode;
import com.jpbergeaud.fomprovider.model.ParameterInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Panneau de droite : affiche le détail de l'élément sélectionné dans l'arborescence.
 *
 * <p>Pour une classe d'objet ou d'interaction, montre ses informations générales (sharing,
 * transportation, order, sémantique) ainsi que la liste de ses attributs (objets) ou paramètres
 * (interactions), y compris ceux hérités des classes parentes (une colonne "Classe" indique la
 * classe qui déclare effectivement chaque ligne).</p>
 *
 * <p>Pour une classe d'objet, les premières colonnes du tableau (une par fédéré chargé/créé dans
 * l'IHM) portent le nom du fédéré et contiennent une case à cocher par attribut, signalant que ce
 * fédéré y a accès. Pour une classe d'interaction, la norme HLA ne permettant pas de souscription
 * attribut par attribut, un tableau séparé au-dessus de la liste des paramètres permet de cocher,
 * par fédéré, la souscription à l'interaction dans son intégralité.</p>
 *
 * <p>Pour un fédéré, affiche son nom, sa couleur et un résumé de ses sélections.</p>
 */
public class DetailPanel extends JPanel {

    private static final String CARD_EMPTY = "empty";
    private static final String CARD_CONTENT = "content";
    private static final String CARD_FEDERATE = "federate";

    /** Nom de la colonne "Nom", toujours en première position (figée) du tableau d'attributs. */
    private static final String OBJECT_NAME_COLUMN = "Nom";

    /** Colonnes du tableau d'attributs autres que "Nom" (qui est gérée à part, voir
     *  {@link #OBJECT_NAME_COLUMN}, puisqu'elle occupe toujours la première position, figée). */
    private static final String[] OBJECT_BASE_COLUMNS = {
            "Classe", "Type de données", "Sharing", "Update type", "Update condition",
            "Valeur requise", "Ownership", "Transportation", "Order", "Sémantique"
    };

    private static final String[] INTERACTION_BASE_COLUMNS = {
            "Nom", "Classe", "Type de données", "Sémantique"
    };

    private static final String[] INTERACTION_FEDERATE_COLUMNS = {"Fédéré", "Abonné"};

    /** Tailles de police utilisées dans ce panneau : valeur par défaut de Swing. */
    private static final float TITLE_FONT_SIZE = 12f;
    private static final float TEXT_FONT_SIZE = 12f;

    /** Largeur maximale (en pixels) accordée à une colonne lors de l'ajustement automatique :
     *  au-delà, on compte sur la barre de défilement horizontale plutôt que d'élargir sans limite
     *  une colonne à contenu très long (ex. "Sémantique"). */
    private static final int MAX_COLUMN_WIDTH = 320;
    private static final int COLUMN_PADDING = 24;
    private static final int CHECKBOX_COLUMN_MIN_WIDTH = 60;

    /** Taille (en pixels) de l'icône de l'application affichée quand rien n'est sélectionné. */
    private static final int EMPTY_CARD_ICON_SIZE = 596;

    private final List<Federate> federates;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel headerPanel = new JPanel(new GridBagLayout());
    private final JLabel titleLabel = new JLabel();
    private final JTextArea semanticsArea = new JTextArea();

    /** Nom complet de la classe d'objet actuellement affichée, ou {@code null} si aucune. */
    private String currentFullObjectName;
    /** Nom complet de la classe d'interaction actuellement affichée, ou {@code null} si aucune. */
    private String currentFullInteractionName;
    /** Nombre de colonnes "fédéré" en tête du tableau (uniquement en mode classe d'objet). */
    private int federateCount;
    /** Nom de l'attribut/paramètre représenté par chaque ligne du tableau, dans l'ordre. */
    private List<String> currentRowNames = new ArrayList<>();

    private Runnable selectionChangeListener;

    private final DefaultTableModel tableModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return isFederateColumn(column);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return isFederateColumn(columnIndex) ? Boolean.class : String.class;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            super.setValueAt(value, row, column);
            if (isFederateColumn(column) && row >= 0 && row < currentRowNames.size()) {
                int federateIndex = column - 1;
                if (federateIndex < federates.size()) {
                    Federate federate = federates.get(federateIndex);
                    String attributeName = currentRowNames.get(row);
                    federate.setAttributeSelected(currentFullObjectName, attributeName, Boolean.TRUE.equals(value));
                    notifySelectionChanged();
                }
            }
        }
    };
    private final JTable table = new JTable(tableModel);
    /** Affiche uniquement la première colonne ("Nom") du même modèle que {@link #table}, placée
     *  en en-tête de ligne ("row header") de la {@link JScrollPane} qui contient {@link #table} :
     *  elle reste ainsi toujours visible, quelle que soit la position de la barre de défilement
     *  horizontale du tableau principal (voir {@link #splitFrozenColumn()}). */
    private final JTable fixedColumnTable = new JTable(tableModel);

    private final DefaultTableModel federateInteractionTableModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 1;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 1 ? Boolean.class : String.class;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            super.setValueAt(value, row, column);
            if (column == 1 && currentFullInteractionName != null
                    && row >= 0 && row < federates.size()) {
                Federate federate = federates.get(row);
                federate.setInteractionSelected(currentFullInteractionName, Boolean.TRUE.equals(value));
                notifySelectionChanged();
            }
        }
    };
    private final JTable federateInteractionTable = new JTable(federateInteractionTableModel);
    private JScrollPane federateInteractionScroll;

    private final JLabel federateNameLabel = new JLabel();
    private final JPanel federateColorSwatch = new JPanel();
    private final JLabel federateSummaryLabel = new JLabel();

    public DetailPanel(List<Federate> federates) {
        super();
        this.federates = federates;
        setLayout(cardLayout);
        add(buildEmptyCard(), CARD_EMPTY);
        add(buildContentCard(), CARD_CONTENT);
        add(buildFederateCard(), CARD_FEDERATE);
        showEmpty();
    }

    /** Permet à l'appelant (MainFrame) d'être notifié quand une case à cocher change d'état, afin
     *  de rafraîchir l'affichage des pastilles dans l'arborescence. */
    public void setSelectionChangeListener(Runnable listener) {
        this.selectionChangeListener = listener;
    }

    private void notifySelectionChanged() {
        if (selectionChangeListener != null) {
            selectionChangeListener.run();
        }
    }

    /** {@code true} si la colonne (index dans le modèle) est une colonne à cocher de fédéré, ce
     *  qui suppose un tableau d'attributs d'objet (pas d'interaction) et une colonne située après
     *  la première colonne "Nom", désormais toujours figée en tête de tableau. */
    private boolean isFederateColumn(int column) {
        return currentFullObjectName != null && column >= 1 && column <= federateCount;
    }

    private JPanel buildEmptyCard() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(
                "Sélectionnez une classe d'objet, une classe d'interaction ou un fédéré dans l'arborescence.",
                SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(TEXT_FONT_SIZE));
        label.setForeground(Color.GRAY);
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        label.setVerticalTextPosition(SwingConstants.BOTTOM);
        ImageIcon icon = MascotIcon.scaled(EMPTY_CARD_ICON_SIZE);
        if (icon != null) {
            label.setIcon(icon);
            label.setIconTextGap(16);
        }

        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFederateCard() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        federateNameLabel.setFont(federateNameLabel.getFont().deriveFont(Font.BOLD, TITLE_FONT_SIZE));
        federateNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        federateColorSwatch.setPreferredSize(new Dimension(28, 28));
        federateColorSwatch.setMaximumSize(new Dimension(28, 28));
        federateColorSwatch.setAlignmentX(Component.LEFT_ALIGNMENT);
        federateColorSwatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        federateSummaryLabel.setFont(federateSummaryLabel.getFont().deriveFont(TEXT_FONT_SIZE));
        federateSummaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        federateSummaryLabel.setVerticalAlignment(SwingConstants.TOP);

        panel.add(federateNameLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(federateColorSwatch);
        panel.add(Box.createVerticalStrut(10));
        panel.add(federateSummaryLabel);
        return panel;
    }

    private JPanel buildContentCard() {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, TITLE_FONT_SIZE));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        semanticsArea.setFont(semanticsArea.getFont().deriveFont(TEXT_FONT_SIZE));
        semanticsArea.setLineWrap(true);
        semanticsArea.setWrapStyleWord(true);
        semanticsArea.setEditable(false);
        semanticsArea.setRows(3);
        JScrollPane semanticsScroll = new JScrollPane(semanticsArea);
        semanticsScroll.setBorder(BorderFactory.createTitledBorder("Sémantique"));
        ((javax.swing.border.TitledBorder) semanticsScroll.getBorder())
                .setTitleFont(semanticsScroll.getFont().deriveFont(Font.BOLD, TEXT_FONT_SIZE));
        semanticsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        semanticsScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        semanticsScroll.setPreferredSize(new Dimension(100, 90));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(titleLabel);
        north.add(Box.createVerticalStrut(6));
        north.add(headerPanel);
        north.add(semanticsScroll);

        root.add(north, BorderLayout.NORTH);

        Font tableFont = table.getFont().deriveFont(TEXT_FONT_SIZE);
        table.setFont(tableFont);
        table.setRowHeight(table.getFontMetrics(tableFont).getHeight() + 10);
        table.getTableHeader().setFont(tableFont.deriveFont(Font.BOLD));
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        // Colonnes à largeur fixe (ajustée au contenu) plutôt qu'étirées pour remplir la
        // largeur disponible : au-delà de la largeur totale des colonnes, la JScrollPane fait
        // apparaître une barre de défilement horizontale au lieu de comprimer illisiblement
        // les colonnes (notamment "Sémantique").
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);
        TableCellRenderer defaultHeaderRenderer = table.getTableHeader().getDefaultRenderer();
        table.getTableHeader().setDefaultRenderer(new FederateHeaderRenderer(defaultHeaderRenderer));

        // Colonne "Nom" figée : un second JTable, partageant le même modèle (et le même
        // sélecteur de lignes/trieur que le tableau principal) mais n'affichant que cette unique
        // colonne, posé comme "row header" de la JScrollPane. Le défilement vertical d'un tableau
        // à l'autre est synchronisé automatiquement par JScrollPane ; seule la partie affichée par
        // "table" défile horizontalement, la colonne "Nom" reste donc toujours visible.
        fixedColumnTable.setFont(tableFont);
        fixedColumnTable.setRowHeight(table.getRowHeight());
        fixedColumnTable.getTableHeader().setFont(tableFont.deriveFont(Font.BOLD));
        fixedColumnTable.getTableHeader().setReorderingAllowed(false);
        fixedColumnTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        fixedColumnTable.setFillsViewportHeight(true);
        fixedColumnTable.setRowSorter(table.getRowSorter());
        fixedColumnTable.setSelectionModel(table.getSelectionModel());

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setRowHeaderView(fixedColumnTable);
        tableScroll.setCorner(JScrollPane.UPPER_LEFT_CORNER, fixedColumnTable.getTableHeader());
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Éléments"));
        ((javax.swing.border.TitledBorder) tableScroll.getBorder())
                .setTitleFont(tableScroll.getFont().deriveFont(Font.BOLD, TEXT_FONT_SIZE));

        Font interactionTableFont = federateInteractionTable.getFont().deriveFont(TEXT_FONT_SIZE);
        federateInteractionTable.setFont(interactionTableFont);
        federateInteractionTable.setRowHeight(
                federateInteractionTable.getFontMetrics(interactionTableFont).getHeight() + 8);
        federateInteractionTable.getTableHeader().setFont(interactionTableFont.deriveFont(Font.BOLD));
        federateInteractionTable.getTableHeader().setReorderingAllowed(false);
        federateInteractionTable.setFillsViewportHeight(false);
        federateInteractionScroll = new JScrollPane(federateInteractionTable);
        federateInteractionScroll.setBorder(
                BorderFactory.createTitledBorder("Abonnement des fédérés à l'interaction"));
        ((javax.swing.border.TitledBorder) federateInteractionScroll.getBorder())
                .setTitleFont(federateInteractionScroll.getFont().deriveFont(Font.BOLD, TEXT_FONT_SIZE));
        federateInteractionScroll.setPreferredSize(new Dimension(100, 120));
        federateInteractionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        federateInteractionScroll.setVisible(false);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.add(federateInteractionScroll, BorderLayout.NORTH);
        centerPanel.add(tableScroll, BorderLayout.CENTER);
        root.add(centerPanel, BorderLayout.CENTER);

        return root;
    }

    public void showEmpty() {
        currentFullObjectName = null;
        currentFullInteractionName = null;
        federateCount = 0;
        cardLayout.show(this, CARD_EMPTY);
    }

    /**
     * Affiche le détail d'une classe d'objet, avec ses propres attributs ainsi que ceux hérités
     * de ses classes parentes, et une colonne à cocher par fédéré chargé/créé dans l'IHM.
     *
     * @param objectClass classe d'objet sélectionnée dans l'arborescence
     * @param ancestors   ses classes parentes, de la plus générale (racine) à la plus proche ;
     *                    liste vide si {@code objectClass} est une classe de premier niveau
     */
    public void showObjectClass(ObjectClassNode objectClass, List<ObjectClassNode> ancestors) {
        currentFullInteractionName = null;
        federateInteractionScroll.setVisible(false);
        federateCount = federates.size();

        String fullName = ancestors.isEmpty() ? objectClass.getName()
                : ancestors.stream().map(ObjectClassNode::getName).collect(Collectors.joining("."))
                        + "." + objectClass.getName();
        currentFullObjectName = fullName;

        titleLabel.setText(objectClass.getName());

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Type", "Classe d'objet (objectClass)");
        fields.put("Sharing", objectClass.getSharing());
        setHeaderFields(fields);

        setSemantics(objectClass.getSemantics());

        List<ObjectClassNode> chain = new ArrayList<>(ancestors);
        chain.add(objectClass);

        Map<String, AttributeInfo> mergedAttributes = new LinkedHashMap<>();
        Map<String, String> declaringClass = new LinkedHashMap<>();
        for (ObjectClassNode node : chain) {
            for (AttributeInfo attribute : node.getAttributes().values()) {
                mergedAttributes.put(attribute.getName(), attribute);
                declaringClass.put(attribute.getName(), node.getName());
            }
        }

        currentRowNames = new ArrayList<>();
        Object[][] data = new Object[mergedAttributes.size()][];
        int i = 0;
        for (AttributeInfo attribute : mergedAttributes.values()) {
            Object[] row = new Object[1 + federateCount + OBJECT_BASE_COLUMNS.length];
            int col = 0;
            row[col++] = attribute.getName();
            for (Federate federate : federates) {
                row[col++] = federate.isAttributeSelected(fullName, attribute.getName());
            }
            row[col++] = declaringClass.get(attribute.getName());
            row[col++] = attribute.getDataType();
            row[col++] = attribute.getSharing();
            row[col++] = attribute.getUpdateType();
            row[col++] = attribute.getUpdateCondition();
            row[col++] = attribute.getValueRequired();
            row[col++] = attribute.getOwnership();
            row[col++] = attribute.getTransportation();
            row[col++] = attribute.getOrder();
            row[col] = attribute.getSemantics();
            data[i] = row;
            currentRowNames.add(attribute.getName());
            i++;
        }
        tableModel.setDataVector(data, buildObjectColumns());
        splitFrozenColumn();
        adjustColumnWidths();

        cardLayout.show(this, CARD_CONTENT);
    }

    /**
     * Affiche le détail d'une classe d'interaction, avec ses propres paramètres ainsi que ceux
     * hérités de ses classes parentes, et le tableau d'abonnement des fédérés à l'interaction
     * dans son ensemble (la norme HLA ne permettant pas de souscription paramètre par paramètre).
     *
     * @param interactionClass classe d'interaction sélectionnée dans l'arborescence
     * @param ancestors        ses classes parentes, de la plus générale (racine) à la plus
     *                          proche ; liste vide si {@code interactionClass} est une classe de
     *                          premier niveau
     */
    public void showInteractionClass(InteractionClassNode interactionClass,
                                      List<InteractionClassNode> ancestors) {
        currentFullObjectName = null;
        federateCount = 0;

        String fullName = ancestors.isEmpty() ? interactionClass.getName()
                : ancestors.stream().map(InteractionClassNode::getName).collect(Collectors.joining("."))
                        + "." + interactionClass.getName();
        currentFullInteractionName = fullName;

        titleLabel.setText(interactionClass.getName());

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Type", "Classe d'interaction (interactionClass)");
        fields.put("Sharing", interactionClass.getSharing());
        fields.put("Transportation", interactionClass.getTransportation());
        fields.put("Order", interactionClass.getOrder());
        setHeaderFields(fields);

        setSemantics(interactionClass.getSemantics());

        List<InteractionClassNode> chain = new ArrayList<>(ancestors);
        chain.add(interactionClass);

        Map<String, ParameterInfo> mergedParameters = new LinkedHashMap<>();
        Map<String, String> declaringClass = new LinkedHashMap<>();
        for (InteractionClassNode node : chain) {
            for (ParameterInfo parameter : node.getParameters().values()) {
                mergedParameters.put(parameter.getName(), parameter);
                declaringClass.put(parameter.getName(), node.getName());
            }
        }

        currentRowNames = new ArrayList<>();
        Object[][] data = new Object[mergedParameters.size()][];
        int i = 0;
        for (ParameterInfo parameter : mergedParameters.values()) {
            data[i++] = new Object[]{
                    parameter.getName(), declaringClass.get(parameter.getName()),
                    parameter.getDataType(), parameter.getSemantics()
            };
            currentRowNames.add(parameter.getName());
        }
        tableModel.setDataVector(data, INTERACTION_BASE_COLUMNS);
        splitFrozenColumn();
        adjustColumnWidths();

        Object[][] federateRows = new Object[federates.size()][];
        for (int f = 0; f < federates.size(); f++) {
            Federate federate = federates.get(f);
            federateRows[f] = new Object[]{federate.getName(), federate.isInteractionSelected(fullName)};
        }
        federateInteractionTableModel.setDataVector(federateRows, INTERACTION_FEDERATE_COLUMNS);
        federateInteractionTable.getColumnModel().getColumn(0).setCellRenderer(new FederateNameCellRenderer());
        federateInteractionScroll.setVisible(true);

        cardLayout.show(this, CARD_CONTENT);
    }

    /** Affiche le nom, la couleur et un résumé des sélections d'un fédéré. */
    public void showFederate(Federate federate) {
        currentFullObjectName = null;
        currentFullInteractionName = null;
        federateCount = 0;

        federateNameLabel.setText(federate.getName());
        federateColorSwatch.setBackground(federate.getColor());

        int objectClassCount = federate.getObjectAttributesView().size();
        int attributeCount = federate.getObjectAttributesView().values().stream()
                .mapToInt(Set::size).sum();
        int interactionCount = federate.getInteractionsView().size();
        String fileInfo = federate.getFile() != null
                ? federate.getFile().getAbsolutePath() : "non enregistré";

        federateSummaryLabel.setText("<html>"
                + attributeCount + " attribut(s) sélectionné(s) sur " + objectClassCount + " classe(s) d'objet<br>"
                + interactionCount + " interaction(s) sélectionnée(s)<br>"
                + "Fichier : " + escapeHtml(fileInfo)
                + "</html>");

        cardLayout.show(this, CARD_FEDERATE);
    }

    private String[] buildObjectColumns() {
        String[] columns = new String[1 + federateCount + OBJECT_BASE_COLUMNS.length];
        int i = 0;
        columns[i++] = OBJECT_NAME_COLUMN;
        for (Federate federate : federates) {
            columns[i++] = federate.getName();
        }
        System.arraycopy(OBJECT_BASE_COLUMNS, 0, columns, i, OBJECT_BASE_COLUMNS.length);
        return columns;
    }

    /**
     * Répartit les colonnes du modèle partagé entre les deux {@link JTable} : la première colonne
     * ("Nom") sur {@link #fixedColumnTable} seule, toutes les autres sur {@link #table}. Un appel
     * à {@link DefaultTableModel#setDataVector} reconstruit entièrement les colonnes de CHAQUE
     * JTable qui observe le modèle (donc des deux), avec l'ensemble des colonnes à chaque fois :
     * cette méthode doit donc être appelée après chaque {@code setDataVector} pour rétablir le
     * partage, avant tout ajustement de largeur.
     */
    private void splitFrozenColumn() {
        TableColumnModel mainColumns = table.getColumnModel();
        TableColumn nameColumnInMain = findColumnByModelIndex(mainColumns, 0);
        if (nameColumnInMain != null) {
            mainColumns.removeColumn(nameColumnInMain);
        }

        TableColumnModel fixedColumns = fixedColumnTable.getColumnModel();
        for (int i = fixedColumns.getColumnCount() - 1; i >= 0; i--) {
            TableColumn column = fixedColumns.getColumn(i);
            if (column.getModelIndex() != 0) {
                fixedColumns.removeColumn(column);
            }
        }
    }

    private static TableColumn findColumnByModelIndex(TableColumnModel columnModel, int modelIndex) {
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            if (column.getModelIndex() == modelIndex) {
                return column;
            }
        }
        return null;
    }

    /**
     * Recalcule la largeur préférée de chaque colonne en fonction de son en-tête et du contenu
     * de ses cellules (plafonnée à {@link #MAX_COLUMN_WIDTH}) ; les colonnes de case à cocher
     * reçoivent une largeur fixe minimale plutôt qu'une largeur basée sur "true"/"false". Cet
     * ajustement est nécessaire après chaque {@link DefaultTableModel#setDataVector} : celui-ci
     * reconstruit les colonnes avec une largeur par défaut. La colonne 0 ("Nom", figée) est
     * ajustée sur {@link #fixedColumnTable}, toutes les autres sur {@link #table} — voir
     * {@link #splitFrozenColumn()}, qui doit avoir été appelée avant.
     */
    private void adjustColumnWidths() {
        FontMetrics cellMetrics = table.getFontMetrics(table.getFont());
        FontMetrics headerMetrics = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            int width = headerMetrics.stringWidth(tableModel.getColumnName(col)) + COLUMN_PADDING;
            if (isFederateColumn(col)) {
                width = Math.max(width, CHECKBOX_COLUMN_MIN_WIDTH);
            } else {
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    Object value = tableModel.getValueAt(row, col);
                    String text = value == null ? "" : value.toString();
                    width = Math.max(width, cellMetrics.stringWidth(text) + COLUMN_PADDING);
                }
            }
            int finalWidth = Math.min(width, MAX_COLUMN_WIDTH);
            JTable targetTable = col == 0 ? fixedColumnTable : table;
            TableColumn column = findColumnByModelIndex(targetTable.getColumnModel(), col);
            if (column != null) {
                column.setPreferredWidth(finalWidth);
            }
            if (col == 0) {
                // JViewport calque la taille de la zone "row header" non pas sur la largeur réelle
                // des colonnes de fixedColumnTable, mais sur Scrollable#getPreferredScrollableView
                // portSize() — qui, tant qu'on ne la fixe pas explicitement, vaut par défaut
                // (450, 400) pour n'importe quel JTable. Sans cette ligne, la zone figée s'affiche
                // donc à 450px de large quel que soit le contenu réel de la colonne "Nom", laissant
                // un grand espace vide entre elle et la première colonne défilante.
                Dimension viewportSize = fixedColumnTable.getPreferredScrollableViewportSize();
                fixedColumnTable.setPreferredScrollableViewportSize(new Dimension(finalWidth, viewportSize.height));
            }
        }
    }

    private void setSemantics(String text) {
        semanticsArea.setText(text);
        semanticsArea.setCaretPosition(0);
    }

    private void setHeaderFields(Map<String, String> fields) {
        headerPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 16);

        int row = 0;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            JLabel keyLabel = new JLabel(entry.getKey() + " :");
            keyLabel.setFont(keyLabel.getFont().deriveFont(Font.BOLD, TEXT_FONT_SIZE));
            headerPanel.add(keyLabel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            String value = entry.getValue();
            JLabel valueLabel = new JLabel(value == null || value.isEmpty() ? "—" : value);
            valueLabel.setFont(valueLabel.getFont().deriveFont(TEXT_FONT_SIZE));
            headerPanel.add(valueLabel, gbc);

            row++;
        }
        headerPanel.revalidate();
        headerPanel.repaint();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static Color bestTextColor(Color background) {
        double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen()
                + 0.114 * background.getBlue()) / 255.0;
        return luminance > 0.6 ? Color.BLACK : Color.WHITE;
    }

    /** Colore l'en-tête des colonnes de fédéré avec la couleur du fédéré correspondant. */
    private final class FederateHeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer base;

        FederateHeaderRenderer(TableCellRenderer base) {
            this.base = base;
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = base.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
            // "column" est ici un index de VUE dans le modèle de colonnes de "table" (qui n'affiche
            // plus la colonne "Nom", désormais figée à part) : on le convertit en index de MODÈLE
            // pour retrouver le bon fédéré.
            int modelIndex = tbl.getColumnModel().getColumn(column).getModelIndex();
            int federateIndex = modelIndex - 1;
            if (isFederateColumn(modelIndex) && federateIndex < federates.size() && c instanceof JComponent jc) {
                Color color = federates.get(federateIndex).getColor();
                jc.setOpaque(true);
                jc.setBackground(color);
                jc.setForeground(bestTextColor(color));
            }
            return c;
        }
    }

    /** Colore, comme les en-têtes de colonne de fédéré du tableau d'attributs d'objet, la
     *  première colonne (nom du fédéré) du tableau d'abonnement aux interactions : fond de la
     *  couleur du fédéré, texte en blanc (ou noir si la couleur est trop claire pour rester
     *  lisible). */
    private final class FederateNameCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
            if (row >= 0 && row < federates.size() && c instanceof JComponent jc) {
                Color color = federates.get(row).getColor();
                jc.setOpaque(true);
                jc.setBackground(color);
                jc.setForeground(bestTextColor(color));
            }
            return c;
        }
    }
}
