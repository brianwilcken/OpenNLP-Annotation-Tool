package nlpannotator;

import com.google.common.base.Strings;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import common.Tools;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.List;

public class DocumentSelector extends JFrame {
    private RestTemplate restTemplate;
    private JPanel panel1;
    private JList<String> list1;
    private JScrollPane scrollPane1;
    private JTextField textField1;
    private JButton loadURLButton;
    private JButton crawlURLButton;
    private JTable table1;
    private JButton deleteButton;
    private JCheckBox showNotApplicableDocumentsCheckBox;
    private JButton addToProjectButton;
    private JTextField txtProject;
    private JButton filterByProjectButton;
    private JLabel lblNumDocs;
    private List<Map<String, Object>> documents;
    private Main mainUI;
    private FileDrop fileDrop;
    private DefaultTableModel tableModel;
    private boolean clearingTableModel;

    public DocumentSelector(Main mainUI) {
        this.mainUI = mainUI;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);

        initTableModel();
        populate();

        addEventListeners();
        setTitle("Document Selector");
        setLocation(mainUI.getLocationOnScreen());
        setContentPane(panel1);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();

        setVisible(true);
    }

    public static void main(String[] args) {
        DocumentSelector selector = new DocumentSelector(null);
    }

    private void initTableModel() {
        tableModel = new DefaultTableModel();
        tableModel.addColumn("ID");
        tableModel.addColumn("Filename");
        tableModel.addColumn("URL");
        tableModel.addColumn("Category");
        tableModel.addColumn("Project");
        tableModel.addColumn("Last Updated");
        tableModel.addColumn("Updated By");
        tableModel.addColumn("% Annotated");
        tableModel.addColumn("Size (lines)");
        tableModel.addColumn("Use");

        table1.setModel(tableModel);

        table1.getColumn("ID").setMaxWidth(0);
        table1.getColumn("ID").setMinWidth(0);
        table1.getColumn("ID").setResizable(false);
        table1.getColumn("Last Updated").setMaxWidth(200);
        table1.getColumn("Last Updated").setMinWidth(200);
        table1.getColumn("Last Updated").setResizable(false);
        table1.getColumn("Updated By").setMaxWidth(100);
        table1.getColumn("Updated By").setMinWidth(100);
        table1.getColumn("Updated By").setResizable(false);
        table1.getColumn("% Annotated").setMaxWidth(100);
        table1.getColumn("% Annotated").setMinWidth(100);
        table1.getColumn("% Annotated").setResizable(false);
        table1.getColumn("Size (lines)").setMaxWidth(100);
        table1.getColumn("Size (lines)").setMinWidth(100);
        table1.getColumn("Size (lines)").setResizable(false);
        table1.getColumn("Use").setMaxWidth(100);
        table1.getColumn("Use").setMinWidth(100);
        table1.getColumn("Use").setResizable(false);
        table1.setDefaultEditor(Object.class, null);
        table1.setAutoCreateRowSorter(true);
        table1.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    public void populate() {
        try {
            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

            RequestEntity<Void> request = RequestEntity.get(new URI(mainUI.getHostURL() + "/documents?docText=*&fields=id&fields=filename&fields=category&fields=created&fields=lastUpdated&fields=annotatedBy&fields=percentAnnotated&fields=totalLines&fields=url&fields=project&fields=includeInNERTraining&fields=includeInNERTesting"))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            Map<String, Object> jsonDict = response.getBody();

            documents = ((List<Map<String, Object>>) jsonDict.get("data"));

            Collections.sort(documents, Tools.documentComparator);

            //clear out the table model before re-populating it with data
            clearingTableModel = true;
            for (int r = tableModel.getRowCount() - 1; r >= 0; r--) {
                tableModel.removeRow(r);
            }
            clearingTableModel = false;

            int numDocs = 0;
            for (Map doc : documents) {
                String category = doc.containsKey("category") ? doc.get("category").toString() : "";
                if (!showNotApplicableDocumentsCheckBox.isSelected() && category.equals("[Not_Applicable]")) {
                    continue;
                }
                ++numDocs;
                String id = doc.get("id").toString();
                String filename = doc.get("filename").toString();
                String url = doc.containsKey("url") ? doc.get("url").toString() : "";
                String project = doc.containsKey("project") ? doc.get("project").toString() : "UNASSIGNED";
                long created = Long.parseLong(doc.get("created").toString());
                long lastUpdated = Long.parseLong(doc.get("lastUpdated").toString());
                String lastUpdatedStr = Tools.getFormattedDateTimeString(Instant.ofEpochMilli((long) doc.get("lastUpdated")));
                if (created == lastUpdated) {
                    lastUpdatedStr = "NEW: " + lastUpdatedStr;
                }
                String annotatedBy = "";
                if (doc.containsKey("annotatedBy")) {
                    annotatedBy = ((List) doc.get("annotatedBy")).get(0).toString();
                }
                String percentAnnotated = "0%";
                if (doc.containsKey("percentAnnotated")) {
                    percentAnnotated = Long.parseLong(doc.get("percentAnnotated").toString()) + "%";
                }
                String totalLines = "";
                if (doc.containsKey("totalLines")) {
                    totalLines = doc.get("totalLines").toString();
                }
                String use = "";
                if (doc.containsKey("includeInNERTraining")) {
                    boolean useForTraining = Boolean.parseBoolean(doc.get("includeInNERTraining").toString());
                    if (useForTraining) {
                        use = "train";
                    }
                }
                if (doc.containsKey("includeInNERTesting")) {
                    boolean useForTesting = Boolean.parseBoolean(doc.get("includeInNERTesting").toString());
                    if (useForTesting) {
                        use = "test";
                    }
                }

                tableModel.addRow(new Object[]{id, filename, url, category, project, lastUpdatedStr, annotatedBy, percentAnnotated, totalLines, use});
            }

            lblNumDocs.setText(Integer.toString(numDocs));

            scrollPane1.setPreferredSize(new Dimension(400, 200));
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
        } catch (ResourceAccessException e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
        }
    }

    private void addEventListeners() {
        table1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                listSelectActionListener(event);
            }
        });

        fileDrop = new FileDrop(table1, new FileDrop.Listener() {
            @Override
            public void filesDropped(File[] files) {
                uploadFiles(files);
            }
        });

        loadURLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                loadURLActionListener(actionEvent);
            }
        });

        crawlURLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                crawlURLActionListener(actionEvent);
            }
        });

        addToProjectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                addToProject();
            }
        });

        filterByProjectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                filterByProject();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteActionListener(actionEvent);
            }
        });

        showNotApplicableDocumentsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                populate();
            }
        });
    }

    private void uploadFiles(File[] files) {
        ProcessMonitor procMon = mainUI.getProcessMonitor();
        procMon.setVisible(true);
        String procId = procMon.addProcess("(" + Instant.now() + ") Uploading Files");
        new Thread(() -> {
            try {
                for (int i = 0; i < files.length; i++) {
                    Map<String, String> metadata = new HashMap<>();
                    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                    body.add("metadata", metadata);
                    body.add("file", new FileSystemResource(files[i]));
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                    ParameterizedTypeReference<HashMap<String, Object>> responseType =
                            new ParameterizedTypeReference<HashMap<String, Object>>() {
                            };

                    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

                    ResponseEntity<HashMap<String, Object>> response = null;
                    try {
                        response = restTemplate.exchange(mainUI.getHostURL() + "/documents", HttpMethod.POST, request, responseType);
                    } catch (HttpClientErrorException | HttpServerErrorException e) {
                        JOptionPane.showMessageDialog(mainUI, "Failed to upload file: (" + files[i].getName() + ") Reason: " + e.getResponseBodyAsString());
                    }

                    if (response.getStatusCode() != HttpStatus.OK) {
                        JOptionPane.showMessageDialog(mainUI, "Failed to upload file: (" + files[i].getName() + ") Reason: " + response.getBody().get("data").toString());
                    }
                }
            } catch (ResourceAccessException e) {
                JOptionPane.showMessageDialog(mainUI, e.getMessage());
            } finally {
                procMon.removeProcess(procId);
            }
            mainUI.openDocument();
        }).start();
    }

    public void loadURLActionListener(ActionEvent evt) {
        textField1.setBackground(new Color(Color.WHITE.getRGB()));
        String urlString = textField1.getText();
        ProcessMonitor procMon = mainUI.getProcessMonitor();
        procMon.setVisible(true);
        String procId = procMon.addProcess("(" + Instant.now() + ") Loading URL: " + urlString);
        new Thread(() -> {
            try {
                URL url = new URL(urlString);

                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("url", url.toString());

                RequestEntity<MultiValueMap<String, Object>> request = RequestEntity.post(new URI(mainUI.getHostURL() + "/documents/url"))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(body);

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);
                mainUI.openDocument();
            } catch (MalformedURLException e) {
                textField1.setBackground(new Color(Color.RED.getRGB()));
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                JOptionPane.showMessageDialog(mainUI, e.getResponseBodyAsString());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainUI, e.getMessage());
            } finally {
                procMon.removeProcess(procId);
            }
        }).start();
    }

    public void crawlURLActionListener(ActionEvent evt) {
        try {
            if (JOptionPane.showConfirmDialog(this, "This could take a very long time.  Are you sure?") == 0) {
                textField1.setBackground(new Color(Color.WHITE.getRGB()));
                String urlString = textField1.getText();
                URL url = new URL(urlString);

                CrawlURLTask crawlURLTask = new CrawlURLTask(url.toString());
                Thread thread = new Thread(crawlURLTask);
                thread.start();
            }

        } catch (MalformedURLException e) {
            textField1.setBackground(new Color(Color.RED.getRGB()));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
        }
    }

    public void filterByProject() {
        String project = txtProject.getText().toLowerCase();
        populate();
        int numDocs = Integer.parseInt(lblNumDocs.getText());
        if (!Strings.isNullOrEmpty(project)) {
            for (int r = tableModel.getRowCount() - 1; r >= 0; r--) {
                String proj = tableModel.getValueAt(r, 4).toString().toLowerCase();
                if (!proj.equals(project)) {
                    tableModel.removeRow(r);
                    --numDocs;
                }
            }
        }
        lblNumDocs.setText(Integer.toString(numDocs));
    }

    public void addToProject() {
        try {
            String project = txtProject.getText();
            if (Strings.isNullOrEmpty(project)) {
                JOptionPane.showMessageDialog(this, "Enter a project name...");
                return;
            }
            if (table1.getSelectedRow() != -1) {
                int[] rows = table1.getSelectedRows();
                for (int r = 0; r < rows.length; r++) {
                    String id = table1.getValueAt(rows[r], 0).toString();
                    ParameterizedTypeReference<HashMap<String, Object>> responseType =
                            new ParameterizedTypeReference<HashMap<String, Object>>() {
                            };

                    Map<String, Object> doc = new HashMap<>();
                    doc.put("project", project);

                    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                    body.add("metadata", doc);
                    body.add("doNLP", false);

                    RequestEntity<Map> request = RequestEntity.put(new URI(mainUI.getHostURL() + "/documents/metadata/" + id))
                            .accept(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                            .body(body);

                    ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                    if (response.getStatusCode() != HttpStatus.OK) {
                        JOptionPane.showMessageDialog(this, "Failed to add document to project! Reason: " + response.getStatusCode());
                    }
                }
                filterByProject();
            } else {
                JOptionPane.showMessageDialog(this, "Select a document...");
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            JOptionPane.showMessageDialog(mainUI, e.getResponseBodyAsString());
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
        }
    }

    public void deleteActionListener(ActionEvent evt) {
        try {
            if (table1.getSelectedRow() != -1 && JOptionPane.showConfirmDialog(this, "Are you sure (this action cannot be undone)?") == 0) {
                int[] rows = table1.getSelectedRows();
                for (int r = 0; r < rows.length; r++) {
                    String id = table1.getValueAt(rows[r], 0).toString();
                    ParameterizedTypeReference<HashMap<String, Object>> responseType =
                            new ParameterizedTypeReference<HashMap<String, Object>>() {
                            };

                    RequestEntity<Void> request = RequestEntity.delete(new URI(mainUI.getHostURL() + "/documents/" + id))
                            .accept(MediaType.APPLICATION_JSON).build();

                    ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                    if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                        mainUI.clearIfDeleted(id);
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to delete document! Reason: " + response.getStatusCode());
                    }
                }
                populate();
            } else {
                JOptionPane.showMessageDialog(this, "Select a document...");
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            JOptionPane.showMessageDialog(mainUI, e.getResponseBodyAsString());
        } catch (URISyntaxException e) {
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
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 10, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(30);
        panel1.add(scrollPane1, new GridConstraints(2, 0, 1, 10, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(1200, 600), new Dimension(1200, 600), null, 0, false));
        table1 = new JTable();
        scrollPane1.setViewportView(table1);
        final JLabel label1 = new JLabel();
        label1.setText("Resource URL:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField1 = new JTextField();
        textField1.setText("");
        panel1.add(textField1, new GridConstraints(0, 1, 1, 6, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        loadURLButton = new JButton();
        loadURLButton.setText("Load URL");
        panel1.add(loadURLButton, new GridConstraints(0, 7, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("OR Select a Document From the List Below:");
        panel1.add(label2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        crawlURLButton = new JButton();
        crawlURLButton.setText("Crawl URL");
        panel1.add(crawlURLButton, new GridConstraints(0, 9, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showNotApplicableDocumentsCheckBox = new JCheckBox();
        showNotApplicableDocumentsCheckBox.setText("Show Not Applicable Documents");
        panel1.add(showNotApplicableDocumentsCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtProject = new JTextField();
        panel1.add(txtProject, new GridConstraints(1, 5, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(1, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Project:");
        panel1.add(label3, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setText("Delete Selected Document(s)");
        panel1.add(deleteButton, new GridConstraints(3, 9, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addToProjectButton = new JButton();
        addToProjectButton.setText("Add to Project");
        panel1.add(addToProjectButton, new GridConstraints(1, 9, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        filterByProjectButton = new JButton();
        filterByProjectButton.setText("Filter By Project");
        panel1.add(filterByProjectButton, new GridConstraints(1, 7, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JToolBar.Separator toolBar$Separator1 = new JToolBar.Separator();
        panel1.add(toolBar$Separator1, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Number Documents Displayed:");
        panel1.add(label4, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblNumDocs = new JLabel();
        lblNumDocs.setText("0");
        panel1.add(lblNumDocs, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    private class CrawlURLTask implements Runnable {

        private String url;

        public CrawlURLTask(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            ProcessMonitor procMon = mainUI.getProcessMonitor();
            procMon.setVisible(true);
            String procId = procMon.addProcess("(" + Instant.now() + ") Crawling URL: " + url);
            try {
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("url", url);

                RequestEntity<MultiValueMap<String, Object>> request = RequestEntity.post(new URI(mainUI.getHostURL() + "/documents/crawl"))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(body);

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);
            } catch (URISyntaxException e) {
                JOptionPane.showMessageDialog(mainUI, e.getMessage());
            } finally {
                procMon.removeProcess(procId);
            }
        }
    }

    public void listSelectActionListener(ListSelectionEvent listSelectionEvent) {
        try {
            if (listSelectionEvent.getValueIsAdjusting() && table1.getSelectedRow() != -1 && !clearingTableModel) {
                String id = table1.getValueAt(table1.getSelectedRow(), 0).toString();

                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                RequestEntity<Void> request = RequestEntity.get(new URI(mainUI.getHostURL() + "/documents?id=" + id))
                        .accept(MediaType.APPLICATION_JSON).build();

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                Map<String, Object> jsonDict = response.getBody();

                List<Map<String, Object>> document = ((List<Map<String, Object>>) jsonDict.get("data"));

                if (document.size() > 0) {
                    mainUI.loadDocument(document.get(0));
                    mainUI.reloadHistory();
                }
            }
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
        }
    }

}
