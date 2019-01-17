package nlpannotator;

import com.google.common.base.Strings;
import common.FacilityTypes;
import common.SizedStack;
import common.Tools;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.Highlighter.Highlight;

public class Main extends javax.swing.JFrame {


    ArrayList<Offset> coordinates;
    Map document;
    private DefaultComboBoxModel doccatModel;

    private RestTemplate restTemplate;
    private ProcessMonitor processMonitor;
    private DocumentSelector documentSelector;
    private HistoryViewer historyViewer;
    private AutoDetectionThreshold autoDetectionThreshold;
    private AnnotatedLinesTracker annotatedLinesTracker;

    private SizedStack<String> undoStates;
    private SizedStack<String> redoStates;

    public Main() {
        undoStates = new SizedStack<>(10);
        redoStates = new SizedStack<>(10);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);
        initComponents();
    }

    private boolean validateAnnotation(int start, int end, Document doc) {
        try {
            Pattern lineBreakPattern = Pattern.compile("(\\n|\\r)");

            //valid annotations may not be multi-sentence
            String text = doc.getText(start, end - start);
            if (lineBreakPattern.matcher(text).find()) {
                return false;
            }

            //valid annotations must begin and end with complete words
            Pattern wordBreakPattern = Pattern.compile("(\\W|\\s)");
            if (start > 0 && end < doc.getLength() - 1) {
                String prevChar = doc.getText(start - 1, 1);
                String postChar = doc.getText(end, 1);

                return wordBreakPattern.matcher(prevChar).matches() && wordBreakPattern.matcher(postChar).matches();
            } else if (start == 0 && end < doc.getLength() - 1) {
                String postChar = doc.getText(end, 1);

                return wordBreakPattern.matcher(postChar).matches();
            } else if (start > 0 && end == doc.getLength() - 1) {
                String prevChar = doc.getText(start - 1, 1);

                return wordBreakPattern.matcher(prevChar).matches();
            } else {
                return false;
            }
        } catch (BadLocationException e) {
            return false;
        }
    }

    private void annotateSingle(Highlight[] highlights) {
        for (Highlight highlight : highlights) {
            Document doc = playground.getDocument();
            int start = highlight.getStartOffset();
            int end = highlight.getEndOffset();

            if (validateAnnotation(start, end, doc)) {
                String annotation = getAnnotation();
                try {
                    doc.insertString(start, annotation, null);
                    doc.insertString(end + annotation.length(), " <END> ", null);
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Annotation is not valid!");
            }
        }
        removeHighlights();
        highlightAnnotations();
        highlightFound();
    }

    private void annotateMultiple(Highlight[] highlights) {
        for (Highlight highlight : highlights) {
            Document doc = playground.getDocument();
            int start = highlight.getStartOffset();
            int end = highlight.getEndOffset();

            if (validateAnnotation(start, end, doc)) {
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
            } else {
                JOptionPane.showMessageDialog(this, "Annotation is not valid!");
            }
        }
        removeHighlights();
        highlightAnnotations();
        highlightFound();
    }

    private void unannotateSingle(Highlight[] highlights) {
        for (Highlight highlight : highlights) {
            Document doc = playground.getDocument();
            int start = highlight.getStartOffset();
            int end = highlight.getEndOffset();

            String annotationType = getAnnotationType();
            Pattern annotationPattern = Pattern.compile(" ?<START:" + annotationType + ">.+?<END> ?");
            try {
                String highlighted = doc.getText(start, (end - start));
                Matcher annotationMatcher = annotationPattern.matcher(highlighted);
                if (annotationMatcher.find()) {
                    highlighted = highlighted.replaceAll(" ?<START:" + annotationType + "> ", "");
                    highlighted = highlighted.replaceAll(" <END> ?", "");
                    doc.remove(start, (end - start));
                    doc.insertString(start, highlighted, null);
                }
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }
        updateAnnotatedLinesList();
    }

    private void unannotateMultiple(Highlight[] highlights) {
        for (Highlight highlight : highlights) {
            Document doc = playground.getDocument();
            int start = highlight.getStartOffset();
            int end = highlight.getEndOffset();

            String annotationType = getAnnotationType();
            Pattern annotationPattern = Pattern.compile(" ?<START:" + annotationType + ">.+?<END> ?");
            try {
                int caretPos = playground.getCaretPosition();
                String highlighted = doc.getText(start, (end - start));
                Matcher annotationMatcher = annotationPattern.matcher(highlighted);
                if (annotationMatcher.find()) {
                    String unannotated = highlighted.replaceAll(" ?<START:" + annotationType + "> ", "");
                    unannotated = unannotated.replaceAll(" <END> ?", "");
                    String unannotatedDoc = doc.getText(0, doc.getLength()).replaceAll(highlighted, unannotated);
                    doc.remove(0, doc.getLength());
                    doc.insertString(0, unannotatedDoc, null);
                    playground.setCaretPosition(caretPos);
                }
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }
        removeHighlights();
        highlightAnnotations();
        highlightFound();
    }

    public void updateAnnotatedLinesList() {
        AnnotatedLinesTracker tracker = getAnnotatedLinesTracker();
        String annotationTarget = "<END>";
        if(tracker.inSelectedAnnotationMode()) {
            annotationTarget = getAnnotation();
        }
        String[] allLines = playground.getText().split(System.lineSeparator());

        TreeMap<Integer, String> annotatedLines = new TreeMap<>();
        for (int i = 0; i < allLines.length; i++) {
            if (allLines[i].contains(annotationTarget)) {
                annotatedLines.put(i, allLines[i]);
            }
        }

        tracker.update(annotatedLines);
        tracker.setVisible(true);
    }

    private AnnotatedLinesTracker getAnnotatedLinesTracker() {
        if (annotatedLinesTracker == null) {
            annotatedLinesTracker = new AnnotatedLinesTracker(this);
        }
        return annotatedLinesTracker;
    }

    private class PlaygroundKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (document != null) {

                if (e.getKeyCode() == KeyEvent.VK_F1) {
                    Highlight[] highlights = playground.getHighlighter().getHighlights();
                    undoStates.push(playground.getText());
                    //F1 key - add annotation to single element
                    annotateSingle(highlights);
                } else if (e.getKeyCode() == KeyEvent.VK_F2) {
                    Highlight[] highlights = playground.getHighlighter().getHighlights();
                    undoStates.push(playground.getText());
                    //F2 key - add annotation to all elements
                    annotateMultiple(highlights);
                } else if ((e.getKeyCode() == KeyEvent.VK_F) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                    findActionPerformed(null);
                } else if ((e.getKeyCode() == KeyEvent.VK_Z) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                    undo();
                } else if ((e.getKeyCode() == KeyEvent.VK_Y) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                    redo();
                } else if ((e.getKeyCode() == KeyEvent.VK_P) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                    getProcessMonitor().setVisible(true);
                } else if ((e.getKeyCode() == KeyEvent.VK_H) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                    showHistoryViewer();
                } else if ((e.getKeyCode() == KeyEvent.VK_M) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                    showMetadataEditor();
                } else if ((e.getKeyCode() == KeyEvent.VK_D) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                    deleteDocument();
                } else if ((e.getKeyCode() == KeyEvent.VK_R) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                    resetDocument();
                } else if ((e.getKeyCode() == KeyEvent.VK_N) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                    showAutoDetectionThreshold();
                } else if (e.getKeyCode() == KeyEvent.VK_F3) {
                    Highlight[] highlights = playground.getHighlighter().getHighlights();
                    undoStates.push(playground.getText());
                    //F3 Key - remove annotation from single element
                    unannotateSingle(highlights);
                } else if (e.getKeyCode() == KeyEvent.VK_F4) {
                    Highlight[] highlights = playground.getHighlighter().getHighlights();
                    undoStates.push(playground.getText());
                    //F4 Key - remove annotation from all elements
                    unannotateMultiple(highlights);
                } else if (e.getKeyCode() == KeyEvent.VK_F5) {
                    trainNERModelActionPerformed(null);
                } else if (e.getKeyCode() == KeyEvent.VK_F6) {
                    trainDoccatModelActionPerformed(null);
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    }

    public void showAutoDetectionThreshold() {
        if (document != null) {
            if (autoDetectionThreshold == null) {
                autoDetectionThreshold = new AutoDetectionThreshold(this);
            }
            autoDetectionThreshold.setVisible(true);
        }
    }

    public void showHistoryViewer() {
        if (document != null) {
            if (historyViewer == null) {
                historyViewer = new HistoryViewer(this);
            }
            historyViewer.populate(document);
            historyViewer.setVisible(true);
        }
    }

    public ProcessMonitor getProcessMonitor() {
        if (processMonitor == null) {
            processMonitor = new ProcessMonitor(this);
        }
        return processMonitor;
    }

    private void undo() {
        if (document != null && !undoStates.isEmpty()) {
            int caretPos = playground.getCaretPosition();
            redoStates.push(playground.getText());
            playground.setText(undoStates.pop());
            playground.setCaretPosition(caretPos);
            removeHighlights();
            highlightAnnotations();
            highlightFound();
        }
    }

    private void redo() {
        if (document != null && !redoStates.isEmpty()) {
            int caretPos = playground.getCaretPosition();
            undoStates.push(playground.getText());
            playground.setText(redoStates.pop());
            playground.setCaretPosition(caretPos);
            removeHighlights();
            highlightAnnotations();
            highlightFound();
        }
    }

    private String getAnnotation() {
        String annotationType = getAnnotationType();
        String annotation = " <START:" + annotationType + "> ";

        return annotation;
    }

    private String getAnnotationType() {
        String annotationType = type.getSelectedItem().toString().replace("--", "");

        return annotationType;
    }

    public List getDocumentCategories() {
        List categories = doccat.getSelectedValuesList();

        return categories;
    }

    public void highlightAnnotations() {
        highlightText(getAnnotation(), true, Color.BLUE, null);
        String endAnnotation = " <END> ";
        highlightText(endAnnotation, true, Color.RED, null);
        updateAnnotatedLinesList();
    }

    public void highlightFound(TreeMap<Integer, String> locMap) {
        if (locMap != null) {
            highlightText(locMap, true, Color.GREEN, Color.BLACK);
        }
    }

    public void navigatePrevious(TreeMap<Integer, String> locMap) {
        Integer prev = null;
        int caret = playground.getCaretPosition();
        if (locMap.size() > 0) {
            prev = locMap.lowerKey(caret);
            if (prev == null) {
                prev = locMap.lastKey();
            }
            playground.setCaretPosition(prev);
        }
    }

    public void navigateNext(TreeMap<Integer, String> locMap) {
        Integer next = null;
        int caret = playground.getCaretPosition();
        if (locMap.size() > 0) {
            next = locMap.higherKey(caret);
            if (next == null) {
                next = locMap.firstKey();
            }
            playground.setCaretPosition(next);
        }
    }

    public void navigateToLine(String line) {
        Document doc = playground.getDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            int index = text.indexOf(line);
            playground.setCaretPosition(index);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public TreeMap<Integer, String> getLocationMap(List foundList) {
        Document doc = playground.getDocument();
        TreeMap<Integer, String> locMap = new TreeMap<>();
        try {
            String text = doc.getText(0, doc.getLength());
            for (Object obj : foundList) {
                String found = obj.toString();
                int index = text.indexOf(found);
                while (index >= 0) {
                    locMap.put(index, found);
                    index = text.indexOf(found, index + 1);
                }
            }
            return locMap;
        } catch (BadLocationException e) {
            return null;
        }
    }

    private void highlightText(TreeMap<Integer, String> locMap, Boolean isBold, Color foreColor, Color backColor) {
        for (int index : locMap.keySet()) {
            int len = locMap.get(index).length();
            applyTextStyle(index, len, isBold, foreColor, backColor);
        }
    }

    private void highlightText(String str, Boolean isBold, Color foreColor, Color backColor) {
        Document doc = playground.getDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            int len = str.length();
            int index = text.indexOf(str);
            while (index >= 0) {
                applyTextStyle(index, len, isBold, foreColor, backColor);

                index = text.indexOf(str, index + 1);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void applyTextStyle(int index, int len, Boolean isBold, Color foreColor, Color backColor) {
        StyledDocument style = playground.getStyledDocument();
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
    }

    private void highlightFound() {
        if (findAndReplace != null) {
            TreeMap<Integer, String> locMap = findAndReplace.getLocationMap();
            highlightFound(locMap);
        }
    }

    public void removeHighlights() {
        Document doc = playground.getDocument();
        StyledDocument style = playground.getStyledDocument();
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet empty = sc.getEmptySet();
        AttributeSet foreground = sc.addAttribute(empty, StyleConstants.Foreground, Color.BLACK);
        AttributeSet background = sc.addAttribute(foreground, StyleConstants.Background, Color.WHITE);
        AttributeSet bold = sc.addAttribute(background, StyleConstants.Bold, false);
        style.setCharacterAttributes(0, doc.getLength(), bold, true);
    }

    public void replaceAllText(List selections, String replace) {
        undoStates.push(playground.getText());
        Document doc = playground.getDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            for (Object selection : selections) {
                text = text.replaceAll((String)selection, replace);
            }
            playground.setText(text);
            //highlightAnnotations();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void populateDocumentCategories() {
        doccatModel = new DefaultComboBoxModel();

        for (String key : FacilityTypes.dictionary.keySet()) {
            doccatModel.addElement(key);
        }

        doccat.setModel(doccatModel);
    }

    private void populateAnnotationTypes() {
        DefaultComboBoxModel model = new DefaultComboBoxModel();

        for (String key : FacilityTypes.dictionary.keySet()) {
            model.addElement("--" + key + "--");
            List<String> facilityTypes = FacilityTypes.dictionary.get(key);
            for (String facilityType : facilityTypes) {
                model.addElement(facilityType);
            }
        }

        type.setModel(model);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        setTitleFilename("");

        jScrollPane1 = new javax.swing.JScrollPane();
        playground = new javax.swing.JTextPane();
        playground.setEditable(false);
        load = new javax.swing.JButton();
        save = new javax.swing.JButton();
        process = new javax.swing.JButton();
        download = new javax.swing.JButton();
        hostLabel = new JLabel();
        host = new JTextField();
        type = new JComboBox();
        populateAnnotationTypes();
        doccat = new JList();
        doccatPane = new JScrollPane();
        populateDocumentCategories();

        JLabel annotationLblTag = new JLabel();
        annotationLblTag.setText("Annotation Tag:");

        JLabel doccatLblTag = new JLabel();
        doccatLblTag.setText("Document Category:");

        hostLabel.setText("Host:");
        host.setText(Tools.getProperty("restApi.url"));

        doccat.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        doccat.setLayoutOrientation(JList.VERTICAL);
        doccat.setVisibleRowCount(6);

        doccatPane.setViewportView(doccat);
        doccatPane.setPreferredSize(new Dimension(180, 100));
        doccatPane.setMaximumSize(new Dimension(180, 100));

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

        save.setText("Save Document");
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt, false);
            }
        });

        process.setText("Process Document");
        process.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt, true);
            }
        });

        download.setText("Download Document");
        download.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadActionPerformed(evt);
            }
        });

        type.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                highlightActionPerformed(actionEvent);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addGap(2, 2, 2)
                    .addComponent(hostLabel)
                    .addComponent(host)
                    .addComponent(load)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 100, Short.MAX_VALUE)
                    .addComponent(annotationLblTag)
                    .addComponent(type)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                    .addComponent(doccatLblTag)
                    .addComponent(doccatPane)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(save)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 5, Short.MAX_VALUE)
                    .addComponent(process)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 5, Short.MAX_VALUE)
                    .addComponent(download)
                    .addGap(29, 29, 29))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(hostLabel)
                            .addComponent(host)
                            .addComponent(load)
                            .addComponent(annotationLblTag)
                            .addComponent(type)
                            .addComponent(doccatLblTag)
                            .addComponent(doccatPane)
                            .addComponent(save)
                            .addComponent(process)
                            .addComponent(download)))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE)
            )
        );

        pack();
    }

    private FindAndReplace findAndReplace;
    private void findActionPerformed(ActionEvent evt) {
        if (document != null) {
            if (findAndReplace == null) {
                findAndReplace = new FindAndReplace("FindAndReplace", this);
                findAndReplace.init();
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
                    JOptionPane.showMessageDialog(this, e.getMessage());
                }
            }
        }
    }

    private void highlightActionPerformed(ActionEvent evt) {
        if (document != null) {
            removeHighlights();
            highlightAnnotations();
            highlightFound();
        }
    }

    private void trainNERModelActionPerformed(ActionEvent evt) {
        if (document != null) {
            ProcessMonitor procMon = getProcessMonitor();
            procMon.setVisible(true);
            String procId = procMon.addProcess("(" + Instant.now() + ") Training NER Model");
            new Thread(() -> {
                try {
                    ParameterizedTypeReference<HashMap<String, Object>> responseType =
                            new ParameterizedTypeReference<HashMap<String, Object>>() {
                            };

                    List categories = (List)document.get("category");
                    List<String> categoryList = (List<String>)categories.stream().map(category -> "category=" + category.toString()).collect(Collectors.toList());
                    String categoryQuery = categoryList.stream().reduce((p1, p2) -> p1 + "&" + p2).orElse("");
                    RequestEntity<Void> request = RequestEntity.get(new URI(getHostURL() + "/documents/trainNER" + "?" + categoryQuery + "&doAsync=false"))
                            .accept(MediaType.APPLICATION_JSON).build();

                    ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                    if (response.getStatusCode() != HttpStatus.OK) {
                        JOptionPane.showMessageDialog(this, "Server error has occurred!!");
                    }
                } catch (URISyntaxException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage());
                } catch (ResourceAccessException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage());
                } catch (HttpServerErrorException e) {
                    JOptionPane.showMessageDialog(this, e.getResponseBodyAsString());
                } finally {
                    procMon.removeProcess(procId);
                }
            }).start();
        }
    }

    private void trainDoccatModelActionPerformed(ActionEvent evt) {
        ProcessMonitor procMon = getProcessMonitor();
        procMon.setVisible(true);
        String procId = procMon.addProcess("(" + Instant.now() + ") Training Doccat Model");
        new Thread(() -> {
            try {
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                RequestEntity<Void> request = RequestEntity.get(new URI(getHostURL() + "/documents/trainDoccat?doAsync=false"))
                        .accept(MediaType.APPLICATION_JSON).build();


                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                if (response.getStatusCode() == HttpStatus.OK) {
                    double accuracy = (double)response.getBody().get("data");
                    JOptionPane.showMessageDialog(this, "Model accuracy: " + accuracy);
                } else {
                    JOptionPane.showMessageDialog(this, "Server error has occurred!!");
                }
            } catch (URISyntaxException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            } catch (ResourceAccessException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            } catch (HttpServerErrorException e) {
                JOptionPane.showMessageDialog(this, e.getResponseBodyAsString());
            } finally {
                procMon.removeProcess(procId);
            }
        }).start();

    }

    private void resetDocument() {
        if (document != null) {
            undoStates.empty();
            redoStates.empty();
            playground.setText(document.get("parsed").toString());
            playground.setCaretPosition(0);
            highlightAnnotations();
        } else {
            JOptionPane.showMessageDialog(this, "Please load a document...");
        }
    }

    public void autoAnnotateDocument(int threshold) {
        if (document != null) {
            undoStates.empty();
            redoStates.empty();
            try {
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {};

                RequestEntity<Void> request = RequestEntity.get(new URI(getHostURL() + "/documents/annotate/" + document.get("id").toString() + "?threshold=" + threshold))
                        .accept(MediaType.APPLICATION_JSON).build();

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                Map<String, Object> jsonDict = response.getBody();

                String annotated = jsonDict.get("data").toString();

                playground.setText(annotated);
                playground.setCaretPosition(0);
                highlightAnnotations();
            } catch (URISyntaxException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            } catch (ResourceAccessException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please load a document...");
        }
    }

    public String getHostURL() {
        return host.getText();
    }

    public void loadActionPerformed(java.awt.event.ActionEvent evt) {
        if (documentSelector == null) {
            documentSelector = new DocumentSelector(this);
        } else {
            documentSelector.populate();
            documentSelector.setVisible(true);
        }
    }

    private void showMetadataEditor() {
        if (document != null) {
            MetadataEditor editor = new MetadataEditor(this);
            editor.populate(document);
        }
    }

    public void updateMetadata(Map<Object, Object> doc) {
        this.document = doc;
    }

    public void updateAnnotation(String annotated) {
        int caretPos = playground.getCaretPosition();
        document.replace("annotated", annotated);
        loadDocument(document);
        playground.setCaretPosition(caretPos);
    }

    public void reloadHistory() {
        if (historyViewer != null && historyViewer.isVisible()) {
            historyViewer.populate(document);
        }
    }

    public void loadDocument(Map document) {
        undoStates.empty();
        redoStates.empty();
        setTitleFilename(document.get("filename").toString());

        if (document.containsKey("category")) {
            List categories = (List)document.get("category");
            int[] selections = new int[0];
            for (Object category : categories) {
                int selected = doccatModel.getIndexOf(category);
                selections = ArrayUtils.addAll(selections, new int[]{selected});
            }
            doccat.setSelectedIndices(selections);
        }
        if (document.containsKey("annotated")) {
            playground.setText(document.get("annotated").toString());
        } else {
            playground.setText(document.get("parsed").toString());
        }

        playground.setCaretPosition(0);
        this.document = document;
        removeHighlights();
        highlightAnnotations();

        if (findAndReplace != null) {
            findAndReplace.reset();
        }
    }

    private boolean validateForSave() {
        Pattern duplicateTags = Pattern.compile("<START:[\\w]*?>(?=(?:(?!<END>).)*<START:[\\w]*?>)", Pattern.MULTILINE);
        boolean isValid = true;

        String text = playground.getText();
        Matcher duplicateMatcher = duplicateTags.matcher(text);
        while(duplicateMatcher.find()) {
            String duplicateTag = text.substring(duplicateMatcher.start(), duplicateMatcher.end());
            int len = duplicateTag.length();
            applyTextStyle(duplicateMatcher.start() - 1, len, true, Color.RED, Color.black);
            isValid = false;
        }

        if (isValid) {
            removeHighlights();
            highlightAnnotations();
            highlightFound();
        } else {
            JOptionPane.showMessageDialog(this, "Please resolve all duplicate annotation tags before saving.");
        }

        return isValid;
    }

    private void saveActionPerformed(java.awt.event.ActionEvent evt, boolean doNLP) {
        if (document != null && validateForSave()) {
            ProcessMonitor procMon = getProcessMonitor();
            procMon.setVisible(true);
            String procId = procMon.addProcess("(" + Instant.now() + ") Processing Document: " + document.get("filename").toString());
            new Thread(() -> {
                if (document.containsKey("annotated")) {
                    document.replace("annotated", playground.getText());
                } else {
                    document.put("annotated", playground.getText());
                }

                List categories = doccat.getSelectedValuesList();
                if (document.containsKey("category")) {
                    document.replace("category", categories);
                } else {
                    document.put("category", categories);
                }

                String annotatedBy = System.getProperty("user.name");
                if (document.containsKey("annotatedBy")) {
                    document.replace("annotatedBy", annotatedBy);
                } else {
                    document.put("annotatedBy", annotatedBy);
                }

                try {
                    Map<String, Object> doc = new HashMap<>();
                    for (Object key : document.keySet()) {
                        String docKey = key.toString();
                        Object value = document.get(key);
                        doc.put(docKey, value);
                    }

                    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                    body.add("metadata", doc);
                    body.add("doNLP", doNLP);

                    ParameterizedTypeReference<HashMap<String, Object>> responseType =
                            new ParameterizedTypeReference<HashMap<String, Object>>() {};

                    RequestEntity<Map> request = RequestEntity.put(new URI(getHostURL() + "/documents/metadata/" + document.get("id").toString()))
                            .accept(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                            .body(body);

                    ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                    if (response.getStatusCode() == HttpStatus.OK) {
                        if (doNLP) {
                            JOptionPane.showMessageDialog(this, "Document Processing Complete");
                        } else {
                            JOptionPane.showMessageDialog(this, "Save Successful");
                        }
                        reloadHistory();
                        documentSelector.populate();
                    } else {
                        JOptionPane.showMessageDialog(this, "SAVE FAILURE!!!");
                    }

                } catch (URISyntaxException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage());
                } catch (ResourceAccessException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage());
                } finally {
                    procMon.removeProcess(procId);
                }
            }).start();
        } else {
            JOptionPane.showMessageDialog(this, "Please load a document...");
        }
    }

    private void downloadActionPerformed(ActionEvent evt) {
        if (document != null) {
            try {
                String filename = document.get("filename").toString();
                final Path path = getSavePath(filename);
                if (path != null) {
                    if (path.toFile().exists()) {
                        if (Desktop.isDesktopSupported()) {
                            File file = path.toFile();
                            Desktop.getDesktop().open(file);
                        }
                    } else {
                        RequestCallback requestCallback = request -> request.getHeaders()
                                .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

                        ResponseExtractor<Void> responseExtractor = response -> {
                            Files.copy(response.getBody(), path);
                            if (Desktop.isDesktopSupported()) {
                                File file = path.toFile();
                                Desktop.getDesktop().open(file);
                            }
                            return null;
                        };

                        restTemplate.execute(new URI(getHostURL() + "/documents/" + document.get("docStoreId").toString()), HttpMethod.GET, requestCallback, responseExtractor);
                    }
                }
            } catch (URISyntaxException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            } catch (ResourceAccessException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please load a document...");
        }
    }

    private Path getSavePath(String filename) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Choose Save Location");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getPath() + "/" + filename;
            return new File(path).toPath();
        }
        else {
            return null;
        }
    }

    private void deleteDocument() {
        if (document != null && JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the document (this action cannot be undone)?") == 0) {
            undoStates.empty();
            redoStates.empty();
            try {
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {};

                RequestEntity<Void> request = RequestEntity.delete(new URI(getHostURL() + "/documents/" + document.get("id").toString()))
                        .accept(MediaType.APPLICATION_JSON).build();

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                    JOptionPane.showMessageDialog(this, "Document Deleted");
                    setTitleFilename("");
                    document = null;
                    playground.setText("");
                    documentSelector.populate();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete document! Reason: " + response.getStatusCode());
                }

            } catch (URISyntaxException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            } catch (ResourceAccessException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
        }
    }

    private void setTitleFilename(String filename) {
        if (!Strings.isNullOrEmpty(filename)) {
            this.setTitle("Annotation Tool: " + filename);
        } else {
            this.setTitle("Annotation Tool");
        }
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
    private JLabel hostLabel;
    private JTextField host;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton load;
    private javax.swing.JTextPane playground;
    private javax.swing.JButton save;
    private javax.swing.JButton process;
    private javax.swing.JButton download;
    private JComboBox type;
    private JList doccat;
    private JScrollPane doccatPane;
    // End of variables declaration//GEN-END:variables
}
