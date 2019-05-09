package nlpannotator;

import com.google.common.base.Strings;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import common.CrawlGoogleManager;
import common.FacilityTypes;
import common.Tools;
import org.apache.commons.io.FileUtils;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
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
    private JTable tblDocuments;
    private JButton deleteButton;
    private JCheckBox showNotApplicableDocumentsCheckBox;
    private JButton updateProjectButton;
    private JTextField txtProject;
    private JButton filterButton;
    private JLabel lblNumDocs;
    private JTabbedPane tabbedPane1;
    private JTextField srchFilename;
    private JTextField srchURL;
    private JComboBox srchCategory;
    private JTextField srchProject;
    private JTextField srchDocText;
    private JComboBox ddlUpdateCategory;
    private JButton updateCategoryButton;
    private JComboBox ddlUpdateUse;
    private JButton updateUseButton;
    private JTextField txtOrganization;
    private JButton updateOrganizationButton;
    private JTextField srchOrganization;
    private JTextField txtGoogleSearchTerm;
    private JButton crawlGoogleButton;
    private JTable tblCrawlSchedule;
    private JButton loadGoogleCrawlScheduleButton;
    private JButton clearCrawlScheduleButton;
    private List<Map<String, Object>> documents;
    private Main mainUI;
    private FileDrop fileDrop;
    private DefaultTableModel tblDocumentsModel;
    private DefaultTableModel tblCrawlScheduleModel;
    private boolean clearingTableModel;
    private CrawlDepth crawlDepth;
    private CrawlGoogleManager mgr;
    private List<CrawlGoogleTask> crawlGoogleTasks;

    public DocumentSelector(Main mainUI) {
        this.mainUI = mainUI;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);
        mgr = new CrawlGoogleManager();
        crawlGoogleTasks = new ArrayList<>();

        populateCategorySearch();
        populateUpdateCategory();
        populateUpdateUse();
        initDocumentTableModel();
        initCrawlStatusTableModel();
        populate();

        addEventListeners();
        setTitle("Document Selector");
        setLocation(mainUI.getLocationOnScreen());
        setContentPane(panel1);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();

        setVisible(true);

        crawlDepth = new CrawlDepth(this);
    }

    public static void main(String[] args) {
        DocumentSelector selector = new DocumentSelector(null);
    }

    private void initDocumentTableModel() {
        tblDocumentsModel = new DefaultTableModel();
        tblDocumentsModel.addColumn("ID");
        tblDocumentsModel.addColumn("Filename");
        tblDocumentsModel.addColumn("URL");
        tblDocumentsModel.addColumn("Category");
        tblDocumentsModel.addColumn("Project");
        tblDocumentsModel.addColumn("Organization");
        tblDocumentsModel.addColumn("Last Updated");
        tblDocumentsModel.addColumn("Updated By");
        tblDocumentsModel.addColumn("% Annotated");
        tblDocumentsModel.addColumn("Size (lines)");
        tblDocumentsModel.addColumn("Use");

        tblDocuments.setModel(tblDocumentsModel);

        tblDocuments.getColumn("ID").setMaxWidth(0);
        tblDocuments.getColumn("ID").setMinWidth(0);
        tblDocuments.getColumn("ID").setResizable(false);
        tblDocuments.getColumn("Last Updated").setMaxWidth(200);
        tblDocuments.getColumn("Last Updated").setMinWidth(200);
        tblDocuments.getColumn("Last Updated").setResizable(false);
        tblDocuments.getColumn("Updated By").setMaxWidth(100);
        tblDocuments.getColumn("Updated By").setMinWidth(100);
        tblDocuments.getColumn("Updated By").setResizable(false);
        tblDocuments.getColumn("% Annotated").setMaxWidth(100);
        tblDocuments.getColumn("% Annotated").setMinWidth(100);
        tblDocuments.getColumn("% Annotated").setResizable(false);
        tblDocuments.getColumn("Size (lines)").setMaxWidth(100);
        tblDocuments.getColumn("Size (lines)").setMinWidth(100);
        tblDocuments.getColumn("Size (lines)").setResizable(false);
        tblDocuments.getColumn("Use").setMaxWidth(100);
        tblDocuments.getColumn("Use").setMinWidth(100);
        tblDocuments.getColumn("Use").setResizable(false);
        tblDocuments.setDefaultEditor(Object.class, null);
        tblDocuments.setAutoCreateRowSorter(true);
        tblDocuments.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    private void initCrawlStatusTableModel() {
        tblCrawlScheduleModel = new DefaultTableModel();
        tblCrawlScheduleModel.addColumn("Search Term");
        tblCrawlScheduleModel.addColumn("Crawl Status");

        tblCrawlSchedule.setModel(tblCrawlScheduleModel);

        tblCrawlSchedule.setDefaultEditor(Object.class, null);
        tblCrawlSchedule.setAutoCreateRowSorter(true);
        tblCrawlSchedule.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    private void populateCategorySearch() {
        DefaultComboBoxModel model = new DefaultComboBoxModel();

        model.addElement("-- Exclude From Search --");
        model.addElement("Not_Applicable");
        for (String key : FacilityTypes.dictionary.keySet()) {
            model.addElement(key);
        }

        srchCategory.setModel(model);
    }

    private void populateUpdateCategory() {
        DefaultComboBoxModel model = new DefaultComboBoxModel();

        model.addElement("Not_Applicable");
        for (String key : FacilityTypes.dictionary.keySet()) {
            model.addElement(key);
        }

        ddlUpdateCategory.setModel(model);
    }

    private void populateUpdateUse() {
        DefaultComboBoxModel model = new DefaultComboBoxModel();

        model.addElement("None");
        model.addElement("Train");
        model.addElement("Test");

        ddlUpdateUse.setModel(model);
    }

    public void populate() {
        try {
            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

            String docText = srchDocText.getText();
            if (Strings.isNullOrEmpty(docText)) {
                docText = "*";
            } else {
                try {
                    docText = URLEncoder.encode("\"" + docText + "\"", "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    docText = "*";
                }
            }

            RequestEntity<Void> request = RequestEntity.get(new URI(mainUI.getHostURL() + "/documents?docText=" + docText + "&fields=id&fields=filename&fields=category&fields=created&fields=lastUpdated&fields=annotatedBy&fields=percentAnnotated&fields=totalLines&fields=url&fields=project&fields=organization&fields=includeInNERTraining&fields=includeInNERTesting"))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            Map<String, Object> jsonDict = response.getBody();

            documents = ((List<Map<String, Object>>) jsonDict.get("data"));

            Collections.sort(documents, Tools.documentComparator);

            //clear out the table model before re-populating it with data
            clearingTableModel = true;
            for (int r = tblDocumentsModel.getRowCount() - 1; r >= 0; r--) {
                tblDocumentsModel.removeRow(r);
            }
            clearingTableModel = false;

            int numDocs = 0;
            for (Map doc : documents) {
                String category = doc.containsKey("category") ? doc.get("category").toString() : "";
                ++numDocs;
                String id = doc.get("id").toString();
                String filename = doc.get("filename").toString();
                String url = doc.containsKey("url") ? doc.get("url").toString() : "";
                String project = doc.containsKey("project") ? doc.get("project").toString() : "";
                String organization = doc.containsKey("organization") ? doc.get("organization").toString() : "";
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

                tblDocumentsModel.addRow(new Object[]{id, filename, url, category, project, organization, lastUpdatedStr, annotatedBy, percentAnnotated, totalLines, use});
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
        tblDocuments.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                listSelectActionListener(event);
            }
        });

        fileDrop = new FileDrop(tblDocuments, new FileDrop.Listener() {
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

        txtGoogleSearchTerm.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String searchTerm = txtGoogleSearchTerm.getText();
                    txtGoogleSearchTerm.setText("");
                    tblCrawlScheduleModel.addRow(new Object[]{searchTerm, "Pending"});
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        });

        loadGoogleCrawlScheduleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                loadGoogleCrawlSchedule();
            }
        });

        clearCrawlScheduleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                clearGoogleCrawlSchedule();
            }
        });

        crawlGoogleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                crawlGoogle();
            }
        });

        updateProjectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateProject();
            }
        });

        updateOrganizationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateOrganization();
            }
        });

        updateCategoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateCategory();
            }
        });

        updateUseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateUse();
            }
        });

        filterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                filter();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteActionListener(actionEvent);
            }
        });
    }

    private void loadGoogleCrawlSchedule() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        chooser.setDialogTitle("Choose File");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String schedule = FileUtils.readFileToString(file);
                String[] terms = schedule.split(System.lineSeparator());

                //clear out all previous search terms
                clearGoogleCrawlScheduleTable();

                for (String term : terms) {
                    if (!term.startsWith("#")) {
                        tblCrawlScheduleModel.addRow(new Object[]{term, "Pending"});
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
        }
    }

    private void clearGoogleCrawlScheduleTable() {
        for (int r = tblCrawlScheduleModel.getRowCount() - 1; r >= 0; r--) {
            tblCrawlScheduleModel.removeRow(r);
        }
    }

    private void clearGoogleCrawlSchedule() {
        if (crawlGoogleTasks.stream().anyMatch(p -> p.isActive())) {
            int input = JOptionPane.showConfirmDialog(this, "Active crawl tasks are underway.  Are you sure you want to cancel?", "Confirm Cancel Crawl Tasks", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (input == 0) { //yes
                for (CrawlGoogleTask task : crawlGoogleTasks) {
                    task.kill();
                }
                clearGoogleCrawlScheduleTable();
                crawlGoogleTasks.clear();
            }
        } else {
            clearGoogleCrawlScheduleTable();
        }
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
        crawlDepth.setVisible(true);
    }

    public void startCrawling(int depth) {
        try {
            textField1.setBackground(new Color(Color.WHITE.getRGB()));
            String urlString = textField1.getText();
            URL url = new URL(urlString);

            CrawlURLTask crawlURLTask = new CrawlURLTask(url.toString(), depth);
            Thread thread = new Thread(crawlURLTask);
            thread.start();
        } catch (MalformedURLException e) {
            textField1.setBackground(new Color(Color.RED.getRGB()));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
        }
    }

    public void crawlGoogle() {
        try {
            crawlGoogleTasks.clear();
            for (int r = 0; r < tblCrawlScheduleModel.getRowCount(); r++) {
                String searchTerm = tblCrawlScheduleModel.getValueAt(r, 0).toString();
                CrawlGoogleTask crawlGoogleTask = new CrawlGoogleTask(searchTerm);
                crawlGoogleTask.setMyThread(mgr.startProcess(crawlGoogleTask));
                crawlGoogleTasks.add(crawlGoogleTask);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
        }
    }

    public void filter() {
        String filenameSrch = srchFilename.getText().toLowerCase();
        String urlSrch = srchURL.getText().toLowerCase();
        String categorySrch = srchCategory.getSelectedItem().toString().toLowerCase();
        String projectSrch = srchProject.getText().toLowerCase();
        String organizationSrch = srchOrganization.getText().toLowerCase();

        populate();
        int numDocs = Integer.parseInt(lblNumDocs.getText());
        for (int r = tblDocumentsModel.getRowCount() - 1; r >= 0; r--) {
            String filename = tblDocumentsModel.getValueAt(r, 1).toString().toLowerCase();
            String url = tblDocumentsModel.getValueAt(r, 2).toString().toLowerCase();
            String category = tblDocumentsModel.getValueAt(r, 3).toString().toLowerCase();
            String project = tblDocumentsModel.getValueAt(r, 4).toString().toLowerCase();
            String organization = tblDocumentsModel.getValueAt(r, 4).toString().toLowerCase();

            boolean includeRow = Strings.isNullOrEmpty(filenameSrch) || Tools.similarity(filenameSrch, filename) > 0.6 || filename.contains(filenameSrch);
            includeRow = includeRow && (Strings.isNullOrEmpty(urlSrch) || Tools.similarity(urlSrch, url) > 0.6 || url.contains(urlSrch));
            includeRow = includeRow && (srchCategory.getSelectedIndex() == 0 || category.contains(categorySrch));
            includeRow = includeRow && (Strings.isNullOrEmpty(projectSrch) || Tools.similarity(projectSrch, project) > 0.6);
            includeRow = includeRow && (Strings.isNullOrEmpty(organizationSrch) || Tools.similarity(organizationSrch, organization) > 0.6);
            if (!includeRow) {
                tblDocumentsModel.removeRow(r);
                --numDocs;
            }
        }
        lblNumDocs.setText(Integer.toString(numDocs));
    }

    private void updateProject() {
        if (tblDocuments.getSelectedRow() != -1) {
            String project = txtProject.getText();
            if (Strings.isNullOrEmpty(project)) {
                JOptionPane.showMessageDialog(this, "Enter a project name...");
                return;
            }
            updateAttribute("project", project);
            filter();
        } else {
            JOptionPane.showMessageDialog(this, "Select a document...");
        }
    }

    private void updateOrganization() {
        if (tblDocuments.getSelectedRow() != -1) {
            String organization = txtOrganization.getText();
            if (Strings.isNullOrEmpty(organization)) {
                JOptionPane.showMessageDialog(this, "Enter an organization name...");
                return;
            }
            updateAttribute("organization", organization);
            filter();
        } else {
            JOptionPane.showMessageDialog(this, "Select a document...");
        }
    }

    private void updateCategory() {
        if (tblDocuments.getSelectedRow() != -1) {
            List categories = new ArrayList();
            categories.add(ddlUpdateCategory.getSelectedItem());
            updateAttribute("category", categories);
            filter();
        } else {
            JOptionPane.showMessageDialog(this, "Select a document...");
        }
    }

    private void updateUse() {
        if (tblDocuments.getSelectedRow() != -1) {
            if (ddlUpdateUse.getSelectedIndex() == 0) {
                updateAttribute("includeInNERTraining", false);
                updateAttribute("includeInNERTesting", false);
            } else if (ddlUpdateUse.getSelectedItem().equals("Train")) {
                updateAttribute("includeInNERTraining", true);
                updateAttribute("includeInNERTesting", false);
            } else if (ddlUpdateUse.getSelectedItem().equals("Test")) {
                updateAttribute("includeInNERTraining", false);
                updateAttribute("includeInNERTesting", true);
            }
            filter();
        } else {
            JOptionPane.showMessageDialog(this, "Select a document...");
        }
    }

    private void updateAttribute(String attrName, Object attr) {
        try {
            int[] rows = tblDocuments.getSelectedRows();
            for (int r = 0; r < rows.length; r++) {
                String id = tblDocuments.getValueAt(rows[r], 0).toString();
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                Map<String, Object> doc = new HashMap<>();
                doc.put(attrName, attr);

                String annotatedBy = System.getProperty("user.name");
                doc.put("annotatedBy", annotatedBy);

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("metadata", doc);
                body.add("doNLP", false);

                RequestEntity<Map> request = RequestEntity.put(new URI(mainUI.getHostURL() + "/documents/metadata/" + id))
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                        .body(body);

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                if (response.getStatusCode() != HttpStatus.OK) {
                    JOptionPane.showMessageDialog(this, "Failed to update document! Reason: " + response.getStatusCode());
                }
            }
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            JOptionPane.showMessageDialog(mainUI, e.getResponseBodyAsString());
        }
    }

    public void deleteActionListener(ActionEvent evt) {
        try {
            if (tblDocuments.getSelectedRow() != -1 && JOptionPane.showConfirmDialog(this, "Are you sure (this action cannot be undone)?") == 0) {
                int[] rows = tblDocuments.getSelectedRows();
                for (int r = 0; r < rows.length; r++) {
                    String id = tblDocuments.getValueAt(rows[r], 0).toString();
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
        panel1.setLayout(new GridLayoutManager(4, 11, new Insets(5, 5, 5, 5), -1, -1));
        scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(30);
        panel1.add(scrollPane1, new GridConstraints(2, 0, 1, 11, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(1200, 600), new Dimension(1200, 600), null, 0, false));
        tblDocuments = new JTable();
        scrollPane1.setViewportView(tblDocuments);
        final JLabel label1 = new JLabel();
        label1.setText("Select a Document From the List Below:");
        panel1.add(label1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(1, 3, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setText("Delete Selected Document(s)");
        panel1.add(deleteButton, new GridConstraints(3, 10, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tabbedPane1 = new JTabbedPane();
        panel1.add(tabbedPane1, new GridConstraints(0, 0, 1, 11, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(7, 3, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("Search Filters", panel2);
        srchFilename = new JTextField();
        panel2.add(srchFilename, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Filename");
        panel2.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("URL");
        panel2.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        srchURL = new JTextField();
        panel2.add(srchURL, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Category");
        panel2.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        srchCategory = new JComboBox();
        panel2.add(srchCategory, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Project");
        panel2.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        filterButton = new JButton();
        filterButton.setText("Filter");
        panel2.add(filterButton, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        srchProject = new JTextField();
        panel2.add(srchProject, new GridConstraints(4, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Text");
        panel2.add(label6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        srchDocText = new JTextField();
        panel2.add(srchDocText, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Organization");
        panel2.add(label7, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        srchOrganization = new JTextField();
        panel2.add(srchOrganization, new GridConstraints(5, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 4, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("Load URL", panel3);
        final JLabel label8 = new JLabel();
        label8.setText("URL:");
        panel3.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField1 = new JTextField();
        textField1.setText("");
        panel3.add(textField1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        loadURLButton = new JButton();
        loadURLButton.setText("Load URL");
        panel3.add(loadURLButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        crawlURLButton = new JButton();
        crawlURLButton.setText("Crawl URL");
        panel3.add(crawlURLButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Google Crawler", panel4);
        txtGoogleSearchTerm = new JTextField();
        panel4.add(txtGoogleSearchTerm, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Enter Search Term:");
        panel4.add(label9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        loadGoogleCrawlScheduleButton = new JButton();
        loadGoogleCrawlScheduleButton.setText("Load Google Crawl Schedule");
        panel5.add(loadGoogleCrawlScheduleButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        crawlGoogleButton = new JButton();
        crawlGoogleButton.setText("Crawl Google");
        panel5.add(crawlGoogleButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel5.add(scrollPane2, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(1200, 200), new Dimension(1200, 200), null, 0, false));
        tblCrawlSchedule = new JTable();
        scrollPane2.setViewportView(tblCrawlSchedule);
        clearCrawlScheduleButton = new JButton();
        clearCrawlScheduleButton.setText("Clear Crawl Schedule");
        panel5.add(clearCrawlScheduleButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(4, 3, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("Change Attributes", panel6);
        final JLabel label10 = new JLabel();
        label10.setText("Category");
        panel6.add(label10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ddlUpdateCategory = new JComboBox();
        panel6.add(ddlUpdateCategory, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        updateCategoryButton = new JButton();
        updateCategoryButton.setText("Update Category");
        panel6.add(updateCategoryButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("Project");
        panel6.add(label11, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtProject = new JTextField();
        panel6.add(txtProject, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        updateProjectButton = new JButton();
        updateProjectButton.setText("Update Project");
        panel6.add(updateProjectButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("Use");
        panel6.add(label12, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ddlUpdateUse = new JComboBox();
        panel6.add(ddlUpdateUse, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        updateUseButton = new JButton();
        updateUseButton.setText("Update Use");
        panel6.add(updateUseButton, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("Organization");
        panel6.add(label13, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtOrganization = new JTextField();
        panel6.add(txtOrganization, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        updateOrganizationButton = new JButton();
        updateOrganizationButton.setText("Update Organization");
        panel6.add(updateOrganizationButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setText("Number Documents Displayed:");
        panel1.add(label14, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblNumDocs = new JLabel();
        lblNumDocs.setText("0");
        panel1.add(lblNumDocs, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    private class CrawlURLTask implements Runnable {

        private String url;
        private String depth;

        public CrawlURLTask(String url, int depth) {
            this.url = url;
            this.depth = Integer.toString(depth);
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
                body.add("depth", depth);

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

    private void updateGoogleCrawlSchedule(String searchTerm, String status) {
        for (int r = 0; r < tblCrawlScheduleModel.getRowCount(); r++) {
            String otherSearchTerm = tblCrawlScheduleModel.getValueAt(r, 0).toString();
            if (searchTerm.equals(otherSearchTerm)) {
                tblCrawlScheduleModel.setValueAt(status, r, 1);
            }
        }
    }

    private class CrawlGoogleTask implements Runnable {

        private String searchTerm;
        private Thread myThread;
        private boolean active;
        private String procId;
        private ProcessMonitor procMon;

        public CrawlGoogleTask(String searchTerm) {
            this.searchTerm = searchTerm;
            procMon = mainUI.getProcessMonitor();
        }

        @Override
        public void run() {
            //update crawl schedule
            updateGoogleCrawlSchedule(searchTerm, "In Progress");
            active = true;
            //update process monitor
            procMon.setVisible(true);
            procId = procMon.addProcess("(" + Instant.now() + ") Crawling Google: " + searchTerm);
            try {
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("searchTerm", searchTerm);

                RequestEntity<MultiValueMap<String, Object>> request = RequestEntity.post(new URI(mainUI.getHostURL() + "/documents/crawlGoogle"))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(body);

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);
            } catch (URISyntaxException e) {
                JOptionPane.showMessageDialog(mainUI, e.getMessage());
                updateGoogleCrawlSchedule(searchTerm, "Failed");
            } finally {
                procMon.removeProcess(procId);
                updateGoogleCrawlSchedule(searchTerm, "Done");
                active = false;
            }
        }

        public void kill() {
            myThread.interrupt();
            procMon.removeProcess(procId);
        }

        public Thread getMyThread() {
            return myThread;
        }

        public void setMyThread(Thread myThread) {
            this.myThread = myThread;
        }

        public boolean isActive() {
            return active;
        }
    }

    public void listSelectActionListener(ListSelectionEvent listSelectionEvent) {
        try {
            if (listSelectionEvent.getValueIsAdjusting() && tblDocuments.getSelectedRow() != -1 && !clearingTableModel) {
                String id = tblDocuments.getValueAt(tblDocuments.getSelectedRow(), 0).toString();

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
                    mainUI.refreshMetadataEditor();
                }
            }
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(mainUI, e.getMessage());
        }
    }

}
