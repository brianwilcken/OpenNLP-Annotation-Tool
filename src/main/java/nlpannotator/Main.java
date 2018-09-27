package nlpannotator;

import common.Tools;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import javax.swing.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.swing.event.CaretEvent;
import javax.swing.text.Highlighter.Highlight;

public class Main extends javax.swing.JFrame {

    private RestTemplate restTemplate;
    private final String restApiUrl = Tools.getProperty("restApi.url");
    ArrayList<Offset> coordinates;
    File inputFile;

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
        submit = new javax.swing.JButton();
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

        submit.setText("Submit");
        submit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                submitActionPerformed(evt);
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
                .addComponent(status, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(submit)
                .addGap(29, 29, 29))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(load)
                        .addComponent(submit)
                        .addComponent(fileName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE))
        );

        pack();
    }


    private void loadActionPerformed(java.awt.event.ActionEvent evt) {

        try {
            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {};

            RequestEntity<Void> request = RequestEntity.get(new URI(restApiUrl + "/documents"))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            Map<String, Object> jsonDict = response.getBody();

            for (String key : jsonDict.keySet()) {

            }

            coordinates.clear();//clear the values

            String userDir = System.getProperty("user.home");
            final JFileChooser fileDialog = new JFileChooser(userDir + "/Desktop");
            int fileStatus = fileDialog.showOpenDialog(this);
            if (fileStatus == JFileChooser.APPROVE_OPTION) {
                inputFile = fileDialog.getSelectedFile();
                fileName.setText(inputFile.getName());
                String inputdata = Util.readData(inputFile.getPath());
                if(inputdata!=null||inputdata!="")
                    playground.setText(inputdata);
                else
                    status.setText("Status: Can't read File");
                status.setText("Status: File loaded");
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void submitActionPerformed(java.awt.event.ActionEvent evt) {
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
        
        if(Util.writeData(finaldata_with_even_spaces,inputFile.getParent()+File.separator+"Mod_"+inputFile.getName())!=null){
            System.out.println("Successfuly Written");
            status.setText("Status: Saved to File");
        }
        else{
            System.out.println("Error Occured");
            status.setText("Status: Couldn't save File");
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
    private javax.swing.JTextPane playground;
    private javax.swing.JLabel status;
    private javax.swing.JButton submit;
    // End of variables declaration//GEN-END:variables
}
