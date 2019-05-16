package nlpannotator;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import common.FacilityTypes;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoDetectionThreshold extends JFrame {
    private JSlider slider1;
    private JButton autoDetectNERButton;
    private JPanel sliderPanel;
    private JComboBox ddlCategory;
    private JTable tblModelList;
    private JSpinner spnrPrecision;
    private Main mainUI;
    private RestTemplate restTemplate;

    private DefaultComboBoxModel ddlCategoryModel;
    private DefaultTableModel tblModelListModel;
    private List<Map<String, Object>> modelListing;

    public static void main(String[] args) {
        AutoDetectionThreshold threshold = new AutoDetectionThreshold(null);
    }

    public AutoDetectionThreshold(Main mainUI) {
        this.mainUI = mainUI;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);

        setTitle("NER Model Library");
        setContentPane(sliderPanel);
        if (mainUI != null) {
            setLocation(mainUI.getLocationOnScreen());
        }
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();

        populateDocumentCategories();
        initSpinner();
        initTableModel();
        addEventHandlers();
    }

    private void initSpinner() {
        SpinnerNumberModel model = new SpinnerNumberModel(0.1, 0, 1, 0.001);
        spnrPrecision.setModel(model);
        spnrPrecision.setEditor(new JSpinner.NumberEditor(spnrPrecision));
    }

    private void initTableModel() {
        tblModelListModel = new DefaultTableModel();
        tblModelListModel.addColumn("Version");
        tblModelListModel.addColumn("Date");
        tblModelListModel.addColumn("Model Sentences");
        tblModelListModel.addColumn("Test Sentences");
        tblModelListModel.addColumn("Test Tags");
        tblModelListModel.addColumn("Test Accuracy");
        tblModelListModel.addColumn("F-Measure");
        tblModelListModel.addColumn("");
        tblModelListModel.addColumn("");
        tblModelListModel.addColumn("");

        tblModelList.setModel(tblModelListModel);

        tblModelList.setDefaultEditor(Object.class, null);
        tblModelList.setAutoCreateRowSorter(true);
        tblModelList.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    private void addEventHandlers() {
        autoDetectNERButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (mainUI.document != null) {
                    double threshold = Double.parseDouble(spnrPrecision.getValue().toString());
                    Map<String, String> modelVersion = getModelVersion();
                    mainUI.autoAnnotateDocument(threshold, modelVersion);
                } else {
                    JOptionPane.showMessageDialog(mainUI, "Please load a document...");
                }
            }
        });

        ddlCategory.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ddlCategorySelectionChanged(actionEvent);
            }
        });
    }

    private void populateDocumentCategories() {
        ddlCategoryModel = new DefaultComboBoxModel();

        ddlCategoryModel.addElement("-- Please Choose --");
        for (String key : FacilityTypes.dictionary.keySet()) {
            ddlCategoryModel.addElement(key);
        }

        ddlCategory.setModel(ddlCategoryModel);
    }

    public void ddlCategorySelectionChanged(ActionEvent actionEvent) {
        try {
            if (ddlCategory.getSelectedIndex() != 0) {
                String category = ddlCategory.getSelectedItem().toString();

                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                RequestEntity<Void> request = RequestEntity.get(new URI(mainUI.getHostURL() + "/documents/NERModelListing" + "?category=" + category))
                        .accept(MediaType.APPLICATION_JSON).build();

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);
                Map<String, Object> jsonDict = response.getBody();

                if (response.getStatusCode() != HttpStatus.OK) {
                    JOptionPane.showMessageDialog(this, "Server error has occurred!!");
                } else {
                    Map<String, Object> data = ((Map<String, Object>) jsonDict.get("data"));
                    modelListing = (List<Map<String, Object>>) data.get(category);
                    clearModelListingTable();
                    populateModelListingTable(modelListing);
                }
            } else {
                clearModelListingTable();
            }
        } catch (URISyntaxException | ResourceAccessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void clearModelListingTable() {
        for (int r = tblModelListModel.getRowCount() - 1; r >= 0; r--) {
            tblModelListModel.removeRow(r);
        }
    }

    private Map<String, String> getModelVersion() {
        Map<String, String> modelVersion = new HashMap<>();
        String category = null;
        String version = null;
        if (ddlCategory.getSelectedIndex() > 0) {
            category = ddlCategory.getSelectedItem().toString();
        }
        if (tblModelList.getSelectedRow() != -1) {
            int row = tblModelList.getSelectedRow();
            version = tblModelList.getValueAt(row, 0).toString();
        }
        if (category != null && version != null) {
            modelVersion.put(category, version);
        }
        return modelVersion;
    }

    private void populateModelListingTable(List<Map<String, Object>> modelListing) {
        for (Map<String, Object> model : modelListing) {
            String modelVersion = model.get("modelVersion").toString();
            String modelDate = model.get("modelDate").toString();
            String numModelSentences = model.get("numModelSentences") != null ? model.get("numModelSentences").toString() : "N/A";
            String numTestSentences = model.get("numSentences") != null ? model.get("numSentences").toString() : "N/A";
            String numTestTags = model.get("tagCount") != null ? model.get("tagCount").toString() : "N/A";
            String entityAccuracy = model.get("entityAccuracy") != null ? model.get("entityAccuracy").toString() : "N/A";
            String entityFMeasure = model.get("entityFMeasure") != null ? model.get("entityFMeasure").toString() : "N/A";

            tblModelListModel.addRow(new Object[]{modelVersion, modelDate, numModelSentences, numTestSentences, numTestTags, entityAccuracy, entityFMeasure, "See Detailed Test Report", "Review Predictions", "Review Testing Corpus"});
        }

        Action seeDetails = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showModelDetails(actionEvent);
            }
        };
        Action reviewPredictions = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                reviewModelData("predLines", "Predictions");
            }
        };
        Action reviewTrainingData = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                reviewModelData("refLines", "Testing Corpus");
            }
        };

        ButtonColumn detailsColumn = new ButtonColumn(tblModelList, seeDetails, 7);
        ButtonColumn predColumn = new ButtonColumn(tblModelList, reviewPredictions, 8);
        ButtonColumn refColumn = new ButtonColumn(tblModelList, reviewTrainingData, 9);
    }

    private void showModelDetails(ActionEvent e) {
        int modelRow = Integer.valueOf(e.getActionCommand());
        String modelVersion = tblModelList.getValueAt(modelRow, 0).toString();
        for (Map<String, Object> model : modelListing) {
            if (model.get("modelVersion").toString().equals(modelVersion)) {
                String testReport = model.get("testReport").toString();
                JTextArea resultText = new JTextArea(testReport);
                JScrollPane scrollPane = new JScrollPane();
                scrollPane.setViewportView(resultText);
                scrollPane.setPreferredSize(new Dimension(2200, 1080));
                resultText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

                JOptionPane pane = new JOptionPane(scrollPane, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION);
                JDialog dialog = pane.createDialog(this, "Test Report for Version " + modelVersion);
                dialog.setModal(false);
                dialog.setVisible(true);

                //JOptionPane.showMessageDialog(this, scrollPane, "Model Details", JOptionPane.INFORMATION_MESSAGE);
                break;
            }
        }
    }

    private void reviewModelData(String attribute, String title) {
        try {
            Map<String, String> modelVersion = getModelVersion();
            String category = modelVersion.keySet().toArray()[0].toString();
            String version = modelVersion.get(category);

            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

            RequestEntity<Void> request = RequestEntity.get(new URI(mainUI.getHostURL() + "/documents/NERModelReviewPredictions" + "?category=" + category + "&version=" + version))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);
            Map<String, Object> jsonDict = response.getBody();

            if (response.getStatusCode() != HttpStatus.OK) {
                JOptionPane.showMessageDialog(this, "Server error has occurred!!");
            } else {
                Map<String, Object> data = ((Map<String, Object>) jsonDict.get("data"));
                String attrData = data.get(attribute).toString();
                List categories = new ArrayList();
                categories.add(category);

                Map document = new HashMap();
                document.put("category", categories);
                document.put("filename", category + " Model Version " + version + " " + title);
                document.put("annotated", attrData);

                mainUI.loadDocument(document);
            }

        } catch (URISyntaxException | ResourceAccessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
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
        sliderPanel = new JPanel();
        sliderPanel.setLayout(new GridLayoutManager(3, 3, new Insets(20, 20, 20, 20), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        sliderPanel.add(panel1, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Select a Model Version for Named Entity Detection");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Model Category:");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ddlCategory = new JComboBox();
        panel1.add(ddlCategory, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        sliderPanel.add(panel2, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel2.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(1600, -1), null, 0, false));
        tblModelList = new JTable();
        scrollPane1.setViewportView(tblModelList);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        sliderPanel.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Precision Threshold:");
        panel3.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spnrPrecision = new JSpinner();
        panel3.add(spnrPrecision, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        autoDetectNERButton = new JButton();
        autoDetectNERButton.setText("Auto Detect NER");
        sliderPanel.add(autoDetectNERButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return sliderPanel;
    }
}
