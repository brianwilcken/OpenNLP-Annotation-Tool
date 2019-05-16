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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryViewer extends JFrame {
    private RestTemplate restTemplate;
    private JTable table1;
    private JPanel panel1;
    private Main mainUI;
    private DefaultTableModel tableModel;

    public HistoryViewer(Main mainUI) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);
        this.mainUI = mainUI;

        setTitle("History Viewer");
        setContentPane(panel1);
        if (mainUI != null) {
            setLocation(mainUI.getLocationOnScreen());
        }
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();

        initListeners();
        initTableModel();
    }

    private void initListeners() {
        table1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (table1.getSelectedRow() != -1) {
                    String historyId = table1.getValueAt(table1.getSelectedRow(), 0).toString();
                    loadDocumentHistory(historyId);
                }
            }
        });
    }

    private void initTableModel() {
        tableModel = new DefaultTableModel();
        tableModel.addColumn("ID");
        tableModel.addColumn("User ID");
        tableModel.addColumn("Last Updated");

        table1.setModel(tableModel);

        table1.getColumn("ID").setMaxWidth(0);
        table1.getColumn("ID").setMinWidth(0);
        table1.getColumn("ID").setResizable(false);

        table1.setDefaultEditor(Object.class, null);
        table1.setAutoCreateRowSorter(true);
        table1.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    public void populate(Map<Object, Object> document) {
        try {
            String docId = document.get("id").toString();

            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

            RequestEntity<Void> request = RequestEntity.get(new URI(mainUI.getHostURL() + "/documents/history/" + docId))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            Map<String, Object> jsonDict = response.getBody();

            List<Map<String, Object>> history = ((List<Map<String, Object>>) jsonDict.get("data"));

            for (int r = tableModel.getRowCount() - 1; r >= 0; r--) {
                tableModel.removeRow(r);
            }

            for (Map historyEntry : history) {
                String id = historyEntry.get("id").toString();
                String userId = historyEntry.get("username").toString();
                String lastUpdated = Tools.getFormattedDateTimeString(Instant.ofEpochMilli((long) historyEntry.get("created")));
                tableModel.addRow(new Object[]{id, userId, lastUpdated});
            }
        } catch (URISyntaxException | ResourceAccessException e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
        }
    }

    private void loadDocumentHistory(String historyId) {
        try {
            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

            RequestEntity<Void> request = RequestEntity.get(new URI(mainUI.getHostURL() + "/documents/annotate/history/" + historyId))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            Map<String, Object> jsonDict = response.getBody();

            List<Map<String, Object>> history = ((List<Map<String, Object>>) jsonDict.get("data"));

            String annotated = history.get(0).get("annotated").toString();

            mainUI.updateAnnotation(annotated);
        } catch (URISyntaxException | ResourceAccessException e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
        }
    }

    public static void main(String[] args) {
        HistoryViewer viewer = new HistoryViewer(null);
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
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        table1 = new JTable();
        scrollPane1.setViewportView(table1);
        final JLabel label1 = new JLabel();
        label1.setText("Document Annotation History");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}
