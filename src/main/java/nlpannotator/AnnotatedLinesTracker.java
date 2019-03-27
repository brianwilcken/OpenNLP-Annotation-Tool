package nlpannotator;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnotatedLinesTracker extends JFrame {
    private JTable annotatedLinesTable;
    private JPanel panel1;
    private JCheckBox onlyShowSelectedAnnotationsCheckBox;
    private JTable annotationsTable;
    private JButton deleteSelectedAnnotationButton;
    private JButton findMoreLikeThisButton;
    private Main mainUI;

    private DefaultTableModel tableModel;
    private DefaultTableModel annotationsTableModel;

    private TreeMap<Integer, String> annotatedLines;
    private List<Map<String, Object>> autoAnnotateEntities;

    private AnnotationSelectionManager annotationSelectionManager;

    public AnnotatedLinesTracker(Main mainUI) {
        this.mainUI = mainUI;

        annotationSelectionManager = new AnnotationSelectionManager();

        setTitle("Annotated Lines Tracker");
        setContentPane(panel1);
        if (mainUI != null) {
            setLocation(mainUI.getLocationOnScreen());
        }
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();

        init();
    }

    private void init() {
        annotatedLinesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (annotatedLinesTable.getSelectedRow() != -1) {
                    String text = annotatedLinesTable.getValueAt(annotatedLinesTable.getSelectedRow(), 1).toString();
                    mainUI.navigateToLine(text);
                }
            }
        });

        annotationsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                populateAnnotatedLinesTableModel();
                if (annotationsTable.getSelectedRow() != -1) {
                    String entity = annotationsTable.getValueAt(annotationsTable.getSelectedRow(), 0).toString();
                    String type = annotationsTable.getValueAt(annotationsTable.getSelectedRow(), 1).toString();
                    String fullAnnotation = "<START:" + type + "> " + entity + " <END>";
                    for (int r = annotatedLinesTable.getRowCount() - 1; r >= 0; r--) {
                        String line = annotatedLinesTable.getValueAt(r, 1).toString();
                        if (!line.contains(fullAnnotation)) {
                            tableModel.removeRow(r);
                        }
                    }
                }
            }
        });

        onlyShowSelectedAnnotationsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mainUI.updateAnnotatedLinesList();
            }
        });

        deleteSelectedAnnotationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteSelectedAnnotation();
            }
        });

        findMoreLikeThisButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                findMoreLikeThisAnnotation();
            }
        });
    }

    private void deleteSelectedAnnotation() {
        if (annotationsTable.getSelectedRow() != -1 && JOptionPane.showConfirmDialog(this, "Really delete annotation?") == 0) {
            int row = annotationsTable.getSelectedRow();
            String annotation = annotationsTable.getValueAt(row, 0).toString();
            String type = annotationsTable.getValueAt(row, 1).toString();
            mainUI.deleteAnnotation(annotation, type);
        } else {
            JOptionPane.showMessageDialog(this, "Select an annotation...");
        }
    }

    private void findMoreLikeThisAnnotation() {
        if (annotationsTable.getSelectedRow() != -1) {
            int row = annotationsTable.getSelectedRow();
            String annotation = annotationsTable.getValueAt(row, 0).toString();
            mainUI.findMoreLikeThisAnnotation(annotation);
        } else {
            JOptionPane.showMessageDialog(this, "Select an annotation...");
        }
    }

    public boolean inSelectedAnnotationMode() {
        return onlyShowSelectedAnnotationsCheckBox.isSelected();
    }

    public void update(TreeMap<Integer, String> annotatedLines) {
        this.annotatedLines = annotatedLines;

        tableModel = new DefaultTableModel();
        tableModel.addColumn("Line #");
        tableModel.addColumn("Text");

        populateAnnotatedLinesTableModel();

        annotatedLinesTable.setModel(tableModel);
        annotatedLinesTable.getColumnModel().getColumn(0).setMaxWidth(50);
        annotatedLinesTable.getColumnModel().getColumn(1).setCellRenderer(new WordWrapCellRenderer());
        annotatedLinesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        annotatedLinesTable.setDefaultEditor(Object.class, null);

        populateAnnotationsList(annotatedLines);
    }

    public void acknowledgeAutoEntities(List<Map<String, Object>> autoAnnotateEntities) {
        this.autoAnnotateEntities = new ArrayList<>();
        this.autoAnnotateEntities.addAll(autoAnnotateEntities);
    }

    public void forgetAutoEntities() {
        this.autoAnnotateEntities = null;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("All Annotated Lines:");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel2.add(scrollPane1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        annotatedLinesTable = new JTable();
        scrollPane1.setViewportView(annotatedLinesTable);
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        onlyShowSelectedAnnotationsCheckBox = new JCheckBox();
        onlyShowSelectedAnnotationsCheckBox.setText("Only Show Selected Annotations");
        panel2.add(onlyShowSelectedAnnotationsCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, new Dimension(800, 400), new Dimension(-1, 400), new Dimension(-1, 400), 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Annotations:");
        panel3.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findMoreLikeThisButton = new JButton();
        findMoreLikeThisButton.setText("Find More Like This");
        panel3.add(findMoreLikeThisButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel3.add(scrollPane2, new GridConstraints(1, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        annotationsTable = new JTable();
        scrollPane2.setViewportView(annotationsTable);
        deleteSelectedAnnotationButton = new JButton();
        deleteSelectedAnnotationButton.setText("Delete Selected Annotation");
        panel3.add(deleteSelectedAnnotationButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    private static class WordWrapCellRenderer extends JTextArea implements TableCellRenderer {
        public WordWrapCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value.toString());
            setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
            if (table.getRowHeight(row) != getPreferredSize().height) {
                table.setRowHeight(row, getPreferredSize().height);
            }
            return this;
        }
    }

    private void populateAnnotatedLinesTableModel() {
        for (int r = tableModel.getRowCount() - 1; r >= 0; r--) {
            tableModel.removeRow(r);
        }

        for (Map.Entry<Integer, String> entry : annotatedLines.entrySet()) {
            int line = entry.getKey() + 1;
            String annotation = entry.getValue();
            tableModel.addRow(new Object[]{line, annotation});
        }
    }

    private class AnnotationSelectionManager {
        private String annotation;
        private String type;

        public void saveAnnotationSelection() {
            int row = annotationsTable.getSelectedRow();
            if (row != -1) {
                annotation = annotationsTable.getValueAt(row, 0).toString();
                type = annotationsTable.getValueAt(row, 1).toString();
            } else {
                annotation = null;
                type = null;
            }
        }

        public void restoreAnnotationSelection() {
            if (annotation != null && type != null) {
                for (int r = 0; r < annotationsTable.getRowCount(); r++) {
                    String lineAnnotation = annotationsTable.getValueAt(r, 0).toString();
                    String lineType = annotationsTable.getValueAt(r, 1).toString();
                    if (lineAnnotation.equals(annotation) && lineType.equals(type)) {
                        annotationsTable.setRowSelectionInterval(r, r);
                        break;
                    }
                }
            }
        }
    }

    private void populateAnnotationsList(TreeMap<Integer, String> annotatedLines) {
        annotationSelectionManager.saveAnnotationSelection();

        annotationsTableModel = new DefaultTableModel();
        annotationsTableModel.addColumn("Annotation");
        annotationsTableModel.addColumn("Type");
        annotationsTableModel.addColumn("Source");
        annotationsTableModel.addColumn("Probability");

        Pattern annotationExtractor = Pattern.compile("(?<=<START:).+?(?= <END>)");
        Pattern entityExtractor = Pattern.compile("(?<=> ).+");
        Pattern typeExtractor = Pattern.compile(".+(?=>)");

        Set<String> annotations = new TreeSet<>();
        for (Map.Entry<Integer, String> entry : annotatedLines.entrySet()) {
            String annotation = entry.getValue();
            Matcher matcher = annotationExtractor.matcher(annotation);

            while (matcher.find()) {
                String annotatedText = annotation.substring(matcher.start(), matcher.end());
                Matcher entityMatcher = entityExtractor.matcher(annotatedText);
                Matcher typeMatcher = typeExtractor.matcher(annotatedText);
                if (entityMatcher.find() && typeMatcher.find()) {
                    String entity = annotatedText.substring(entityMatcher.start(), entityMatcher.end());
                    String type = annotatedText.substring(typeMatcher.start(), typeMatcher.end());
                    Map<String, String> sourceProb = getAnnotationSource(entity, type);
                    String source = sourceProb.get("Source");
                    String prob = sourceProb.get("Prob");
                    String element = entity + "\t [" + type + "]";
                    if (!annotations.contains(element)) {
                        annotations.add(element);
                        annotationsTableModel.addRow(new Object[]{entity, type, source, prob});
                    }
                }
            }
        }

        annotationsTable.setModel(annotationsTableModel);
        annotationsTable.setAutoCreateRowSorter(true);
        annotationsTable.setDefaultEditor(Object.class, null);

        annotationsTable.getColumn("Probability").setMaxWidth(150);
        annotationsTable.getColumn("Probability").setMinWidth(150);
        annotationsTable.getColumn("Probability").setResizable(false);

        //sort by the Annotation column
        DefaultRowSorter sorter = ((DefaultRowSorter) annotationsTable.getRowSorter());
        ArrayList list = new ArrayList();
        list.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(list);
        sorter.sort();

        annotationSelectionManager.restoreAnnotationSelection();
    }

    private Map<String, String> getAnnotationSource(String entityName, String entityType) {
        Map<String, String> sourceProb = new HashMap<>();
        if (autoAnnotateEntities != null) {
            for (Map<String, Object> entity : autoAnnotateEntities) {
                Map<String, Object> span = (Map<String, Object>) entity.get("span");
                String type = span.get("type").toString();
                if (entity.get("entity").toString().equals(entityName) && type.equals(entityType)) {
                    if (entity.containsKey("source")) {
                        String source = entity.get("source").toString();
                        double prob = (double) span.get("prob");
                        sourceProb.put("Source", source);
                        sourceProb.put("Prob", Double.toString(prob));
                        return sourceProb;
                    }
                    break;
                }
            }
        }
        sourceProb.put("Source", "Annotation");
        sourceProb.put("Prob", Double.toString(1.0));
        return sourceProb;
    }

}
