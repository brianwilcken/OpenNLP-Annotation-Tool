package nlpannotator;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
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

    public AnnotatedLinesTracker(Main mainUI) {
        this.mainUI = mainUI;

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
        annotatedLinesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        annotatedLinesTable.setDefaultEditor(Object.class, null);

        populateAnnotationsList(annotatedLines);
    }

    private void populateAnnotatedLinesTableModel() {
        for (int r = tableModel.getRowCount() - 1; r >= 0; r--) {
            tableModel.removeRow(r);
        }

        for (Map.Entry<Integer, String> entry : annotatedLines.entrySet()) {
            int line = entry.getKey();
            String annotation = entry.getValue();
            tableModel.addRow(new Object[]{line, annotation});
        }
    }

    private void populateAnnotationsList(TreeMap<Integer, String> annotatedLines) {
        annotationsTableModel = new DefaultTableModel();
        annotationsTableModel.addColumn("Annotation");
        annotationsTableModel.addColumn("Type");

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
                    String element = entity + "\t [" + type + "]";
                    if (!annotations.contains(element)) {
                        annotations.add(element);
                        annotationsTableModel.addRow(new Object[]{entity, type});
                    }
                }
            }
        }

        annotationsTable.setModel(annotationsTableModel);
        annotationsTable.setAutoCreateRowSorter(true);
        annotationsTable.setDefaultEditor(Object.class, null);

        //sort by the Annotation column
        DefaultRowSorter sorter = ((DefaultRowSorter) annotationsTable.getRowSorter());
        ArrayList list = new ArrayList();
        list.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(list);
        sorter.sort();
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
        panel1.setLayout(new GridLayoutManager(2, 10, new Insets(0, 0, 0, 0), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(1600, -1), null, 0, false));
        annotatedLinesTable = new JTable();
        scrollPane1.setViewportView(annotatedLinesTable);
        final JLabel label1 = new JLabel();
        label1.setText("All Annotated Lines:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        onlyShowSelectedAnnotationsCheckBox = new JCheckBox();
        onlyShowSelectedAnnotationsCheckBox.setText("Only Show Selected Annotations");
        panel1.add(onlyShowSelectedAnnotationsCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel1.add(scrollPane2, new GridConstraints(1, 2, 1, 8, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(600, -1), null, 0, false));
        annotationsTable = new JTable();
        scrollPane2.setViewportView(annotationsTable);
        findMoreLikeThisButton = new JButton();
        findMoreLikeThisButton.setText("Find More Like This");
        panel1.add(findMoreLikeThisButton, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Annotations:");
        panel1.add(label2, new GridConstraints(0, 2, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteSelectedAnnotationButton = new JButton();
        deleteSelectedAnnotationButton.setText("Delete Selected Annotation");
        panel1.add(deleteSelectedAnnotationButton, new GridConstraints(0, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 9, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}
