package nlpannotator;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TrainingIterations extends JFrame {
    private JSpinner iterationsSpinner;
    private JButton startButton;
    private JPanel mainPanel;
    private JSlider percentEntropySlider;
    private JSpinner percentEntropySpinner;
    private Main main;

    public TrainingIterations(Main main) {
        this.main = main;

        setTitle("Training Iterations");
        setContentPane(mainPanel);
        setLocation(main.getLocationOnScreen());
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();

        initSpinners();
        addEventHandlers();
    }

    private void initSpinners() {
        SpinnerNumberModel iterationsModel = new SpinnerNumberModel(100, 10, 1000, 10);
        iterationsSpinner.setModel(iterationsModel);
        iterationsSpinner.setEditor(new JSpinner.NumberEditor(iterationsSpinner));

        SpinnerNumberModel percentEntropyModel = new SpinnerNumberModel(25, 0, 50, 1);
        percentEntropySpinner.setModel(percentEntropyModel);
        percentEntropySpinner.setEditor(new JSpinner.NumberEditor(percentEntropySpinner));
    }

    private void addEventHandlers() {
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int iterations = Integer.parseInt(iterationsSpinner.getValue().toString());
                double percentEntropy = (double) Integer.parseInt(percentEntropySpinner.getValue().toString()) / (double) 100;
                main.trainDoccatModel(iterations, percentEntropy);
                setVisible(false);
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(3, 2, new Insets(5, 5, 5, 5), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Enter Number of Iterations:");
        mainPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        iterationsSpinner = new JSpinner();
        mainPanel.add(iterationsSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startButton = new JButton();
        startButton.setText("Start");
        mainPanel.add(startButton, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, -1), new Dimension(300, -1), new Dimension(300, -1), 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Enter Entropy Percentage:");
        mainPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        percentEntropySpinner = new JSpinner();
        mainPanel.add(percentEntropySpinner, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
