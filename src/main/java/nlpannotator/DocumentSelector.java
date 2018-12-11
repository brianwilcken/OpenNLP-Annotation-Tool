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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;

public class DocumentSelector extends JFrame implements ListSelectionListener {
    private RestTemplate restTemplate;
    private JPanel panel1;
    private JList<String> list1;
    private JScrollPane scrollPane1;
    private JTextField textField1;
    private JButton loadURLButton;
    private List<Map<String, Object>> documents;
    private Main annotatorUI;
    private FileDrop fileDrop;

    public DocumentSelector(Main annotatorUI) {
        this.annotatorUI = annotatorUI;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);

        populate();

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

    private void populate() {
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
                String displayText = document.get("filename").toString() + " (" + document.get("category").toString() + ")";
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

            scrollPane1.setPreferredSize(new Dimension(400, 200));

        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(annotatorUI, e.getMessage());
        } catch (ResourceAccessException e) {
            JOptionPane.showMessageDialog(annotatorUI, e.getMessage());
        }
    }

    private void uploadFiles(File[] files) {
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

//                RequestEntity<Map> request = RequestEntity.post(new URI(annotatorUI.getHostURL() + "/documents"))
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
//                        .body(body);

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
        }
        setVisible(false);
        annotatorUI.loadActionPerformed(null);
    }

    public void loadURLActionListener(ActionEvent evt) {
        try {
            textField1.setBackground(new Color(Color.WHITE.getRGB()));
            String urlString = textField1.getText();
            URL url = new URL(urlString);

            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

//            Map<String, String> urlData = new HashMap<>();
//            urlData.put("url", url.toString());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("url", url.toString());

            RequestEntity<MultiValueMap<String, Object>> request = RequestEntity.post(new URI(annotatorUI.getHostURL() + "/documents/url"))
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body);

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            setVisible(false);
            annotatorUI.loadActionPerformed(null);
        } catch (MalformedURLException e) {
            textField1.setBackground(new Color(Color.RED.getRGB()));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(annotatorUI, e.getMessage());
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        try {
            int index = listSelectionEvent.getFirstIndex();
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
            setVisible(false);
        } catch (URISyntaxException e) {
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
        panel1.setLayout(new GridLayoutManager(3, 4, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(30);
        panel1.add(scrollPane1, new GridConstraints(2, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(800, 600), new Dimension(800, 600), null, 0, false));
        list1 = new JList();
        list1.setLayoutOrientation(0);
        scrollPane1.setViewportView(list1);
        final JLabel label1 = new JLabel();
        label1.setText("Load From URL:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField1 = new JTextField();
        panel1.add(textField1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        loadURLButton = new JButton();
        loadURLButton.setText("Load URL");
        panel1.add(loadURLButton, new GridConstraints(0, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("OR Select a Document From the List Below:");
        panel1.add(label2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}
