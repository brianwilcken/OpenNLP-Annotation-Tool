package nlpannotator;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependencyResolver extends JFrame {
    private JComboBox ddlDependency;
    private JLabel txtProviding;
    private JLabel txtDependent;
    private JTable tblProviding;
    private JTable tblDependent;
    private JLabel txtRelation;
    private JButton commitButton;
    private JButton ignoreButton;
    private JPanel panel1;
    private JComboBox ddlAssets;

    private Main annotatorUI;
    private RestTemplate restTemplate;
    private List<Map<String, Object>> dependencies;
    private List<Map<String, Object>> assets;
    private Map<String, Object> selectedDependency;
    private String docId;

    private DefaultComboBoxModel ddlDependenciesModel;
    private DefaultComboBoxModel ddlAssetsModel;
    private DefaultTableModel tblProvidingModel;
    private DefaultTableModel tblDependentModel;

    public DependencyResolver(Main annotatorUI) {
        this.annotatorUI = annotatorUI;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);

        ignoreButton.setVisible(false);
        commitButton.setVisible(false);
        initTableModel();
        initExchangedAssets();

        addEventListeners();
        setTitle("Resolve Extracted Dependencies");
        setLocation(annotatorUI.getLocationOnScreen());
        setContentPane(panel1);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        pack();
    }

    private void initTableModel() {
        tblProvidingModel = new DefaultTableModel();
        tblProvidingModel.addColumn("UUID");
        tblProvidingModel.addColumn("Probability");
        tblProvidingModel.addColumn("AHA Entry");
        tblProvidingModel.addColumn("Type");
        tblProvidingModel.addColumn("Location");
        tblProvidingModel.addColumn("Confidence");

        tblDependentModel = new DefaultTableModel();
        tblDependentModel.addColumn("UUID");
        tblDependentModel.addColumn("Probability");
        tblDependentModel.addColumn("AHA Entry");
        tblDependentModel.addColumn("Type");
        tblDependentModel.addColumn("Location");
        tblDependentModel.addColumn("Confidence");

        tblProviding.setModel(tblProvidingModel);
        tblDependent.setModel(tblDependentModel);

        tblProviding.getColumn("UUID").setMaxWidth(0);
        tblProviding.getColumn("UUID").setMinWidth(0);
        tblProviding.getColumn("UUID").setResizable(false);
        tblProviding.getColumn("Probability").setMaxWidth(0);
        tblProviding.getColumn("Probability").setMinWidth(0);
        tblProviding.getColumn("Probability").setResizable(false);
        tblProviding.setDefaultEditor(Object.class, null);
        tblProviding.setAutoCreateRowSorter(true);
        tblProviding.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        tblDependent.getColumn("UUID").setMaxWidth(0);
        tblDependent.getColumn("UUID").setMinWidth(0);
        tblDependent.getColumn("UUID").setResizable(false);
        tblDependent.getColumn("Probability").setMaxWidth(0);
        tblDependent.getColumn("Probability").setMinWidth(0);
        tblDependent.getColumn("Probability").setResizable(false);
        tblDependent.setDefaultEditor(Object.class, null);
        tblDependent.setAutoCreateRowSorter(true);
        tblDependent.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    private void initExchangedAssets() {
        try {
            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

            RequestEntity<Void> request = RequestEntity.get(new URI(annotatorUI.getHostURL() + "/documents/dependencies/relations"))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            Map<String, Object> jsonDict = response.getBody();

            assets = ((List<Map<String, Object>>) jsonDict.get("data"));

            populateAssetsDropDownList();
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(annotatorUI, e.getMessage());
        }
    }

    public void populate(String docId) {
        this.docId = docId;
        try {
            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

            RequestEntity<Void> request = RequestEntity.get(new URI(annotatorUI.getHostURL() + "/documents/dependencies/" + docId))
                    .accept(MediaType.APPLICATION_JSON).build();

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            Map<String, Object> jsonDict = response.getBody();

            dependencies = ((List<Map<String, Object>>) jsonDict.get("data"));

            populateDependenciesDropDownList();
            ddlDependenciesSelectionChanged(null);
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(annotatorUI, e.getMessage());
        }
    }

    private void populateAssetsDropDownList() {
        ddlAssetsModel = new DefaultComboBoxModel();

        ddlAssetsModel.addElement("-- Please Choose --");
        for (Map<String, Object> asset : assets) {
            String element = asset.get("name").toString();
            ddlAssetsModel.addElement(element);
        }

        ddlAssets.setModel(ddlAssetsModel);
    }

    private void populateDependenciesDropDownList() {
        ddlDependenciesModel = new DefaultComboBoxModel();

        ddlDependenciesModel.addElement("-- Please Choose --");
        for (Map<String, Object> dependency : dependencies) {
            Map<String, Object> providingFacility = (Map<String, Object>) dependency.get("providingFacility");
            Map<String, Object> dependentFacility = (Map<String, Object>) dependency.get("dependentFacility");
            String relation = dependency.get("relation").toString();
            String providingFacilityName = providingFacility.get("name").toString();
            String dependentFacilityName = dependentFacility.get("name").toString();
            String element = providingFacilityName + " -> " + relation + " -> " + dependentFacilityName;
            boolean committed = (boolean) dependency.get("committed");
            if (committed) {
                element += " (COMMITTED)";
            }
            ddlDependenciesModel.addElement(element);
        }

        ddlDependency.setModel(ddlDependenciesModel);
    }

    private void clearDependenciesTable(DefaultTableModel model) {
        for (int r = model.getRowCount() - 1; r >= 0; r--) {
            model.removeRow(r);
        }
    }

    private void populateDependenciesTable(Map<String, Object> facility, DefaultTableModel model) {
        clearDependenciesTable(model);

        Map<String, Object> possibleMatches = (Map<String, Object>) facility.get("possibleMatches");
        for (String probability : possibleMatches.keySet()) {
            Map<String, Object> possibleMatch = (Map<String, Object>) possibleMatches.get(probability);
            String UUID = possibleMatch.get("uuid").toString();
            String matchName = possibleMatch.get("name").toString();
            String type = possibleMatch.get("dataModelNode").toString();
            String location = formLocationText(possibleMatch);
            double degOfConfidence = Double.parseDouble(probability) * 100;
            String confidenceScore = String.format("%.1f", degOfConfidence) + "%";
            model.addRow(new Object[]{UUID, probability, matchName, type, location, confidenceScore});
        }
    }

    private void addEventListeners() {
        ddlDependency.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ddlDependenciesSelectionChanged(actionEvent);
            }
        });

        commitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                commit();
            }
        });

        ignoreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ignore();
            }
        });
    }

    private void ddlDependenciesSelectionChanged(ActionEvent actionEvent) {
        ddlAssets.setSelectedIndex(0);
        selectedDependency = null;
        int depIndex = ddlDependency.getSelectedIndex() - 1;
        ignoreButton.setVisible(false);
        commitButton.setVisible(false);
        if (depIndex >= 0) {
            selectedDependency = dependencies.get(depIndex);
            Map<String, Object> providingFacility = (Map<String, Object>) selectedDependency.get("providingFacility");
            Map<String, Object> dependentFacility = (Map<String, Object>) selectedDependency.get("dependentFacility");

            formDependencyText(providingFacility, txtProviding);
            formDependencyText(dependentFacility, txtDependent);
            txtRelation.setText(selectedDependency.get("relation").toString());

            populateDependenciesTable(providingFacility, tblProvidingModel);
            populateDependenciesTable(dependentFacility, tblDependentModel);

            boolean committed = (boolean) selectedDependency.get("committed");
            if (!committed) {
                ignoreButton.setVisible(true);
                commitButton.setVisible(true);
            } else {
                setSelectedAsset();
                setSelectedFacility("committedProvidingUUID", tblProviding);
                setSelectedFacility("committedDependentUUID", tblDependent);
            }
        } else {
            txtProviding.setText("");
            txtDependent.setText("");
            txtRelation.setText("");

            clearDependenciesTable(tblProvidingModel);
            clearDependenciesTable(tblDependentModel);
        }
    }

    private void formDependencyText(Map<String, Object> facility, JLabel lbl) {
        StringBuilder txt = new StringBuilder();
        if (facility.get("name") != null) {
            txt.append(facility.get("name").toString());
        }

        if (facility.get("dataModelNode") != null) {
            txt.append(" (" + facility.get("dataModelNode").toString() + ") @ ");
        }

        txt.append(formLocationText(facility));

        lbl.setText(txt.toString());
    }

    private String formLocationText(Map<String, Object> facility) {
        StringBuilder txt = new StringBuilder();

        if (facility.get("city") != null) {
            txt.append(facility.get("city").toString());
            if (facility.get("county") != null) {
                txt.append(", ");
            }
        }

        if (facility.get("county") != null) {
            txt.append(facility.get("county").toString());
            if (facility.get("state") != null) {
                txt.append(", ");
            }
        }

        if (facility.get("state") != null) {
            txt.append(facility.get("state").toString());
        }

        return txt.toString();
    }

    private void setSelectedAsset() {
        String assetUUID = selectedDependency.get("dependencyTypeId").toString();
        for (Map<String, Object> asset : assets) {
            String UUID = asset.get("uuid").toString();
            if (assetUUID.equals(UUID)) {
                String name = asset.get("name").toString();
                ddlAssets.setSelectedItem(name);
                break;
            }
        }
    }

    private void setSelectedFacility(String committedTarget, JTable tbl) {
        String targetUUID = selectedDependency.get(committedTarget).toString();
        int rows = tbl.getRowCount();
        for (int r = 0; r < rows; r++) {
            String uuid = tbl.getValueAt(r, 0).toString();
            if (targetUUID.equals(uuid)) {
                tbl.setRowSelectionInterval(r, r);
            }
        }
    }

    private void commit() {
        if (selectedDependency == null) {
            JOptionPane.showMessageDialog(this, "Select a Dependency...");
        }
        if (ddlAssets.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Select an Asset...");
        }
        Map<String, Object> committedDependency = new HashMap<>();

        Map<String, Object> asset = assets.get(ddlAssets.getSelectedIndex() - 1);

        Map<String, Object> providingFacility = (Map<String, Object>) selectedDependency.get("providingFacility");
        Map<String, Object> dependentFacility = (Map<String, Object>) selectedDependency.get("dependentFacility");

        if (tblProviding.getSelectedRow() != -1) {
            setNominatedFacility(committedDependency, tblProviding, providingFacility, "providingFacility");
        } else {
            committedDependency.put("providingFacility", providingFacility);
            committedDependency.put("addNewProvider", true);
        }

        if (tblDependent.getSelectedRow() != -1) {
            setNominatedFacility(committedDependency, tblDependent, dependentFacility, "dependentFacility");
        } else {
            committedDependency.put("dependentFacility", dependentFacility);
            committedDependency.put("addNewDependent", true);
        }
        committedDependency.put("assetUUID", asset.get("uuid"));
        committedDependency.put("relationState", "COMMITTED");
        committedDependency.put("relationId", selectedDependency.get("relationId"));
        committedDependency.put("docId", annotatorUI.document.get("id"));
        fixUUID(committedDependency);

        commitDependency(committedDependency);
    }

    private void ignore() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to ignore this relation?") == 0) {
            Map<String, Object> committedDependency = new HashMap<>();
            committedDependency.put("relationState", "IGNORED");
            committedDependency.put("relationId", selectedDependency.get("relationId"));
            committedDependency.put("docId", annotatorUI.document.get("id"));

            commitDependency(committedDependency);
        }
    }

    private void commitDependency(Map<String, Object> committedDependency) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("dependency", committedDependency);

            ParameterizedTypeReference<HashMap<String, Object>> responseType =
                    new ParameterizedTypeReference<HashMap<String, Object>>() {
                    };

            RequestEntity<Map> request = RequestEntity.put(new URI(annotatorUI.getHostURL() + "/documents/dependencies"))
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                    .body(body);

            ResponseEntity<HashMap<String, Object>> response = restTemplate.exchange(request, responseType);

            if (response.getStatusCode() == HttpStatus.OK) {
                JOptionPane.showMessageDialog(this, "Relation Committed");
                populate(this.docId);
            } else {
                JOptionPane.showMessageDialog(this, "COMMIT FAILURE!!!");
            }

        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (ResourceAccessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void fixUUID(Map<String, Object> committedDependency) {
        Map<String, Object> providingFacility = (Map<String, Object>) committedDependency.get("providingFacility");
        Map<String, Object> dependentFacility = (Map<String, Object>) committedDependency.get("dependentFacility");

        String uuid1 = providingFacility.get("uuid").toString();
        providingFacility.put("UUID", uuid1);

        String uuid2 = dependentFacility.get("uuid").toString();
        dependentFacility.put("UUID", uuid2);
    }

    private void setNominatedFacility(Map<String, Object> committedDependency, JTable table, Map<String, Object> facility, String targetFacility) {
        int row = table.getSelectedRow();
        String probability = table.getValueAt(row, 1).toString();
        Map<String, Object> possibleMatches = (Map<String, Object>) facility.get("possibleMatches");
        Map<String, Object> nominatedProvider = (Map<String, Object>) possibleMatches.get(probability);
        committedDependency.put(targetFacility, nominatedProvider);
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
        panel1.setLayout(new GridLayoutManager(6, 10, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Select a Dependency:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Providing Facility:");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ddlDependency = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        ddlDependency.setModel(defaultComboBoxModel1);
        panel1.add(ddlDependency, new GridConstraints(0, 2, 1, 8, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(2, 0, 1, 6, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(768, -1), new Dimension(768, -1), null, 0, false));
        tblProviding = new JTable();
        scrollPane1.setViewportView(tblProviding);
        txtProviding = new JLabel();
        txtProviding.setText("");
        panel1.add(txtProviding, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel1.add(scrollPane2, new GridConstraints(2, 6, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(768, -1), new Dimension(768, -1), null, 0, false));
        tblDependent = new JTable();
        scrollPane2.setViewportView(tblDependent);
        final JLabel label3 = new JLabel();
        label3.setText("Dependent Facility:");
        panel1.add(label3, new GridConstraints(1, 6, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtDependent = new JLabel();
        txtDependent.setText("");
        panel1.add(txtDependent, new GridConstraints(1, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        commitButton = new JButton();
        commitButton.setText("Commit");
        panel1.add(commitButton, new GridConstraints(5, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ignoreButton = new JButton();
        ignoreButton.setText("Ignore");
        panel1.add(ignoreButton, new GridConstraints(5, 5, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(5, 7, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(1, 2, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel1.add(spacer3, new GridConstraints(5, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel1.add(spacer4, new GridConstraints(1, 9, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        ddlAssets = new JComboBox();
        panel1.add(ddlAssets, new GridConstraints(4, 4, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Asset Exchanged:");
        panel1.add(label4, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel1.add(spacer5, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Relationship:");
        panel1.add(label5, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel1.add(spacer6, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        txtRelation = new JLabel();
        txtRelation.setText("");
        panel1.add(txtRelation, new GridConstraints(3, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}
