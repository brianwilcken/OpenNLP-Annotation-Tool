package nlpannotator;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
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
    private Main mainUI;

    public MetadataEditor(Main mainUI) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);
        this.mainUI = mainUI;

        setTitle("Metadata Editor");
        setContentPane(panel1);
        setLocation(mainUI.getLocationOnScreen());
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    public static void main(String[] args) {
        MetadataEditor metadataEditor = new MetadataEditor(null);

        metadataEditor.populate(null);
    }

    public void populate(Map<Object, Object> document) {
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn("Key");
        tableModel.addColumn("Value");

        for (Map.Entry entry : document.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            tableModel.addRow(new Object[]{key, value.toString()});
        }

        documentTable.setModel(tableModel);

        buildEntitiesTable(document);

        addRowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                tableModel.addRow(new Object[]{"", ""});
            }
        });

        keepChangesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Map<Object, Object> doc = (Map<Object, Object>) tableModel.getDataVector().stream().collect(Collectors.toMap(p -> ((Vector) p).get(0), p -> ((Vector) p).get(1)));
                mainUI.updateMetadata(doc);
                setVisible(false);
            }
        });
    }

    private void buildEntitiesTable(Map<Object, Object> document) {
        List<Map<String, Object>> entities = getDocumentEntities(document);

        if (entities != null) {
            DefaultTableModel tableModel = new DefaultTableModel();
            tableModel.addColumn("Key");
            tableModel.addColumn("Value");

            for (Map<String, Object> entity : entities) {
                String key = null;
                String value = null;
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
                tableModel.addRow(new Object[]{key, value});
            }

            entityTable.setModel(tableModel);
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
        } catch (URISyntaxException e) {
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
        panel1.setLayout(new GridLayoutManager(5, 3, new Insets(0, 0, 0, 0), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        documentTable = new JTable();
        scrollPane1.setViewportView(documentTable);
        addRowButton = new JButton();
        addRowButton.setText("Add Row");
        panel1.add(addRowButton, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        keepChangesButton = new JButton();
        keepChangesButton.setText("Keep Changes");
        panel1.add(keepChangesButton, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Document Metadata");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel1.add(scrollPane2, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        entityTable = new JTable();
        scrollPane2.setViewportView(entityTable);
        final JLabel label2 = new JLabel();
        label2.setText("Entities (Read-Only)");
        panel1.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}
