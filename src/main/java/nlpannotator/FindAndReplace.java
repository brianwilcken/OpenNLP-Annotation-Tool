package nlpannotator;

import com.google.common.base.Strings;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import common.Tools;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.WordUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class FindAndReplace extends JFrame implements ListSelectionListener {
    public static void main(String[] args) {
        FindAndReplace findAndReplace = new FindAndReplace("FindAndReplace", null);
        findAndReplace.init();
    }

    private JButton findButton;
    private JButton resetButton;
    private JTextArea txtFind;
    private JTextArea txtReplace;
    private JPanel jPanelFindAndReplace;
    private JList lsFound;
    private JButton replaceAllButton;
    private JButton removeButton;
    private JScrollPane spFound;
    private JButton loadDictionaryButton;
    private JButton nextButton;
    private JButton previousButton;
    private Main annotator;

    private TreeMap<Integer, String> locMap;

    private DefaultListModel<String> found;

    public FindAndReplace(String frame, Main annotator) {
        super(frame);
        this.annotator = annotator;
        found = new DefaultListModel<>();
    }

    public void init() {
        frameInit();
        populate();

        setTitle("Find And Replace");
        setContentPane(jPanelFindAndReplace);
        setLocation(annotator.getLocationOnScreen());
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();
    }

    private void populate() {

        findButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                findActionPerformed(actionEvent);
            }
        });

        replaceAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                replaceAllActionPerformed(actionEvent);
            }
        });

        previousButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                navigatePreviousActionPerformed(actionEvent);
            }
        });

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                navigateNextActionPerformed(actionEvent);
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                removeActionPerformed(actionEvent);
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                resetActionPerformed(actionEvent);
            }
        });

        loadDictionaryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                loadDictionaryActionPerformed(actionEvent);
            }
        });

        lsFound.setModel(found);
        lsFound.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lsFound.setLayoutOrientation(JList.VERTICAL);
        lsFound.setVisibleRowCount(-1);
        lsFound.addListSelectionListener(this);

        spFound.setPreferredSize(new Dimension(400, 200));
    }

    private void findActionPerformed(ActionEvent actionEvent) {
        String find = txtFind.getText();
        if (!Strings.isNullOrEmpty(find)) {
            addFoundElement(find);
            txtFind.setText("");
        }
    }

    private void addFoundElement(String find) {
        if (!found.contains(find)) {
            found.addElement(find);
            int[] selections = lsFound.getSelectedIndices();
            int index = found.indexOf(find);
            int[] newSelections = ArrayUtils.addAll(selections, new int[]{index});
            lsFound.setSelectedIndices(newSelections);
        }
    }

    private void replaceAllActionPerformed(ActionEvent actionEvent) {
        List selections = lsFound.getSelectedValuesList();
        String replace = txtReplace.getText();

        annotator.replaceAllText(selections, replace);

        removeActionPerformed(actionEvent);
        addFoundElement(replace);
    }

    private void navigatePreviousActionPerformed(ActionEvent actionEvent) {
        if (locMap != null) {
            annotator.navigatePrevious(locMap);
        }
    }

    private void navigateNextActionPerformed(ActionEvent actionEvent) {
        if (locMap != null) {
            annotator.navigateNext(locMap);
        }
    }

    private void removeActionPerformed(ActionEvent actionEvent) {
        List selections = lsFound.getSelectedValuesList();
        for (Object selected : selections) {
            found.removeElement(selected);
        }
        // annotator.removeHighlights(selections);
    }

    private void resetActionPerformed(ActionEvent actionEvent) {
        //annotator.removeHighlights(Arrays.asList(found.toArray()));
        found.clear();
    }

    private void loadDictionaryActionPerformed(ActionEvent evt) {
        List categories = annotator.getDocumentCategories();
        for (Object category : categories) {
            String wordsText = Tools.getResource("dictionary/" + category + ".txt");
            List<String> wordsList = Arrays.asList(wordsText.split("\\r\\n"));
            loadSearchTerms(wordsList);
        }
    }

    public void loadSearchTerms(List<String> wordsList) {
        int[] selections = lsFound.getSelectedIndices();
        for (String entry : wordsList) {
            String titleCaps = WordUtils.capitalizeFully(entry);
            String allCaps = entry.toUpperCase();
            selections = updateFoundSelections(entry, selections);
            selections = updateFoundSelections(titleCaps, selections);
            selections = updateFoundSelections(allCaps, selections);
        }
        lsFound.setSelectedIndices(selections);
    }

    private int[] updateFoundSelections(String entry, int[] selections) {
        if (!found.contains(entry)) {
            found.addElement(entry);
            int index = found.indexOf(entry);
            selections = ArrayUtils.addAll(selections, new int[]{index});
        }
        return selections;
    }

    public void reset() {
        found.clear();
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        List selections = lsFound.getSelectedValuesList();
        annotator.removeHighlights();
        annotator.highlightAnnotations();

        locMap = annotator.getLocationMap(selections);
        annotator.highlightFound(locMap);
    }

    public TreeMap<Integer, String> getLocationMap() {
        return locMap;
    }

    public void findHighlighted(String selectedText) {
        addFoundElement(selectedText);
    }

    public List getListSelections() {
        List selections = lsFound.getSelectedValuesList();
        return selections;
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
        jPanelFindAndReplace = new JPanel();
        jPanelFindAndReplace.setLayout(new GridLayoutManager(9, 8, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Find:");
        jPanelFindAndReplace.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Replace With:");
        jPanelFindAndReplace.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findButton = new JButton();
        findButton.setText("Find");
        jPanelFindAndReplace.add(findButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spFound = new JScrollPane();
        jPanelFindAndReplace.add(spFound, new GridConstraints(1, 2, 3, 6, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 100), null, 0, false));
        lsFound = new JList();
        spFound.setViewportView(lsFound);
        final JScrollPane scrollPane1 = new JScrollPane();
        jPanelFindAndReplace.add(scrollPane1, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        txtFind = new JTextArea();
        scrollPane1.setViewportView(txtFind);
        final JScrollPane scrollPane2 = new JScrollPane();
        jPanelFindAndReplace.add(scrollPane2, new GridConstraints(2, 1, 7, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 100), null, 0, false));
        txtReplace = new JTextArea();
        scrollPane2.setViewportView(txtReplace);
        replaceAllButton = new JButton();
        replaceAllButton.setText("Replace All");
        jPanelFindAndReplace.add(replaceAllButton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Found:");
        jPanelFindAndReplace.add(label3, new GridConstraints(0, 2, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        removeButton = new JButton();
        removeButton.setText("Remove");
        jPanelFindAndReplace.add(removeButton, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resetButton = new JButton();
        resetButton.setText("Reset");
        jPanelFindAndReplace.add(resetButton, new GridConstraints(4, 4, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loadDictionaryButton = new JButton();
        loadDictionaryButton.setText("Load Dictionary");
        jPanelFindAndReplace.add(loadDictionaryButton, new GridConstraints(5, 2, 1, 6, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Navigation Controls:");
        jPanelFindAndReplace.add(label4, new GridConstraints(6, 2, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        previousButton = new JButton();
        previousButton.setText("<< Previous");
        jPanelFindAndReplace.add(previousButton, new GridConstraints(7, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nextButton = new JButton();
        nextButton.setText("Next >>");
        jPanelFindAndReplace.add(nextButton, new GridConstraints(7, 4, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jPanelFindAndReplace;
    }
}
