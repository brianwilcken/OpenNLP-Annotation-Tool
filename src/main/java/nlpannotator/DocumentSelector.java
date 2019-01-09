package nlpannotator;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import common.Tools;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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

public class DocumentSelector extends JFrame implements ListSelectionListener {
    private RestTemplate restTemplate;
    private JPanel panel1;
    private JList<String> list1;
    private JScrollPane scrollPane1;
    private JTextField textField1;
    private JButton loadURLButton;
    private JButton crawlURLButton;
    private List<Map<String, Object>> documents;
    private Main annotatorUI;
    private FileDrop fileDrop;

    public DocumentSelector(Main annotatorUI) {
        this.annotatorUI = annotatorUI;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);

        populate();

        addEventListeners();
        setTitle("Document Selector");
        setLocation(annotatorUI.getLocationOnScreen());
        setContentPane(panel1);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    public static void main(String[] args) {
        DocumentSelector selector = new DocumentSelector(null);
    }

    public void populate() {
        try {
            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

            RequestEntity<Void> request = RequestEntity.get(new URI(annotatorUI.getHostURL() + "/documents?docText=*&fields=id&fields=filename&fields=category&fields=created&fields=lastUpdated"))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            Map<String, Object> jsonDict = response.getBody();

            documents = ((List<Map<String, Object>>) jsonDict.get("data"));

            Collections.sort(documents, Tools.documentComparator);

            DefaultListModel<String> docsModel = new DefaultListModel<>();
            for (Map document : documents) {
                String displayText = document.get("filename").toString();
                if (document.containsKey("category")) {
                    displayText = document.get("filename").toString() + " (" + document.get("category").toString() + ")";
                }
                long created = Long.parseLong(document.get("created").toString());
                long lastUpdated = Long.parseLong(document.get("lastUpdated").toString());
                if (created == lastUpdated) {
                    displayText = "*" + displayText;
                }
                docsModel.addElement(displayText);
            }

            list1.setModel(docsModel);
            list1.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            list1.setLayoutOrientation(JList.HORIZONTAL_WRAP);
            list1.setVisibleRowCount(-1);

            scrollPane1.setPreferredSize(new Dimension(400, 200));
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(annotatorUI, e.getMessage());
        } catch (ResourceAccessException e) {
            JOptionPane.showMessageDialog(annotatorUI, e.getMessage());
        }
    }

    private void addEventListeners() {
        list1.addListSelectionListener(this);

        fileDrop = new FileDrop(list1, new FileDrop.Listener() {
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
    }

    private void uploadFiles(File[] files) {
        ProcessMonitor procMon = annotatorUI.getProcessMonitor();
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
                        response = restTemplate.exchange(annotatorUI.getHostURL() + "/documents", HttpMethod.POST, request, responseType);
                    } catch (HttpClientErrorException e) {
                        JOptionPane.showMessageDialog(annotatorUI, "Failed to upload file: (" + files[i].getName() + ") Reason: " + e.getMessage());
                    }

                    if (response.getStatusCode() != HttpStatus.OK) {
                        JOptionPane.showMessageDialog(annotatorUI, "Failed to upload file: (" + files[i].getName() + ") Reason: " + response.getStatusCode());
                    }
                }
            } catch (ResourceAccessException e) {
                JOptionPane.showMessageDialog(annotatorUI, e.getMessage());
            } finally {
                procMon.removeProcess(procId);
            }
            annotatorUI.loadActionPerformed(null);
        }).start();
    }

    public void loadURLActionListener(ActionEvent evt) {
        textField1.setBackground(new Color(Color.WHITE.getRGB()));
        String urlString = textField1.getText();
        ProcessMonitor procMon = annotatorUI.getProcessMonitor();
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

                RequestEntity<MultiValueMap<String, Object>> request = RequestEntity.post(new URI(annotatorUI.getHostURL() + "/documents/url"))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(body);

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);
                annotatorUI.loadActionPerformed(null);
            } catch (MalformedURLException e) {
                textField1.setBackground(new Color(Color.RED.getRGB()));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(annotatorUI, e.getMessage());
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
            JOptionPane.showMessageDialog(annotatorUI, e.getMessage());
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
        panel1.setLayout(new GridLayoutManager(3, 5, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(30);
        panel1.add(scrollPane1, new GridConstraints(2, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(800, 600), new Dimension(800, 600), null, 0, false));
        list1 = new JList();
        list1.setLayoutOrientation(0);
        list1.setSelectionMode(0);
        scrollPane1.setViewportView(list1);
        final JLabel label1 = new JLabel();
        label1.setText("Resource URL:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField1 = new JTextField();
        panel1.add(textField1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        loadURLButton = new JButton();
        loadURLButton.setText("Load URL");
        panel1.add(loadURLButton, new GridConstraints(0, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("OR Select a Document From the List Below:");
        panel1.add(label2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        crawlURLButton = new JButton();
        crawlURLButton.setText("Crawl URL");
        panel1.add(crawlURLButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
            ProcessMonitor procMon = annotatorUI.getProcessMonitor();
            procMon.setVisible(true);
            String procId = procMon.addProcess("(" + Instant.now() + ") Crawling URL: " + url);
            try {
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("url", url);

                RequestEntity<MultiValueMap<String, Object>> request = RequestEntity.post(new URI(annotatorUI.getHostURL() + "/documents/crawl"))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(body);

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);
            } catch (URISyntaxException e) {
                JOptionPane.showMessageDialog(annotatorUI, e.getMessage());
            } finally {
                procMon.removeProcess(procId);
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        try {
            int index = list1.getSelectedIndex();
            if (index > -1) {
                Map doc = documents.get(index);
                String id = doc.get("id").toString();

                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                RequestEntity<Void> request = RequestEntity.get(new URI(annotatorUI.getHostURL() + "/documents?id=" + id))
                        .accept(MediaType.APPLICATION_JSON).build();

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                Map<String, Object> jsonDict = response.getBody();

                List<Map<String, Object>> document = ((List<Map<String, Object>>) jsonDict.get("data"));

                annotatorUI.loadDocument(document.get(0));
            }
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(annotatorUI, e.getMessage());
        }
    }

}
