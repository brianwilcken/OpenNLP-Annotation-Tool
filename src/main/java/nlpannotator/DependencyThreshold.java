package nlpannotator;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DependencyThreshold extends JFrame {
    private JPanel panel1;
    private JSlider slider1;
    private JSlider slider2;
    private JButton processDependenciesButton;
    private DependencyResolver resolver;

    public DependencyThreshold(DependencyResolver resolver) {
        this.resolver = resolver;

        setTitle("Dependency Extraction Threshold");
        setContentPane(panel1);
        setLocation(resolver.getLocationOnScreen());
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();

        initComponents();
    }

    private void initComponents() {
        processDependenciesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                double similarityThreshold = (double) slider1.getValue() / (double) 100;
                double lineRange = slider2.getValue();
                resolver.reprocessRelations(similarityThreshold, lineRange);
            }
        });
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
        panel1.setLayout(new GridLayoutManager(5, 3, new Insets(20, 20, 20, 20), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Select a threshold for term similarity:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        slider1 = new JSlider();
        slider1.setMajorTickSpacing(25);
        slider1.setMaximum(100);
        slider1.setMinorTickSpacing(5);
        slider1.setPaintLabels(true);
        slider1.setPaintTicks(true);
        slider1.setSnapToTicks(true);
        slider1.setValue(50);
        panel1.add(slider1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Select a threshold for sentence search range:");
        panel1.add(label2, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        slider2 = new JSlider();
        slider2.setInverted(false);
        slider2.setMajorTickSpacing(10);
        slider2.setMaximum(50);
        slider2.setMinimum(0);
        slider2.setMinorTickSpacing(1);
        slider2.setPaintLabels(true);
        slider2.setPaintTicks(true);
        slider2.setSnapToTicks(true);
        slider2.setValue(5);
        panel1.add(slider2, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        processDependenciesButton = new JButton();
        processDependenciesButton.setText("Process Dependencies");
        panel1.add(processDependenciesButton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(4, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}