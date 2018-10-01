package nlpannotator;

import common.Tools;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.swing.event.CaretEvent;
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

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        playground = new javax.swing.JTextPane();
        load = new javax.swing.JButton();
        annotate = new javax.swing.JButton();
        reset = new javax.swing.JButton();
        save = new javax.swing.JButton();
        fileName = new javax.swing.JLabel();
        status = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        playground.setFont(new java.awt.Font("Segoe UI Symbol", 0, 14)); // NOI18N
        jScrollPane1.setViewportView(playground);

        load.setText("Load");
        load.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadActionPerformed(evt);
            }
        });

        annotate.setText("Auto Detect");
        annotate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotateActionPerformed(evt);
            }
        });

        reset.setText("Reset");
        reset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetActionPerformed(evt);
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 191, Short.MAX_VALUE)
                    .addComponent(annotate)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 191, Short.MAX_VALUE)
                    .addComponent(reset)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 191, Short.MAX_VALUE)
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
                            .addComponent(annotate)
                            .addComponent(reset)
                            .addComponent(save)
                            .addComponent(fileName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE))
        );

        pack();
    }

    private void resetActionPerformed(ActionEvent evt) {
        if (document != null) {
            playground.setText(document.get("parsed").toString());
        } else {
            status.setText("Please load a document...");
        }
    }

    private void annotateActionPerformed(ActionEvent evt) {
        if (document != null) {
            try {
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {};

                RequestEntity<Void> request = RequestEntity.get(new URI(restApiUrl + "/documents/annotate/" + document.get("id").toString()))
                        .accept(MediaType.APPLICATION_JSON).build();

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                Map<String, Object> jsonDict = response.getBody();

                String annotated = jsonDict.get("data").toString();

                playground.setText(annotated);

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
        coordinates.clear();//clear the values

        fileName.setText(document.get("filename").toString());
        if (document.containsKey("annotated")) {
            playground.setText(document.get("annotated").toString());
        } else {
            playground.setText(document.get("parsed").toString());
        }
        status.setText("Status: File loaded");
        this.document = document;
    }

    private void saveActionPerformed(java.awt.event.ActionEvent evt) {
        if (document != null) {
            coordinates=Util.removeSingleChar(coordinates);
            String orgText=playground.getText();
            orgText=orgText.replaceAll("\r", "");

            int prev_index=0,next_index=orgText.length();
            Collections.sort(coordinates);
            StringBuilder finalData=new StringBuilder();

            for (Offset data : coordinates) {
                int start=data.getStart();
                int end=data.getEnd();
                String orgStr=orgText.substring(start, end);
                String newStr=" <START:FAC> "+orgStr+" <END> ";

                finalData.append(orgText.substring(prev_index, start));
                if(newStr!=null)
                    finalData.append(newStr);

                prev_index=end;
            }
            finalData.append(orgText.substring(prev_index,next_index));
            String finaldata_with_even_spaces=Util.makeEvenSpaces(finalData.toString());

            if (document.containsKey("annotated")) {
                document.replace("annotated", finaldata_with_even_spaces);
            } else {
                document.put("annotated", finaldata_with_even_spaces);
            }

            try {

                MultiValueMap<String, String> doc = new LinkedMultiValueMap<>();
                for (Object key : document.keySet()) {
                    String docKey = key.toString();
                    String value = document.get(key).toString();
                    doc.add(docKey, value);
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

                Map<String, Object> jsonDict = response.getBody();

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

        annotator.eventListeners();

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel fileName;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton load;
    private javax.swing.JButton annotate;
    private javax.swing.JButton reset;
    private javax.swing.JTextPane playground;
    private javax.swing.JLabel status;
    private javax.swing.JButton save;
    // End of variables declaration//GEN-END:variables
}
