package nlpannotator;

import org.apache.commons.lang.ArrayUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class FindAndReplace extends JFrame implements ListSelectionListener {
    public static void main(String[] args) {
        FindAndReplace findAndReplace = new FindAndReplace("FindAndReplace", null);
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
    private Main annotator;

    private DefaultListModel<String> found;

    public FindAndReplace(String frame, Main annotator) {
        super(frame);
        this.annotator = annotator;
        found = new DefaultListModel<>();

        populate();

        setTitle("Find And Replace");
        setContentPane(jPanelFindAndReplace);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();
        setVisible(true);
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

        lsFound.setModel(found);
        lsFound.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lsFound.setLayoutOrientation(JList.VERTICAL);
        lsFound.setVisibleRowCount(-1);
        lsFound.addListSelectionListener(this);

        spFound.setPreferredSize(new Dimension(400, 200));
    }

    private void findActionPerformed(ActionEvent actionEvent) {
        String find = txtFind.getText();
        addFoundElement(find);
        txtFind.setText("");
    }

    private void addFoundElement(String find) {
        if (!found.contains(find)) {
            found.addElement(find);
            int[] selections = lsFound.getSelectedIndices();
            int index = found.indexOf(find);
            int[] newSelections = ArrayUtils.addAll(selections, new int[] { index });
            lsFound.setSelectedIndices(newSelections);
        }
    }

    private void replaceAllActionPerformed(ActionEvent actionEvent) {
        java.util.List selections = lsFound.getSelectedValuesList();
        String replace = txtReplace.getText();

        annotator.replaceAllText(selections, replace);

        removeActionPerformed(actionEvent);
        addFoundElement(replace);
    }

    private void removeActionPerformed(ActionEvent actionEvent) {
        java.util.List selections = lsFound.getSelectedValuesList();
        for (Object selected : selections) {
            found.removeElement(selected);
        }
        annotator.removeHighlights(selections);
    }

    private void resetActionPerformed(ActionEvent actionEvent) {
        annotator.removeHighlights(Arrays.asList(found.toArray()));
        found.clear();
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        java.util.List selections = lsFound.getSelectedValuesList();
        annotator.removeHighlights(Arrays.asList(found.toArray()));
        annotator.highlightFound(selections);
    }

    public void findHighlighted(String selectedText) {
        addFoundElement(selectedText);
    }
}
