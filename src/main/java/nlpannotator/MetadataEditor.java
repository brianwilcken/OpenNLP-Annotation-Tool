package nlpannotator;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import common.Tools;
import org.springframework.core.ParameterizedTypeReference;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

public class MetadataEditor extends JFrame {
    private RestTemplate restTemplate;
    private JTable documentTable;
    private JPanel panel1;
    private JButton addRowButton;
    private JButton keepChangesButton;
    private JTable entityTable;
    private JTextField categorySTextField;
    private JTextField filenameTextField;
    private JTextField annotatedTextField;
    private JTextField documentStoreIDTextField;
    private JTextField totalDocumentLinesTextField;
    private JTextField URLTextField;
    private JTextField IDTextField;
    private JTextField lastUpdatedDateTimeTextField;
    private JTextField lastUpdatedByTextField;
    private JTextField createdDateTimeTextField;
    private Main mainUI;

    private DefaultTableModel documentTableModel;
    private DefaultTableModel entitiesTableModel;

    public MetadataEditor(Main mainUI) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);
        this.mainUI = mainUI;

        setTitle("Metadata Editor");
        setContentPane(panel1);
        setLocation(mainUI.getLocationOnScreen());
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();

        initFormFields();
        initDocumentTableModel();
        initEntitiesTableModel();
        initEventListeners();
    }

    public static void main(String[] args) {
        MetadataEditor metadataEditor = new MetadataEditor(null);

        metadataEditor.populate(null);
    }

    private void initFormFields() {
        IDTextField.setEditable(false);
        documentStoreIDTextField.setEditable(false);
        filenameTextField.setEditable(false);
        categorySTextField.setEditable(false);
        totalDocumentLinesTextField.setEditable(false);
        annotatedTextField.setEditable(false);
        createdDateTimeTextField.setEditable(false);
        lastUpdatedByTextField.setEditable(false);
        lastUpdatedDateTimeTextField.setEditable(false);
    }

    private void initDocumentTableModel() {
        documentTableModel = new DefaultTableModel();
        documentTableModel.addColumn("Key");
        documentTableModel.addColumn("Value");

        documentTable.setModel(documentTableModel);
        documentTable.setAutoCreateRowSorter(true);
    }

    private void initEntitiesTableModel() {
        entitiesTableModel = new DefaultTableModel();
        entitiesTableModel.addColumn("Key");
        entitiesTableModel.addColumn("Value");

        entityTable.setModel(entitiesTableModel);
        entityTable.setDefaultEditor(Object.class, null);
        entityTable.setAutoCreateRowSorter(true);
    }

    private void initEventListeners() {
        addRowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                documentTableModel.addRow(new Object[]{"", ""});
            }
        });

        keepChangesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Map<Object, Object> doc = (Map<Object, Object>) documentTableModel.getDataVector().stream().collect(Collectors.toMap(p -> ((Vector) p).get(0), p -> ((Vector) p).get(1)));
                String url = URLTextField.getText();
                doc.put("url", url);
                mainUI.updateMetadata(doc);
                setVisible(false);
            }
        });
    }

    private void resetFormFields() {
        IDTextField.setText("");
        documentStoreIDTextField.setText("");
        filenameTextField.setText("");
        URLTextField.setText("");
        categorySTextField.setText("");
        totalDocumentLinesTextField.setText("");
        annotatedTextField.setText("");
        createdDateTimeTextField.setText("");
        lastUpdatedByTextField.setText("");
        lastUpdatedDateTimeTextField.setText("");
    }

    private void populateFormFields(Map<Object, Object> cpDoc) {
        IDTextField.setText(cpDoc.get("id").toString());
        if (cpDoc.containsKey("docStoreId")) {
            documentStoreIDTextField.setText(cpDoc.get("docStoreId").toString());
        }
        filenameTextField.setText(cpDoc.get("filename").toString());
        if (cpDoc.containsKey("url")) {
            URLTextField.setText(cpDoc.get("url").toString());
            cpDoc.remove("url");
        }
        if (cpDoc.containsKey("category")) {
            categorySTextField.setText(cpDoc.get("category").toString());
            cpDoc.remove("category");
        }
        if (cpDoc.containsKey("totalLines")) {
            totalDocumentLinesTextField.setText(cpDoc.get("totalLines").toString());
            cpDoc.remove("totalLines");
        }
        if (cpDoc.containsKey("percentAnnotated")) {
            annotatedTextField.setText(cpDoc.get("percentAnnotated").toString() + "%");
            cpDoc.remove("percentAnnotated");
        }
        String created = Tools.getFormattedDateTimeString(Instant.ofEpochMilli((long) cpDoc.get("created"))).toString();
        createdDateTimeTextField.setText(created);
        if (cpDoc.containsKey("annotatedBy")) {
            lastUpdatedByTextField.setText(cpDoc.get("annotatedBy").toString());
            cpDoc.remove("annotatedBy");
        }
        String lastUpdated = Tools.getFormattedDateTimeString(Instant.ofEpochMilli((long) cpDoc.get("lastUpdated"))).toString();
        lastUpdatedDateTimeTextField.setText(lastUpdated);

        cpDoc.remove("id");
        cpDoc.remove("docStoreId");
        cpDoc.remove("filename");
        cpDoc.remove("created");
        cpDoc.remove("annotatedBy");
        cpDoc.remove("lastUpdated");
        if (cpDoc.containsKey("_version_")) {
            cpDoc.remove("_version_");
        }
        if (cpDoc.containsKey("annotated")) {
            cpDoc.remove("annotated");
        }
        if (cpDoc.containsKey("parsed")) {
            cpDoc.remove("parsed");
        }
        if (cpDoc.containsKey("docText")) {
            cpDoc.remove("docText");
        }
    }

    public void populate(Map<Object, Object> document) {
        resetFormFields();

        Map<Object, Object> cpDoc = new HashMap<>(document);

        populateFormFields(cpDoc);
        populateDocumentTable(cpDoc);
        populateEntitiesTable(document);
    }

    private void populateDocumentTable(Map<Object, Object> cpDoc) {
        for (int r = documentTableModel.getRowCount() - 1; r >= 0; r--) {
            documentTableModel.removeRow(r);
        }

        for (Map.Entry entry : cpDoc.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            documentTableModel.addRow(new Object[]{key, value.toString()});
        }
    }

    private void populateEntitiesTable(Map<Object, Object> document) {
        List<Map<String, Object>> entities = getDocumentEntities(document);

        for (int r = entitiesTableModel.getRowCount() - 1; r >= 0; r--) {
            entitiesTableModel.removeRow(r);
        }

        if (entities != null) {
            for (Map<String, Object> entity : entities) {
                String key;
                String value;
                if (entity.containsKey("entity")) {
                    if (entity.containsKey("type")) {
                        key = "entity (" + entity.get("type").toString() + ")";
                    } else {
                        key = "entity";
                    }
                    value = entity.get("entity").toString() + " (line: " + entity.get("line").toString() + ")";
                } else {
                    key = "location";
                    value = entity.get("name").toString();
                }
                entitiesTableModel.addRow(new Object[]{key, value});
            }
        }
    }

    private List<Map<String, Object>> getDocumentEntities(Map<Object, Object> document) {
        try {
            String docId = document.get("id").toString();
            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

            RequestEntity<Void> request = RequestEntity.get(new URI(mainUI.getHostURL() + "/documents/entities/" + docId))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            Map<String, Object> jsonDict = response.getBody();

            List<Map<String, Object>> entities = ((List<Map<String, Object>>) jsonDict.get("data"));

            return entities;
        } catch (URISyntaxException | ResourceAccessException e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
            return null;
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
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 4, new Insets(10, 10, 10, 10), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(2, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        documentTable = new JTable();
        scrollPane1.setViewportView(documentTable);
        final JLabel label1 = new JLabel();
        label1.setText("Other Metadata");
        panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel1.add(scrollPane2, new GridConstraints(5, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        entityTable = new JTable();
        scrollPane2.setViewportView(entityTable);
        final JLabel label2 = new JLabel();
        label2.setText("Entities (Read-Only)");
        panel1.add(label2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(10, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(200, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Filename");
        panel2.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Category(s)");
        panel2.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Solr ID");
        panel2.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("% Annotated");
        panel2.add(label6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Mongo ID");
        panel2.add(label7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Last Updated Date/Time");
        panel2.add(label8, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Last Updated By");
        panel2.add(label9, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("Created Date/Time");
        panel2.add(label10, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("Total Document Lines");
        panel2.add(label11, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("URL");
        panel2.add(label12, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(10, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(600, -1), null, 0, false));
        URLTextField = new JTextField();
        panel3.add(URLTextField, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        filenameTextField = new JTextField();
        panel3.add(filenameTextField, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        IDTextField = new JTextField();
        panel3.add(IDTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        documentStoreIDTextField = new JTextField();
        panel3.add(documentStoreIDTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        categorySTextField = new JTextField();
        panel3.add(categorySTextField, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        lastUpdatedDateTimeTextField = new JTextField();
        panel3.add(lastUpdatedDateTimeTextField, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        lastUpdatedByTextField = new JTextField();
        panel3.add(lastUpdatedByTextField, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        createdDateTimeTextField = new JTextField();
        panel3.add(createdDateTimeTextField, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        annotatedTextField = new JTextField();
        panel3.add(annotatedTextField, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        totalDocumentLinesTextField = new JTextField();
        panel3.add(totalDocumentLinesTextField, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        addRowButton = new JButton();
        addRowButton.setText("Add Other Metadata");
        panel1.add(addRowButton, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        keepChangesButton = new JButton();
        keepChangesButton.setText("Keep Changes");
        panel1.add(keepChangesButton, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        label3.setLabelFor(filenameTextField);
        label4.setLabelFor(categorySTextField);
        label5.setLabelFor(IDTextField);
        label6.setLabelFor(annotatedTextField);
        label7.setLabelFor(documentStoreIDTextField);
        label8.setLabelFor(lastUpdatedDateTimeTextField);
        label9.setLabelFor(lastUpdatedByTextField);
        label10.setLabelFor(createdDateTimeTextField);
        label11.setLabelFor(totalDocumentLinesTextField);
        label12.setLabelFor(URLTextField);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}
