package nlpannotator;

import common.Tools;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentSelector extends JFrame implements ListSelectionListener {
    private RestTemplate restTemplate;
    private final String restApiUrl = Tools.getProperty("restApi.url");
    private JPanel panel1;
    private JList<String> list1;
    private JScrollPane scrollPane1;
    private List<Map> documents;
    private Main annotatorUI;

    public DocumentSelector(Main annotatorUI) {
        this.annotatorUI = annotatorUI;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);

        populate();

        setTitle("Document Selector");
        setContentPane(panel1);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    private void populate() {
        try {
            panel1 = new JPanel();

            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {};

            RequestEntity<Void> request = RequestEntity.get(new URI(restApiUrl + "/news?symbol=AMRN&rows=10"))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            Map<String, Object> jsonDict = response.getBody();

            documents = ((List<Map>)jsonDict.get("data"));

            DefaultListModel<String> docsModel = new DefaultListModel<>();
            for (Map document : documents) {
                docsModel.addElement(document.get("title").toString());
            }


            list1 = new JList<>(docsModel);
            list1.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            list1.setLayoutOrientation(JList.HORIZONTAL_WRAP);
            list1.setVisibleRowCount(-1);
            list1.addListSelectionListener(this);

            scrollPane1 = new JScrollPane(list1);

            scrollPane1.setPreferredSize(new Dimension(400, 200));

            panel1.add(scrollPane1);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        int index = listSelectionEvent.getFirstIndex();
        Map doc = documents.get(index);
        annotatorUI.loadDocument(doc);
        setVisible(false);
    }
}
