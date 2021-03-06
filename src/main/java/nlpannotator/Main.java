package nlpannotator;

import com.google.common.base.Strings;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
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

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main extends JFrame {
    private JTextPane playground;
    private JButton openButton;
    private JButton saveButton;
    private JButton downloadOriginalButton;
    private JList doccat;
    private JTree typeTree;
    private JPanel mainPanel;
    private JButton findReplaceButton;
    private JButton undoButton;
    private JButton redoButton;
    private JButton historyButton;
    private JButton metadataButton;
    private JButton resetButton;
    private JButton deleteButton;
    private JButton entityAutoDetectionButton;
    private JButton trainNERModelButton;
    private JButton trainDocCatModelButton;
    private JButton showProcessMonitorButton;
    private JButton annotateSingleF1Button;
    private JButton annotateAllF2Button;
    private JButton deleteAnnotationF3Button;
    private JButton deleteAllAnnotationsF4Button;
    private JTextField host;
    private JButton annotationsTrackerButton;
    private JButton dependencyResolverButton;
    private JToolBar topbar;
    private JToolBar midbar;
    private JToolBar lowbar;
    private JToolBar botbar;
    private JScrollPane playgroundScrollPane;
    private JCheckBox includeInNERTrainingCheckBox;
    private JCheckBox includeInNERTestingCheckBox;
    private JButton testNERModelButton;
    private JButton trainW2VModelButton;
    private JButton viewNERCorpusButton;
    private JLabel lblServerConnection;
    private JButton topicsBrowserButton;
    private JButton testDocCatModelButton;

    private RestTemplate restTemplate;
    private ProcessMonitor processMonitor;
    private DocumentSelector documentSelector;
    private HistoryViewer historyViewer;
    private MetadataEditor metadataEditor;
    private AutoDetectionThreshold autoDetectionThreshold;
    private AnnotatedLinesTracker annotatedLinesTracker;
    private TopicsBrowser topicsBrowser;
    private FindAndReplace findAndReplace;
    private DependencyResolverManager resolverMgr;
    private TrainingIterations trainingIterations;

    private SizedStack<String> undoStates;
    private SizedStack<String> redoStates;

    Map document;
    private List<Map<String, Object>> autoAnnotateEntities;
    private DefaultComboBoxModel doccatModel;
    private DefaultTreeModel typeModel;

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        //</editor-fold>

        Main main = new Main();
        EventQueue.invokeLater(() -> {
            main.setVisible(true);
        });
    }

    public Main() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);

        undoStates = new SizedStack<>(10);
        redoStates = new SizedStack<>(10);
        host.setText(Tools.getProperty("restApi.url"));
        playground.setFont(new Font("Segoe UI Symbol", 0, 16));
        TextLineNumber tln = new TextLineNumber(playground);
        playgroundScrollPane.setRowHeaderView(tln);
        topbar.setFloatable(false);
        midbar.setFloatable(false);
        lowbar.setFloatable(false);
        botbar.setFloatable(false);
        resolverMgr = new DependencyResolverManager(this);
        typeTree.setVisible(false);
        populateDocumentCategories();

        new Thread(new Runnable() {
            @Override
            public void run() {
                populateAnnotationTypes();
            }
        }).start();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addEventListeners();
        setTitleFilename(null);
        setContentPane(mainPanel);
        pack();

        setVisible(true);
    }

    private void populateDocumentCategories() {
        doccatModel = new DefaultComboBoxModel();

        doccatModel.addElement("Not_Applicable");
        for (String key : FacilityTypes.dictionary.keySet()) {
            doccatModel.addElement(key);
        }

        doccat.setModel(doccatModel);
    }

    private void populateAnnotationTypes() {
        try {
            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

            RequestEntity<Void> request = RequestEntity.get(new URI(getHostURL() + "/documents/annotationTypes/BaseNode"))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            Map<String, Object> jsonDict = response.getBody();

            Map<String, List<Map<String, Object>>> types = (Map<String, List<Map<String, Object>>>) jsonDict.get("data");

            DefaultMutableTreeNode top = new DefaultMutableTreeNode("BaseNode");
            populateTypeTreeNodes(types, top);

            typeModel = new DefaultTreeModel(top);
            typeTree.setVisible(true);
            typeTree.setModel(typeModel);
            typeTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            typeTree.setRootVisible(false);
            updateServerConnectionLabel(true);
        } catch (URISyntaxException | ResourceAccessException e) {
            updateServerConnectionLabel(false);
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void updateServerConnectionLabel(boolean success) {
        if (success) {
            lblServerConnection.setText("Connected");
            lblServerConnection.setForeground(Color.BLACK);
        } else {
            lblServerConnection.setText("NOT Connected!");
            lblServerConnection.setForeground(Color.RED);
        }
    }

    private void populateTypeTreeNodes(Map<String, List<Map<String, Object>>> types, DefaultMutableTreeNode node) {
        String typeName = node.toString();
        List<Map<String, Object>> childTypes = types.get(typeName);
        for (Map<String, Object> childType : childTypes) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childType.get("name"));
            node.add(childNode);
            populateTypeTreeNodes(types, childNode);
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(5, 2, new Insets(5, 5, 5, 5), -1, -1));
        topbar = new JToolBar();
        mainPanel.add(topbar, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        saveButton = new JButton();
        saveButton.setText("Save");
        topbar.add(saveButton);
        openButton = new JButton();
        openButton.setText("Open");
        topbar.add(openButton);
        resetButton = new JButton();
        resetButton.setText("Reset");
        topbar.add(resetButton);
        deleteButton = new JButton();
        deleteButton.setText("Delete");
        topbar.add(deleteButton);
        final JToolBar.Separator toolBar$Separator1 = new JToolBar.Separator();
        topbar.add(toolBar$Separator1);
        downloadOriginalButton = new JButton();
        downloadOriginalButton.setText("Download Original");
        topbar.add(downloadOriginalButton);
        final JToolBar.Separator toolBar$Separator2 = new JToolBar.Separator();
        topbar.add(toolBar$Separator2);
        findReplaceButton = new JButton();
        findReplaceButton.setText("Find/Replace");
        topbar.add(findReplaceButton);
        undoButton = new JButton();
        undoButton.setText("Undo");
        topbar.add(undoButton);
        redoButton = new JButton();
        redoButton.setText("Redo");
        topbar.add(redoButton);
        final JToolBar.Separator toolBar$Separator3 = new JToolBar.Separator();
        topbar.add(toolBar$Separator3);
        historyButton = new JButton();
        historyButton.setText("History");
        topbar.add(historyButton);
        metadataButton = new JButton();
        metadataButton.setText("Metadata");
        topbar.add(metadataButton);
        final JToolBar.Separator toolBar$Separator4 = new JToolBar.Separator();
        topbar.add(toolBar$Separator4);
        final JToolBar.Separator toolBar$Separator5 = new JToolBar.Separator();
        topbar.add(toolBar$Separator5);
        showProcessMonitorButton = new JButton();
        showProcessMonitorButton.setText("Show Process Monitor");
        topbar.add(showProcessMonitorButton);
        playgroundScrollPane = new JScrollPane();
        mainPanel.add(playgroundScrollPane, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(1200, -1), null, 0, false));
        playground = new JTextPane();
        playground.setEditable(false);
        playgroundScrollPane.setViewportView(playground);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 1, 4, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Annotation Tag:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(405, 16), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Document Categories:");
        panel1.add(label2, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(405, 16), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(405, 128), null, 0, false));
        doccat = new JList();
        scrollPane1.setViewportView(doccat);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel1.add(scrollPane2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(405, 360), null, 0, false));
        typeTree = new JTree();
        scrollPane2.setViewportView(typeTree);
        includeInNERTrainingCheckBox = new JCheckBox();
        includeInNERTrainingCheckBox.setText("Include in NER Model Training");
        panel1.add(includeInNERTrainingCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        includeInNERTestingCheckBox = new JCheckBox();
        includeInNERTestingCheckBox.setText("Include in NER Model Testing");
        panel1.add(includeInNERTestingCheckBox, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        midbar = new JToolBar();
        mainPanel.add(midbar, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        entityAutoDetectionButton = new JButton();
        entityAutoDetectionButton.setText("NER Model Library");
        midbar.add(entityAutoDetectionButton);
        final JToolBar.Separator toolBar$Separator6 = new JToolBar.Separator();
        midbar.add(toolBar$Separator6);
        viewNERCorpusButton = new JButton();
        viewNERCorpusButton.setText("View NER Corpus");
        midbar.add(viewNERCorpusButton);
        final JToolBar.Separator toolBar$Separator7 = new JToolBar.Separator();
        midbar.add(toolBar$Separator7);
        trainW2VModelButton = new JButton();
        trainW2VModelButton.setText("Train W2V Model");
        midbar.add(trainW2VModelButton);
        trainNERModelButton = new JButton();
        trainNERModelButton.setText("Train NER Model");
        midbar.add(trainNERModelButton);
        testNERModelButton = new JButton();
        testNERModelButton.setText("Test NER Model");
        midbar.add(testNERModelButton);
        final JToolBar.Separator toolBar$Separator8 = new JToolBar.Separator();
        midbar.add(toolBar$Separator8);
        trainDocCatModelButton = new JButton();
        trainDocCatModelButton.setText("Train DocCat Model");
        midbar.add(trainDocCatModelButton);
        testDocCatModelButton = new JButton();
        testDocCatModelButton.setText("Test DocCat Model");
        midbar.add(testDocCatModelButton);
        final JToolBar.Separator toolBar$Separator9 = new JToolBar.Separator();
        midbar.add(toolBar$Separator9);
        dependencyResolverButton = new JButton();
        dependencyResolverButton.setText("Dependency Resolver");
        midbar.add(dependencyResolverButton);
        lowbar = new JToolBar();
        mainPanel.add(lowbar, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        annotateSingleF1Button = new JButton();
        annotateSingleF1Button.setText("Annotate Single (F1)");
        lowbar.add(annotateSingleF1Button);
        annotateAllF2Button = new JButton();
        annotateAllF2Button.setText("Annotate All (F2)");
        lowbar.add(annotateAllF2Button);
        final JToolBar.Separator toolBar$Separator10 = new JToolBar.Separator();
        lowbar.add(toolBar$Separator10);
        deleteAnnotationF3Button = new JButton();
        deleteAnnotationF3Button.setText("Delete Annotation (F3)");
        lowbar.add(deleteAnnotationF3Button);
        deleteAllAnnotationsF4Button = new JButton();
        deleteAllAnnotationsF4Button.setText("Delete All Annotations (F4)");
        lowbar.add(deleteAllAnnotationsF4Button);
        final JToolBar.Separator toolBar$Separator11 = new JToolBar.Separator();
        lowbar.add(toolBar$Separator11);
        annotationsTrackerButton = new JButton();
        annotationsTrackerButton.setText("Annotations Tracker");
        lowbar.add(annotationsTrackerButton);
        final JToolBar.Separator toolBar$Separator12 = new JToolBar.Separator();
        lowbar.add(toolBar$Separator12);
        topicsBrowserButton = new JButton();
        topicsBrowserButton.setHideActionText(false);
        topicsBrowserButton.setText("Topics Browser");
        lowbar.add(topicsBrowserButton);
        botbar = new JToolBar();
        mainPanel.add(botbar, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Host URL:");
        botbar.add(label3);
        host = new JTextField();
        botbar.add(host);
        lblServerConnection = new JLabel();
        lblServerConnection.setText("");
        botbar.add(lblServerConnection);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private class PlaygroundKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (document != null) {

                if (e.getKeyCode() == KeyEvent.VK_F1) {
                    Highlighter.Highlight[] highlights = playground.getHighlighter().getHighlights();
                    undoStates.push(playground.getText());
                    //F1 key - add annotation to single element
                    annotateSingle(highlights);
                } else if (e.getKeyCode() == KeyEvent.VK_F2) {
                    Highlighter.Highlight[] highlights = playground.getHighlighter().getHighlights();
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
                } else if ((e.getKeyCode() == KeyEvent.VK_S) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                    saveActionPerformed(null);
                } else if (e.getKeyCode() == KeyEvent.VK_F3) {
                    Highlighter.Highlight[] highlights = playground.getHighlighter().getHighlights();
                    undoStates.push(playground.getText());
                    //F3 Key - remove annotation from single element
                    unannotateSingle(highlights);
                } else if (e.getKeyCode() == KeyEvent.VK_F4) {
                    Highlighter.Highlight[] highlights = playground.getHighlighter().getHighlights();
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

    private void addEventListeners() {
        playground.addKeyListener(new Main.PlaygroundKeyListener());

        host.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {

            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    populateAnnotationTypes();
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        });

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveActionPerformed(actionEvent);
            }
        });

        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                openDocument();
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                resetDocument();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteDocument();
            }
        });

        downloadOriginalButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                downloadActionPerformed(actionEvent);
            }
        });

        findReplaceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                findActionPerformed(null);
            }
        });

        undoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                undo();
            }
        });

        redoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                redo();
            }
        });

        historyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showHistoryViewer();
            }
        });

        metadataButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showMetadataEditor();
            }
        });

        showProcessMonitorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                getProcessMonitor().setVisible(true);
            }
        });

        entityAutoDetectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showAutoDetectionThreshold();
            }
        });

        viewNERCorpusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                viewNERCorpus();
            }
        });

        trainW2VModelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                trainW2VModelActionPerformed(null);
            }
        });

        trainNERModelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                trainNERModelActionPerformed(null);
            }
        });

        testNERModelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                testNERModelActionPerformed(null);
            }
        });

        trainDocCatModelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                trainDoccatModelActionPerformed(null);
            }
        });

        testDocCatModelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                testDoccatModelActionPerformed(null);
            }
        });

        dependencyResolverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                resolverMgr.showDependencyResolver();
            }
        });

        annotateSingleF1Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Highlighter.Highlight[] highlights = playground.getHighlighter().getHighlights();
                undoStates.push(playground.getText());
                //F1 key - add annotation to single element
                annotateSingle(highlights);
            }
        });

        annotateAllF2Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Highlighter.Highlight[] highlights = playground.getHighlighter().getHighlights();
                undoStates.push(playground.getText());
                //F2 key - add annotation to all elements
                annotateMultiple(highlights);
            }
        });

        deleteAnnotationF3Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Highlighter.Highlight[] highlights = playground.getHighlighter().getHighlights();
                undoStates.push(playground.getText());
                //F3 Key - remove annotation from single element
                unannotateSingle(highlights);
            }
        });

        deleteAllAnnotationsF4Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Highlighter.Highlight[] highlights = playground.getHighlighter().getHighlights();
                undoStates.push(playground.getText());
                //F4 Key - remove annotation from all elements
                unannotateMultiple(highlights);
            }
        });

        annotationsTrackerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateAnnotatedLinesList();
            }
        });

        topicsBrowserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateTopicsBrowser();
            }
        });

        typeTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
                highlightActionPerformed();
            }
        });

        includeInNERTrainingCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                handleNERIncludeTrainingCheckboxAction();
            }
        });

        includeInNERTestingCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                handleNERIncludeTestingCheckboxAction();
            }
        });
    }

    private void handleNERIncludeTrainingCheckboxAction() {
        if (includeInNERTrainingCheckBox.isSelected()) {
            includeInNERTestingCheckBox.setSelected(false);
        }
    }

    private void handleNERIncludeTestingCheckboxAction() {
        if (includeInNERTestingCheckBox.isSelected()) {
            includeInNERTrainingCheckBox.setSelected(false);
        }
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

    private void annotateSingle(Highlighter.Highlight[] highlights) {
        for (Highlighter.Highlight highlight : highlights) {
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

    private void annotateMultiple(Highlighter.Highlight[] highlights) {
        for (Highlighter.Highlight highlight : highlights) {
            Document doc = playground.getDocument();
            int start = highlight.getStartOffset();
            int end = highlight.getEndOffset();

            if (validateAnnotation(start, end, doc)) {
                String annotationStart = getAnnotation();
                String annotationEnd = " <END> ";
                try {
                    int caretPos = playground.getCaretPosition();
                    String selectedText = doc.getText(start, end - start);
                    String annotatedText = annotationStart + selectedText + annotationEnd;
                    int annotationStartLength = annotationStart.length();

                    String docText = doc.getText(0, doc.getLength());
                    int selectedIndex = docText.indexOf(selectedText, 0);
                    while (selectedIndex != -1) {
                        if (selectedIndex - annotationStartLength >= 0) {
                            String possibleAnnotationStart = docText.substring(selectedIndex - annotationStartLength, selectedIndex);
                            if (!possibleAnnotationStart.equals(annotationStart)) {
                                docText = docText.substring(0, selectedIndex) + annotatedText + docText.substring(selectedIndex + selectedText.length());
                                selectedIndex = docText.indexOf(selectedText, selectedIndex + annotatedText.length());
                                continue;
                            }
                        } else {
                            docText = docText.substring(0, selectedIndex) + annotatedText + docText.substring(selectedIndex + selectedText.length());
                        }
                        selectedIndex = docText.indexOf(selectedText, selectedIndex + 1);
                    }

                    doc.remove(0, doc.getLength());
                    doc.insertString(0, docText, null);
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

    private void unannotateSingle(Highlighter.Highlight[] highlights) {
        Document doc = playground.getDocument();
        String annotationType = getAnnotationType();
        Pattern annotationPattern = Pattern.compile(" ?<START:" + Pattern.quote(annotationType) + ">.+?<END> ?");

        if (highlights.length > 0) {
            for (Highlighter.Highlight highlight : highlights) {
                int start = highlight.getStartOffset();
                int end = highlight.getEndOffset();

                try {
                    String highlighted = doc.getText(start, (end - start));
                    Matcher annotationMatcher = annotationPattern.matcher(highlighted);
                    if (annotationMatcher.find()) {
                        highlighted = highlighted.replaceAll(" ?<START:" + Pattern.quote(annotationType) + "> ", "");
                        highlighted = highlighted.replaceAll(" <END> ?", "");
                        doc.remove(start, (end - start));
                        doc.insertString(start, highlighted, null);
                    }
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }
        } else {
            try {
                int caret = playground.getCaretPosition();
                int start = Utilities.getRowStart(playground, caret);
                int end = Utilities.getRowEnd(playground, caret);

                String currentLine = doc.getText(start, (end - start));
                Matcher annotationMatcher = annotationPattern.matcher(currentLine);
                while (annotationMatcher.find()) {
                    int lineStart = annotationMatcher.start();
                    int lineEnd = annotationMatcher.end();
                    int lineCaret = caret - start;
                    if (lineStart < lineCaret && lineCaret < lineEnd) { //inside the target annotation
                        String annotatedChunk = currentLine.substring(lineStart, lineEnd);
                        String annotationRemoved = annotatedChunk.replaceAll(" ?<START:" + Pattern.quote(annotationType) + "> ", "");
                        annotationRemoved = annotationRemoved.replaceAll(" <END> ?", "");
                        String updatedLine = currentLine.substring(0, lineStart) + annotationRemoved + currentLine.substring(lineEnd);
                        doc.remove(start, (end - start));
                        doc.insertString(start, updatedLine, null);
                    }
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        AnnotatedLinesTracker tracker = getAnnotatedLinesTracker();
        if (tracker.isVisible()) {
            updateAnnotatedLinesList();
        }
    }

    private void unannotateMultiple(Highlighter.Highlight[] highlights) {
        Document doc = playground.getDocument();
        String annotationType = getAnnotationType();
        Pattern annotationPattern = Pattern.compile(" ?<START:" + Pattern.quote(annotationType) + ">.+?<END> ?");

        if (highlights.length > 0) {
            for (Highlighter.Highlight highlight : highlights) {
                int start = highlight.getStartOffset();
                int end = highlight.getEndOffset();

                try {
                    int caretPos = playground.getCaretPosition();
                    String highlighted = doc.getText(start, (end - start));
                    Matcher annotationMatcher = annotationPattern.matcher(highlighted);
                    if (annotationMatcher.find()) {
                        String unannotated = highlighted.replaceAll(" ?<START:" + Pattern.quote(annotationType) + "> ", "");
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
        } else {
            try {
                int caret = playground.getCaretPosition();
                int start = Utilities.getRowStart(playground, caret);
                int end = Utilities.getRowEnd(playground, caret);

                String currentLine = doc.getText(start, (end - start));
                Matcher annotationMatcher = annotationPattern.matcher(currentLine);
                while (annotationMatcher.find()) {
                    int lineStart = annotationMatcher.start();
                    int lineEnd = annotationMatcher.end();
                    int lineCaret = caret - start;
                    if (lineStart < lineCaret && lineCaret < lineEnd) { //inside the target annotation
                        String annotatedChunk = currentLine.substring(lineStart, lineEnd);
                        String annotationRemoved = annotatedChunk.replaceAll(" ?<START:" + Pattern.quote(annotationType) + "> ", "");
                        annotationRemoved = annotationRemoved.replaceAll(" <END> ?", "");
                        String unannotatedDoc = doc.getText(0, doc.getLength()).replaceAll(annotatedChunk, annotationRemoved);
                        doc.remove(0, doc.getLength());
                        doc.insertString(0, unannotatedDoc, null);
                        playground.setCaretPosition(caret);
                    }
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
        removeHighlights();
        highlightAnnotations();
        highlightFound();
    }

    private void viewNERCorpus() {
        try {
            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

            List categories = doccat.getSelectedValuesList();
            List<String> categoryList = (List<String>) categories.stream().map(category -> "category=" + category.toString()).collect(Collectors.toList());
            String categoryQuery = categoryList.stream().reduce((p1, p2) -> p1 + "&" + p2).orElse("");
            RequestEntity<Void> request = RequestEntity.get(new URI(getHostURL() + "/documents/reviewNERCorpus" + "?" + categoryQuery))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            if (response.getStatusCode() != HttpStatus.OK) {
                JOptionPane.showMessageDialog(this, "Server error has occurred!!");
            } else {
                String corpus = response.getBody().get("data").toString();

                document = new HashMap();
                document.put("category", categories);
                document.put("filename", "NER Training Corpus");
                document.put("annotated", corpus);

                loadDocument(document);
            }
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (ResourceAccessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (HttpServerErrorException e) {
            JOptionPane.showMessageDialog(this, e.getResponseBodyAsString());
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

                    List categories = (List) document.get("category");
                    List<String> categoryList = (List<String>) categories.stream().map(category -> "category=" + category.toString()).collect(Collectors.toList());
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

    private void trainW2VModelActionPerformed(ActionEvent evt) {
        if (document != null) {
            ProcessMonitor procMon = getProcessMonitor();
            procMon.setVisible(true);
            String procId = procMon.addProcess("(" + Instant.now() + ") Training W2V Model");
            new Thread(() -> {
                try {
                    ParameterizedTypeReference<HashMap<String, Object>> responseType =
                            new ParameterizedTypeReference<HashMap<String, Object>>() {
                            };

                    List categories = (List) document.get("category");
                    List<String> categoryList = (List<String>) categories.stream().map(category -> "category=" + category.toString()).collect(Collectors.toList());
                    String categoryQuery = categoryList.stream().reduce((p1, p2) -> p1 + "&" + p2).orElse("");
                    RequestEntity<Void> request = RequestEntity.get(new URI(getHostURL() + "/documents/trainWordToVec" + "?" + categoryQuery + "&doAsync=false"))
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

    private void testNERModelActionPerformed(ActionEvent evt) {
        if (document != null) {
            ProcessMonitor procMon = getProcessMonitor();
            procMon.setVisible(true);
            String procId = procMon.addProcess("(" + Instant.now() + ") Testing NER Model");
            new Thread(() -> {
                try {
                    ParameterizedTypeReference<HashMap<String, Object>> responseType =
                            new ParameterizedTypeReference<HashMap<String, Object>>() {
                            };

                    List categories = (List) document.get("category");
                    List<String> categoryList = (List<String>) categories.stream().map(category -> "category=" + category.toString()).collect(Collectors.toList());
                    String categoryQuery = categoryList.stream().reduce((p1, p2) -> p1 + "&" + p2).orElse("");
                    RequestEntity<Void> request = RequestEntity.get(new URI(getHostURL() + "/documents/testNER" + "?" + categoryQuery))
                            .accept(MediaType.APPLICATION_JSON).build();

                    ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);
                    Map<String, Object> jsonDict = response.getBody();

                    if (response.getStatusCode() != HttpStatus.OK) {
                        JOptionPane.showMessageDialog(this, "Server error has occurred!!");
                    } else {
                        Map<String, Object> results = ((Map<String, Object>) jsonDict.get("data"));
                        for (Object category : categories) {
                            String result = results.get(category).toString();
                            JTextArea resultText = new JTextArea(result);
                            JScrollPane scrollPane = new JScrollPane();
                            scrollPane.setViewportView(resultText);
                            scrollPane.setPreferredSize(new Dimension(2200, 1080));
                            resultText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                            JOptionPane.showMessageDialog(this, scrollPane, "Test Results for " + category + " Model", JOptionPane.INFORMATION_MESSAGE);
                        }
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
        showTrainingIterations();
    }

    private void testDoccatModelActionPerformed(ActionEvent evt) {
        ProcessMonitor procMon = getProcessMonitor();
        procMon.setVisible(true);
        String procId = procMon.addProcess("(" + Instant.now() + ") Testing Doccat Model");
        new Thread(() -> {
            try {
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                RequestEntity<Void> request = RequestEntity.get(new URI(getHostURL() + "/documents/testDoccat"))
                        .accept(MediaType.APPLICATION_JSON).build();

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                if (response.getStatusCode() == HttpStatus.OK) {
                    displayDocCatModelTestResults(response);
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

    public void trainDoccatModel(int iterations) {
        ProcessMonitor procMon = getProcessMonitor();
        procMon.setVisible(true);
        String procId = procMon.addProcess("(" + Instant.now() + ") Training Doccat Model");
        new Thread(() -> {
            try {
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                RequestEntity<Void> request = RequestEntity.get(new URI(getHostURL() + "/documents/trainDoccat?doAsync=false&iterations=" + iterations))
                        .accept(MediaType.APPLICATION_JSON).build();

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                if (response.getStatusCode() == HttpStatus.OK) {
                    displayDocCatModelTestResults(response);
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

    private void displayDocCatModelTestResults(ResponseEntity<HashMap<String, Object>> response) {
        String report = response.getBody().get("data").toString();
        JTextArea resultText = new JTextArea(report);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(resultText);
        scrollPane.setPreferredSize(new Dimension(2200, 1080));
        resultText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JOptionPane pane = new JOptionPane(scrollPane, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION);
        JDialog dialog = pane.createDialog(this, "Doccat Model Test Report");
        dialog.setModal(false);
        dialog.setVisible(true);
    }

    public void autoAnnotateDocument(double threshold, Map<String, String> modelVersion) {
        if (document != null) {
            undoStates.empty();
            redoStates.empty();
            try {
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

                String modelVersionQuery = "&category=&version=";
                if (!modelVersion.keySet().isEmpty()) {
                    String category = modelVersion.keySet().toArray()[0].toString();
                    String version = modelVersion.get(category);
                    modelVersionQuery = "&category=" + category + "&version=" + version;
                }
                RequestEntity<Void> request = RequestEntity.get(new URI(getHostURL() + "/documents/annotate/" + document.get("id").toString() + "?threshold=" + threshold + modelVersionQuery))
                        .accept(MediaType.APPLICATION_JSON).build();

                ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                Map<String, Object> jsonDict = response.getBody();

                Map<String, Object> annotated = (Map<String, Object>) jsonDict.get("data");
                String annotatedText = annotated.keySet().toArray(new String[1])[0];
                autoAnnotateEntities = (List<Map<String, Object>>) annotated.get(annotatedText);

                playground.setText(annotatedText);
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

    public void showAutoDetectionThreshold() {
        if (autoDetectionThreshold == null) {
            autoDetectionThreshold = new AutoDetectionThreshold(this);
        }
        autoDetectionThreshold.ddlCategorySelectionChanged(null);
        autoDetectionThreshold.setVisible(true);
    }

    public void updateMetadata(Map<Object, Object> doc) {
        for (Object key : doc.keySet()) {
            Object val = doc.get(key);
            if (document.containsKey(key)) {
                document.replace(key, val);
            } else {
                document.put(key, val);
            }
        }
    }

    private MetadataEditor getMetadataEditor() {
        if (metadataEditor == null) {
            metadataEditor = new MetadataEditor(this);
        }
        return metadataEditor;
    }

    private void showTrainingIterations() {
        if (trainingIterations == null) {
            trainingIterations = new TrainingIterations(this);
        }
        trainingIterations.setVisible(true);
    }

    public void showMetadataEditor() {
        if (document != null) {
            getMetadataEditor().setVisible(true);
            refreshMetadataEditor();
        }
    }

    public void refreshMetadataEditor() {
        if (getMetadataEditor().isVisible()) {
            getMetadataEditor().populate(document);
        }
    }

    public void refreshTopicsBrowser() {
        if (getTopicsBrowser().isVisible()) {
            getTopicsBrowser().populate(document);
        }
    }

    public void updateAnnotation(String annotated) {
        int caretPos = playground.getCaretPosition();
        document.replace("annotated", annotated);
        loadDocument(document);
        playground.setCaretPosition(caretPos);
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

    private void findActionPerformed(ActionEvent evt) {
        if (document != null) {
            FindAndReplace findAndReplace = getFindAndReplace();
            findAndReplace.setVisible(true);
            Highlighter.Highlight[] highlights = playground.getHighlighter().getHighlights();
            for (Highlighter.Highlight highlight : highlights) {
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
        chooser.setCurrentDirectory(new File("."));
        chooser.setDialogTitle("Choose Save Location");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getPath() + "/" + filename;
            return new File(path).toPath();
        } else {
            return null;
        }
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

    private void deleteDocument() {
        if (document != null && JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the document (this action cannot be undone)?") == 0) {
            undoStates.empty();
            redoStates.empty();
            try {
                ParameterizedTypeReference<HashMap<String, Object>> responseType =
                        new ParameterizedTypeReference<HashMap<String, Object>>() {
                        };

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
            setTitle("Annotation Tool: " + filename);
        } else {
            setTitle("Annotation Tool");
        }
    }

    public void openDocument() {
        if (documentSelector == null) {
            documentSelector = new DocumentSelector(this);
        } else {
            documentSelector.populate();
            documentSelector.setVisible(true);
        }
    }

    public String getHostURL() {
        return host.getText();
    }

    public ProcessMonitor getProcessMonitor() {
        if (processMonitor == null) {
            processMonitor = new ProcessMonitor(this);
        }
        return processMonitor;
    }

    public void clearIfDeleted(String id) {
        if (document != null &&
                document.containsKey("id") &&
                document.get("id").toString().equals(id)) {
            undoStates.empty();
            redoStates.empty();
            document = null;
            playground.setText("");
        }
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
            List categories = (List) document.get("category");
            int[] selections = new int[0];
            for (Object category : categories) {
                int selected = doccatModel.getIndexOf(category);
                selections = ArrayUtils.addAll(selections, new int[]{selected});
            }
            doccat.setSelectedIndices(selections);
        }
        if (document.containsKey("annotated")) {
            playground.setText(document.get("annotated").toString());
        } else if (document.containsKey("parsed")) {
            playground.setText(document.get("parsed").toString());
        } else {
            playground.setText(document.get("docText").toString());
        }
        if (document.containsKey("includeInNERTraining")) {
            includeInNERTrainingCheckBox.setSelected(Boolean.parseBoolean(document.get("includeInNERTraining").toString()));
        } else {
            includeInNERTrainingCheckBox.setSelected(false);
        }
        if (document.containsKey("includeInNERTesting")) {
            includeInNERTestingCheckBox.setSelected(Boolean.parseBoolean(document.get("includeInNERTesting").toString()));
        } else {
            includeInNERTestingCheckBox.setSelected(false);
        }

        playground.setCaretPosition(0);
        this.document = document;
        AnnotatedLinesTracker tracker = getAnnotatedLinesTracker();
        tracker.forgetAutoEntities();
        removeHighlights();
        highlightAnnotations();

        if (findAndReplace != null) {
            findAndReplace.reset();
        }
        if (resolverMgr.isVisible()) {
            resolverMgr.showDependencyResolver();
        }
    }

    private boolean validateForSave() {
        Pattern duplicateTags = Pattern.compile("<START:[\\w\\(\\)]*?>(?=(?:(?!<END>).)*<START:[\\w]*?>)", Pattern.MULTILINE);
        boolean isValid = true;

        try {
            Document doc = playground.getDocument();
            String text = playground.getText(0, doc.getLength());
            Matcher duplicateMatcher = duplicateTags.matcher(text);
            while (duplicateMatcher.find()) {
                String duplicateTag = text.substring(duplicateMatcher.start(), duplicateMatcher.end());
                int len = duplicateTag.length();
                applyTextStyle(duplicateMatcher.start(), len, true, Color.RED, Color.black);
                isValid = false;
            }
            if (isValid) {
                //the last few characters of the document cannot be an <END> tag or else model training fails
                String closeTag = "<END> ";
                int tagLength = closeTag.length();
                if (text.endsWith(closeTag)) {
                    int len = text.length();
                    applyTextStyle(len - tagLength, len, true, Color.RED, Color.black);
                    isValid = false;
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        if (isValid) {
            removeHighlights();
            highlightAnnotations();
            highlightFound();
        } else {
            JOptionPane.showMessageDialog(this, "Please resolve all highlighted annotation tag problems before saving.");
        }

        return isValid;
    }

    private void saveActionPerformed(ActionEvent evt) {
        if (document == null) {
            JOptionPane.showMessageDialog(this, "Please load a document...");
            return;
        }
        if (validateForSave()) {
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
                if (document.containsKey("userCategory")) {
                    document.replace("userCategory", categories);
                } else {
                    document.put("userCategory", categories);
                }

                String annotatedBy = System.getProperty("user.name");
                if (document.containsKey("annotatedBy")) {
                    document.replace("annotatedBy", annotatedBy);
                } else {
                    document.put("annotatedBy", annotatedBy);
                }

                boolean includeInNERTraining = includeInNERTrainingCheckBox.isSelected();
                if (document.containsKey("includeInNERTraining")) {
                    document.replace("includeInNERTraining", includeInNERTraining);
                } else {
                    document.put("includeInNERTraining", includeInNERTraining);
                }

                boolean includeInNERTesting = includeInNERTestingCheckBox.isSelected();
                if (document.containsKey("includeInNERTesting")) {
                    document.replace("includeInNERTesting", includeInNERTesting);
                } else {
                    document.put("includeInNERTesting", includeInNERTesting);
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

                    ParameterizedTypeReference<HashMap<String, Object>> responseType =
                            new ParameterizedTypeReference<HashMap<String, Object>>() {
                            };

                    RequestEntity<Map> request = RequestEntity.put(new URI(getHostURL() + "/documents/metadata/" + document.get("id").toString()))
                            .accept(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                            .body(body);

                    ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

                    if (response.getStatusCode() == HttpStatus.OK) {
                        reloadHistory();
                        refreshMetadataEditor();
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

    private void highlightActionPerformed() {
        if (document != null) {
            removeHighlights();
            highlightAnnotations();
            highlightFound();
        }
    }

    public void highlightAnnotations() {
        highlightText(getAnnotation(), true, Color.BLUE, null);
        String endAnnotation = " <END> ";
        highlightText(endAnnotation, true, Color.RED, null);
        AnnotatedLinesTracker tracker = getAnnotatedLinesTracker();
        if (tracker.isVisible()) {
            updateAnnotatedLinesList();
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

    public void updateAnnotatedLinesList() {
        AnnotatedLinesTracker tracker = getAnnotatedLinesTracker();
        String annotationTarget = "<END>";
        if (tracker.inSelectedAnnotationMode()) {
            annotationTarget = getAnnotation();
        }
        String docText = playground.getText();
        String[] allLines;
        if (docText.contains(System.lineSeparator())) {
            allLines = playground.getText().split(System.lineSeparator());
        } else {
            allLines = playground.getText().split("\n");
        }

        TreeMap<Integer, String> annotatedLines = new TreeMap<>();
        for (int i = 0; i < allLines.length; i++) {
            if (allLines[i].contains(annotationTarget)) {
                annotatedLines.put(i, allLines[i]);
            }
        }

        if (autoAnnotateEntities != null) {
            tracker.acknowledgeAutoEntities(autoAnnotateEntities);
            autoAnnotateEntities = null;
        }
        tracker.update(annotatedLines);
        tracker.setVisible(true);
    }

    private String getAnnotation() {
        String annotationType = getAnnotationType();
        String annotation = " <START:" + annotationType + "> ";

        return annotation;
    }

    private String getAnnotationType() {
        TreePath selectionPath = typeTree.getSelectionPath();
        String annotationType = null;
        if (selectionPath != null) {
            annotationType = selectionPath.getLastPathComponent().toString().replace(" ", "_");
        }
        return annotationType;
    }

    private AnnotatedLinesTracker getAnnotatedLinesTracker() {
        if (annotatedLinesTracker == null) {
            annotatedLinesTracker = new AnnotatedLinesTracker(this);
        }
        return annotatedLinesTracker;
    }

    private void updateTopicsBrowser() {
        TopicsBrowser topicsBrowser = getTopicsBrowser();
        topicsBrowser.populate(document);

        topicsBrowser.setVisible(true);
    }

    private TopicsBrowser getTopicsBrowser() {
        if (topicsBrowser == null) {
            topicsBrowser = new TopicsBrowser(this);
        }
        return topicsBrowser;
    }

    private class DependencyResolverManager {
        private DependencyResolver resolver;
        private Point location;
        private Dimension dim;
        private Main mainUI;

        public DependencyResolverManager(Main mainUI) {
            this.mainUI = mainUI;
            location = mainUI.getLocation();
        }

        public void showDependencyResolver() {
            if (document != null) {
                ProcessMonitor procMon = getProcessMonitor();
                procMon.setVisible(true);
                String procId = procMon.addProcess("(" + Instant.now() + ") Analyzing Dependencies: " + document.get("filename").toString());
                new Thread(() -> {
                    closeDependencyResolver();
                    resolver = new DependencyResolver(mainUI);
                    resolver.setLocation(location);
                    if (dim != null) {
                        resolver.setSize(dim);
                    }
                    resolver.populate(document.get("id").toString());
                    resolver.setVisible(true);
                    procMon.removeProcess(procId);
                }).start();
            } else {
                JOptionPane.showMessageDialog(mainUI, "Please load a document...");
            }
        }

        public void closeDependencyResolver() {
            if (resolver != null) {
                if (resolver.isVisible()) {
                    location = resolver.getLocationOnScreen();
                    dim = resolver.getSize();
                }
                resolver.setVisible(false);
                resolver.dispose();
                resolver = null;
            }
        }

        public boolean isVisible() {
            if (resolver != null) {
                return resolver.isVisible();
            } else {
                return false;
            }
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
            playground.grabFocus();
            playground.setCaretPosition(index);
            playground.select(index, index + line.length());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void navigateToLine(int start, int end) {
        RXTextUtilities.gotoStartOfLine(playground, start);
        int charStart = playground.getCaretPosition();
        RXTextUtilities.gotoStartOfLine(playground, end + 1);
        int charEnd = playground.getCaretPosition();
        RXTextUtilities.gotoStartOfLine(playground, start);
        RXTextUtilities.centerLineInScrollPane(playground);

        playground.grabFocus();
        playground.select(charStart, charEnd);
    }

    public void deleteAnnotation(String annotation, String type) {
        int caretPos = playground.getCaretPosition();
        String docText = playground.getText();
        String annotationText = " <START:" + type + "> " + annotation + " <END> ";
        docText = docText.replace(annotationText, annotation);
        playground.setText(docText);
        playground.setCaretPosition(caretPos);
        removeHighlights();
        highlightAnnotations();
        highlightFound();
    }

    public void findMoreLikeThisAnnotation(String annotation) {
        FindAndReplace findAndReplace = getFindAndReplace();
        findAndReplace.setVisible(true);
        List<String> wordsList = Arrays.asList(annotation.toLowerCase().split(" "));
        findAndReplace.loadSearchTerms(wordsList);
    }

    private void highlightFound() {
        if (findAndReplace != null) {
            findAndReplace.valueChanged(null);
        }
    }

    public void highlightFound(TreeMap<Integer, String> locMap) {
        if (locMap != null) {
            highlightText(locMap, true, Color.GREEN, Color.BLACK);
        }
    }

    private FindAndReplace getFindAndReplace() {
        if (findAndReplace == null) {
            findAndReplace = new FindAndReplace("FindAndReplace", this);
            findAndReplace.init();
        }
        return findAndReplace;
    }

    public void replaceAllText(List selections, String replace) {
        undoStates.push(playground.getText());
        Document doc = playground.getDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            for (Object selection : selections) {
                text = text.replaceAll((String) selection, replace);
            }
            playground.setText(text);
            //highlightAnnotations();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public List getDocumentCategories() {
        List categories = doccat.getSelectedValuesList();

        return categories;
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

}
