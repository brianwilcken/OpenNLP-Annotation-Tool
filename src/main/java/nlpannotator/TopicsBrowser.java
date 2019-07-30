package nlpannotator;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopicsBrowser extends JFrame {
    private JPanel pnlTopics;
    private JTable tblTopics;
    private JSlider sldrBlockSize;
    private JButton refreshButton;
    private Main mainUI;

    private DefaultTableModel tblTopicsModel;
    private RestTemplate restTemplate;

    public TopicsBrowser(Main mainUI) {
        this.mainUI = mainUI;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);

        setTitle("Topics Browser");
        setContentPane(pnlTopics);
        if (mainUI != null) {
            setLocation(mainUI.getLocationOnScreen());
        }
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();

        addEventListeners();
        buildTopicsTable();
    }

    private void addEventListeners() {
        tblTopics.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (tblTopics.getSelectedRow() != -1) {
                    int start = Integer.parseInt(tblTopics.getValueAt(tblTopics.getSelectedRow(), 0).toString());
                    int end = Integer.parseInt(tblTopics.getValueAt(tblTopics.getSelectedRow(), 1).toString());
                    mainUI.navigateToLine(start, end);
                }
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                populate(mainUI.document);
            }
        });
    }

    private void buildTopicsTable() {
        tblTopicsModel = new DefaultTableModel();
        tblTopicsModel.addColumn("start");
        tblTopicsModel.addColumn("end");
        tblTopicsModel.addColumn("Line #'s");
        tblTopicsModel.addColumn("Topics");

        tblTopics.setModel(tblTopicsModel);
        tblTopics.getColumn("start").setMaxWidth(0);
        tblTopics.getColumn("start").setMinWidth(0);
        tblTopics.getColumn("start").setResizable(false);
        tblTopics.getColumn("end").setMaxWidth(0);
        tblTopics.getColumn("end").setMinWidth(0);
        tblTopics.getColumn("end").setResizable(false);
        tblTopics.getColumnModel().getColumn(3).setCellRenderer(new WordWrapCellRenderer());
        tblTopics.setDefaultEditor(Object.class, null);
        tblTopics.setAutoCreateRowSorter(false);
        tblTopics.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    public void populate(Map document) {
        try {
            if (document != null) {
                String docId = document.get("id").toString();
                ParameterizedTypeReference<HashMap<String, Object>> responseType = new ParameterizedTypeReference<HashMap<String, Object>>() {
                };

                int paragraphSize = sldrBlockSize.getValue();
                RequestEntity<Void> request = RequestEntity.get(new URI(mainUI.getHostURL() + "/documents/topics/" + docId + "?paragraphSize=" + paragraphSize)).accept(MediaType.APPLICATION_JSON).build();

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                Map<String, Object> jsonDict = response.getBody();
                List<Map<String, Object>> topics = (List<Map<String, Object>>) jsonDict.get("data");

                for (int r = tblTopicsModel.getRowCount() - 1; r >= 0; r--) {
                    tblTopicsModel.removeRow(r);
                }

                for (Map<String, Object> topic : topics) {
                    int startLine = Integer.parseInt(topic.get("startLine").toString());
                    int endLine = Integer.parseInt(topic.get("endLine").toString());
                    String lineNums = startLine + " TO " + endLine;
                    List<String> ldaCats = (List<String>) topic.get("ldaCategory");
                    List<String> formattedCats = ldaCats.stream()
                            .map(p -> !p.contains("---") ? String.format("%-40s%s", p.split(" ")[0], String.format("%.2f", Double.parseDouble(p.split(" ")[1]) * 100) + "%") : p)
                            .collect(Collectors.toList());

                    String topicsText = String.join(System.lineSeparator(), formattedCats);

                    tblTopicsModel.addRow(new Object[]{startLine, endLine, lineNums, topicsText});
                }
            }
        } catch (URISyntaxException | ResourceAccessException e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
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
        pnlTopics = new JPanel();
        pnlTopics.setLayout(new GridLayoutManager(2, 3, new Insets(5, 5, 5, 5), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        pnlTopics.add(scrollPane1, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, -1), new Dimension(600, -1), null, 0, false));
        tblTopics = new JTable();
        scrollPane1.setViewportView(tblTopics);
        sldrBlockSize = new JSlider();
        sldrBlockSize.setMajorTickSpacing(10);
        sldrBlockSize.setMinimum(0);
        sldrBlockSize.setMinorTickSpacing(1);
        sldrBlockSize.setPaintLabels(true);
        sldrBlockSize.setPaintTicks(true);
        sldrBlockSize.setSnapToTicks(true);
        sldrBlockSize.setValue(10);
        pnlTopics.add(sldrBlockSize, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Topic Resolution (# of Lines per Block)");
        pnlTopics.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        refreshButton = new JButton();
        refreshButton.setText("Refresh");
        pnlTopics.add(refreshButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return pnlTopics;
    }
}
