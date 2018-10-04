package nlpannotator;

import common.Tools;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.text.*;
import javax.swing.text.Highlighter.Highlight;

public class Main extends javax.swing.JFrame {


    ArrayList<Offset> coordinates;
    Map document;
    private RestTemplate restTemplate;
    private final String restApiUrl = Tools.getProperty("restApi.url");

    public Main() {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);
        initComponents();
    }

    private class PlaygroundKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (document != null) {
                if (e.getKeyCode() == KeyEvent.VK_F1) {
                    //F1 key
                    Highlight[] highlights = playground.getHighlighter().getHighlights();
                    for (Highlight highlight : highlights) {
                        Document doc = playground.getDocument();
                        int start = highlight.getStartOffset();
                        int end = highlight.getEndOffset();

                        String annotation = getAnnotation();
                        try {
                            doc.insertString(start, annotation, null);
                            doc.insertString(end + annotation.length(), " <END> ", null);
                        } catch (BadLocationException e1) {
                            e1.printStackTrace();
                        }
                    }
                    highlightAnnotations();
                } else if (e.getKeyCode() == KeyEvent.VK_F2) {
                    //F2 key
                    Highlight[] highlights = playground.getHighlighter().getHighlights();
                    for (Highlight highlight : highlights) {
                        Document doc = playground.getDocument();
                        int start = highlight.getStartOffset();
                        int end = highlight.getEndOffset();

                        String annotation = getAnnotation();
                        try {
                            int caretPos = playground.getCaretPosition();
                            String selectedText = doc.getText(start, end - start);
                            String annotated = doc.getText(0, doc.getLength()).replaceAll(selectedText, annotation + selectedText + " <END> ");
                            doc.remove(0, doc.getLength());
                            doc.insertString(0, annotated, null);
                            playground.setCaretPosition(caretPos);
                        } catch (BadLocationException e1) {
                            e1.printStackTrace();
                        }
                    }
                    highlightAnnotations();
                } else if ((e.getKeyCode() == KeyEvent.VK_F) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                    findActionPerformed(null);
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    }

    private String getAnnotation() {
        String annotationType = type.getText();
        String annotation = " <START:" + annotationType + "> ";

        return annotation;
    }

    private void highlightAnnotations() {
        highlightText(getAnnotation(), true, Color.BLUE, null);
        String endAnnotation = " <END> ";
        highlightText(endAnnotation, true, Color.RED, null);
    }

    private void highlightText(String str, Boolean isBold, Color foreColor, Color backColor) {
        StyledDocument style = playground.getStyledDocument();
        Document doc = playground.getDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            int len = str.length();
            int index = text.indexOf(str);
            while (index >= 0) {
                int end = index + len;
                AttributeSet oldSet = style.getCharacterElement(end - 1).getAttributes();
                StyleContext sc = StyleContext.getDefaultStyleContext();
                AttributeSet textColor = sc.addAttribute(oldSet, StyleConstants.Foreground, foreColor);
                AttributeSet bold = sc.addAttribute(textColor, StyleConstants.Bold, isBold);
                if (backColor != null) {
                    AttributeSet background = sc.addAttribute(bold, StyleConstants.Background, backColor);
                    style.setCharacterAttributes(index, len, background, true);
                } else {
                    style.setCharacterAttributes(index, len, bold, true);
                }

                index = text.indexOf(str, index + 1);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void highlightFound(List found) {
        for (Object str : found) {
            highlightText((String)str, true, Color.GREEN, Color.BLACK);
        }
    }

    public void removeHighlights(List remove) {
        for (Object str : remove) {
            highlightText((String)str, false, Color.BLACK, Color.WHITE);
        }
    }

    public void replaceAllText(List selections, String replace) {
        Document doc = playground.getDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            for (Object selection : selections) {
                text = text.replaceAll((String)selection, replace);
            }
            playground.setText(text);
            highlightAnnotations();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        playground = new javax.swing.JTextPane();
        load = new javax.swing.JButton();
        train = new javax.swing.JButton();
        annotate = new javax.swing.JButton();
        reset = new javax.swing.JButton();
        find = new javax.swing.JButton();
        save = new javax.swing.JButton();
        fileName = new javax.swing.JLabel();
        status = new javax.swing.JLabel();
        type = new JTextField(5);
        type.setText("FAC");

        JLabel lblTag = new JLabel();
        lblTag.setText("Annotation Tag:");

        DefaultBoundedRangeModel model = new DefaultBoundedRangeModel(50, 0, 1, 100);
        slider = new JSlider(model);
        slider.setOrientation(JSlider.HORIZONTAL);

        Hashtable position = new Hashtable();
        position.put(0, new JLabel("0"));
        position.put(25, new JLabel("25"));
        position.put(50, new JLabel("50"));
        position.put(75, new JLabel("75"));
        position.put(100, new JLabel("100"));

        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setLabelTable(position);

        playground.addKeyListener(new PlaygroundKeyListener());

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        playground.setFont(new java.awt.Font("Segoe UI Symbol", 0, 16)); // NOI18N
        jScrollPane1.setViewportView(playground);

        load.setText("Load");
        load.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadActionPerformed(evt);
            }
        });

        train.setText("Train Model");
        train.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trainModelActionPerformed(evt);
            }
        });

        annotate.setText("Auto Detect");
        annotate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoAnnotateActionPerformed(evt);
            }
        });

        reset.setText("Reset");
        reset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetActionPerformed(evt);
            }
        });

        find.setText("Find/Replace");
        find.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findActionPerformed(evt);
            }
        });

        save.setText("Save");
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt);
            }
        });

        status.setText("Status:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(fileName, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(load)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
                .addComponent(train)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
                .addComponent(lblTag)
                .addComponent(type)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
                .addComponent(annotate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 140, Short.MAX_VALUE)
                .addComponent(slider)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 140, Short.MAX_VALUE)
                .addComponent(reset)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
                .addComponent(find)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 140, Short.MAX_VALUE)
                .addComponent(status, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(save)
                .addGap(29, 29, 29))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(load)
                            .addComponent(train)
                            .addComponent(lblTag)
                            .addComponent(type)
                            .addComponent(annotate)
                            .addComponent(slider)
                            .addComponent(reset)
                            .addComponent(find)
                            .addComponent(save)
                            .addComponent(fileName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE))
        );

        pack();
    }

    private FindAndReplace findAndReplace;
    private void findActionPerformed(ActionEvent evt) {
        if (document != null) {
            if (findAndReplace == null) {
                findAndReplace = new FindAndReplace("FindAndReplace", this);
            } else {
                findAndReplace.setVisible(true);
            }
            Highlight[] highlights = playground.getHighlighter().getHighlights();
            for (Highlight highlight : highlights) {
                Document doc = playground.getDocument();
                int start = highlight.getStartOffset();
                int end = highlight.getEndOffset();
                int caretPos = playground.getCaretPosition();
                try {
                    String selectedText = doc.getText(start, end - start);
                    findAndReplace.findHighlighted(selectedText);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void trainModelActionPerformed(ActionEvent evt) {
        if (document != null) {
            try {
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                RequestEntity<Void> request = RequestEntity.get(new URI(restApiUrl + "/documents/trainNER" + "?category=" + document.get("category").toString()))
                        .accept(MediaType.APPLICATION_JSON).build();

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                if (response.getStatusCode() == HttpStatus.OK) {
                    status.setText("Model Training Started");
                } else {
                    status.setText("SERVER ERROR!!!");
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    private void resetActionPerformed(ActionEvent evt) {
        if (document != null) {
            playground.setText(document.get("parsed").toString());
            playground.setCaretPosition(0);
            highlightAnnotations();
        } else {
            status.setText("Please load a document...");
        }
    }

    private void autoAnnotateActionPerformed(ActionEvent evt) {
        if (document != null) {
            try {
                int threshold = slider.getValue();

                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {};

                RequestEntity<Void> request = RequestEntity.get(new URI(restApiUrl + "/documents/annotate/" + document.get("id").toString() + "?threshold=" + threshold))
                        .accept(MediaType.APPLICATION_JSON).build();

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                Map<String, Object> jsonDict = response.getBody();

                String annotated = jsonDict.get("data").toString();

                playground.setText(annotated);
                playground.setCaretPosition(0);
                highlightAnnotations();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            status.setText("Please load a document...");
        }
    }


    private void loadActionPerformed(java.awt.event.ActionEvent evt) {
        DocumentSelector documentSelector = new DocumentSelector(this);
    }

    public void loadDocument(Map document) {
        fileName.setText(document.get("filename").toString());
        if (document.containsKey("annotated")) {
            playground.setText(document.get("annotated").toString());
        } else {
            playground.setText(document.get("parsed").toString());
        }
        playground.setCaretPosition(0);
        status.setText("Status: File loaded");
        this.document = document;
        highlightAnnotations();
    }

    private void saveActionPerformed(java.awt.event.ActionEvent evt) {
        if (document != null) {
            if (document.containsKey("annotated")) {
                document.replace("annotated", playground.getText());
            } else {
                document.put("annotated", playground.getText());
            }

            try {
                Map<String, String> doc = new HashMap<>();
                for (Object key : document.keySet()) {
                    String docKey = key.toString();
                    String value = document.get(key).toString();
                    doc.put(docKey, value);
                }

                MultiValueMap<String, Object> metadata = new LinkedMultiValueMap<>();
                metadata.add("metadata", doc);

                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {};

                RequestEntity<Map> request = RequestEntity.put(new URI(restApiUrl + "/documents/metadata/" + document.get("id").toString()))
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                        .body(metadata);

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                if (response.getStatusCode() == HttpStatus.OK) {
                    status.setText("Save Successful");
                } else {
                    status.setText("SAVE FAILURE!!!");
                }

            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            status.setText("Please load a document...");
        }
    }

    private void eventListeners() {
        coordinates = new ArrayList();

        playground.addCaretListener((CaretEvent e) -> {
            Highlight[] h = playground.getHighlighter().getHighlights();
            for (Highlight h1 : h) {

                int start = h1.getStartOffset();
                int end = h1.getEndOffset();

                Offset offset = new Offset(start, end);
                if (coordinates.contains(offset)) { //in case people needs to remove annotation
                    coordinates.remove(offset);
                    status.setText("Status: Word Removed");
                    //style.setCharacterAttributes(start, (end - start), black, false);
                } else {
                    coordinates = Util.overlapsAdd(offset, coordinates);
                    status.setText("Status: Word added");
                    //style.setCharacterAttributes(start, (end - start), red, true);
                }

                System.out.println(h1.getStartOffset());
                System.out.println(h1.getEndOffset());
                Util.refreshColor(playground);
                Util.colorIt(coordinates,playground);
            }
        });

    }

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        Main annotator = new Main();
        java.awt.EventQueue.invokeLater(() -> {
            annotator.setVisible(true);
        });

        //annotator.eventListeners();

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel fileName;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton load;
    private javax.swing.JButton train;
    private javax.swing.JButton annotate;
    private javax.swing.JButton reset;
    private javax.swing.JButton find;
    private javax.swing.JTextPane playground;
    private javax.swing.JLabel status;
    private javax.swing.JButton save;
    private JSlider slider;
    private JTextField type;
    // End of variables declaration//GEN-END:variables
}
