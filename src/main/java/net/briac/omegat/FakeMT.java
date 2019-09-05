package net.briac.omegat;

import java.awt.GridBagConstraints;
import java.awt.Window;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.omegat.core.CoreEvents;
import org.omegat.core.events.IApplicationEventListener;
import org.omegat.core.machinetranslators.BaseTranslate;
import org.omegat.core.machinetranslators.MachineTranslators;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.JsonParser;
import org.omegat.util.Language;
import org.omegat.util.Log;
import org.omegat.util.OStrings;
import org.omegat.util.Preferences;
import org.omegat.util.WikiGet;

public class FakeMT  extends BaseTranslate {

    protected static final String ALLOW_FAKEMT = "allow_fakemt";
    
    protected static final String PARAM_URL = "fakemt.url";
    protected static final String PARAM_URL_DEFAULT = "http://localhost:8877/translate";
    protected static final String PARAM_NAME = "fakemt.name";
    protected static final String PARAM_NAME_DEFAULT = "Fake MT";
    protected static final String PARAM_SOURCE = "fakemt.query.source";
    protected static final String PARAM_SOURCE_DEFAULT = "source";
    protected static final String PARAM_TARGET = "fakemt.query.target";
    protected static final String PARAM_TARGET_DEFAULT = "target";
    protected static final String PARAM_TEXT = "fakemt.query.text";
    protected static final String PARAM_TEXT_DEFAULT = "text";

    // Plugin setup
    public static void loadPlugins() {
        CoreEvents.registerApplicationEventListener(new IApplicationEventListener() {
            @Override
            public void onApplicationStartup() {
                MachineTranslators.add(new FakeMT());
            }

            @Override
            public void onApplicationShutdown() {
                /* empty */
            }
        });
    }

    public static void unloadPlugins() {
        /* empty */
    }

    
    @Override
    protected String getPreferenceName() {
        return ALLOW_FAKEMT;
    }

    @Override
    public String getName() {
        return Preferences.getPreferenceDefault(PARAM_NAME, PARAM_NAME_DEFAULT);
    }

    @Override
    protected String translate(Language sLang, Language tLang, String text) throws Exception {
        Map<String, String> params = new TreeMap<String, String>();

        params.put(Preferences.getPreferenceDefault(PARAM_TEXT, PARAM_TEXT_DEFAULT), text);
        params.put(Preferences.getPreferenceDefault(PARAM_SOURCE, PARAM_SOURCE_DEFAULT), sLang.getLanguage());
        params.put(Preferences.getPreferenceDefault(PARAM_TARGET, PARAM_TARGET_DEFAULT), tLang.getLanguage());

        Map<String, String> headers = new TreeMap<String, String>();

        String v;
        try {
            v = WikiGet.get(Preferences.getPreferenceDefault(PARAM_URL, PARAM_URL_DEFAULT), params, headers, "UTF-8");
        } catch (IOException e) {
            return e.getLocalizedMessage();
        }

        String tr = getJsonResults(v);

        if (tr == null) {
            return "";
        }

        putToCache(sLang, tLang, text, tr);
        return tr;
    }

    @SuppressWarnings("unchecked")
    protected String getJsonResults(String json) {
        Map<String, Object> rootNode;
        try {
            rootNode = (Map<String, Object>) JsonParser.parse(json);
        } catch (Exception e) {
            Log.logErrorRB(e, "MT_JSON_ERROR");
            return OStrings.getString("MT_JSON_ERROR");
        }

        // { "translation": "Hello World!" } 
        try {
            return rootNode.get("translation").toString();
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public void showConfigurationUI(Window parent) {
        JPanel mtPanel = new JPanel();
        mtPanel.setLayout(new java.awt.GridBagLayout());
        mtPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 15, 0));
        mtPanel.setAlignmentX(0.0F);

        GridBagConstraints gridBagConstraints = null;

        int uiRow = 0;
        
        // MT Name
        JLabel nameLabel = new JLabel("Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        mtPanel.add(nameLabel, gridBagConstraints);

        JTextField nameField = new JTextField(Preferences.getPreferenceDefault(PARAM_NAME, PARAM_URL_DEFAULT));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        nameLabel.setLabelFor(nameField);
        mtPanel.add(nameField, gridBagConstraints);
        uiRow++;
        
        // MT URL
        JLabel urlLabel = new JLabel("URL:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        mtPanel.add(urlLabel, gridBagConstraints);

        JTextField urlField = new JTextField(Preferences.getPreferenceDefault(PARAM_URL, PARAM_URL_DEFAULT));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        urlLabel.setLabelFor(urlField);
        mtPanel.add(urlField, gridBagConstraints);
        uiRow++;

        // Source Parameter
        JLabel paramSourceLabel = new JLabel("Source Parameter:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        mtPanel.add(paramSourceLabel, gridBagConstraints);

        JTextField paramSourceField = new JTextField(Preferences.getPreferenceDefault(PARAM_SOURCE, PARAM_SOURCE_DEFAULT));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        paramSourceLabel.setLabelFor(paramSourceField);
        mtPanel.add(paramSourceField, gridBagConstraints);
        uiRow++;

        // Target Parameter
        JLabel paramTargetLabel = new JLabel("Target Parameter:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        mtPanel.add(paramTargetLabel, gridBagConstraints);

        JTextField paramTargetField = new JTextField(Preferences.getPreferenceDefault(PARAM_TARGET, PARAM_TARGET_DEFAULT));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        paramSourceLabel.setLabelFor(paramSourceField);
        mtPanel.add(paramTargetField, gridBagConstraints);
        uiRow++;

        // Text Parameter
        JLabel paramTextLabel = new JLabel("Text Parameter:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        mtPanel.add(paramTextLabel, gridBagConstraints);

        JTextField paramTextField = new JTextField(Preferences.getPreferenceDefault(PARAM_TEXT, PARAM_TEXT_DEFAULT));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        paramSourceLabel.setLabelFor(paramSourceField);
        mtPanel.add(paramTextField, gridBagConstraints);
        uiRow++;
        
        
        MTConfigDialog dialog = new MTConfigDialog(parent, getName()) {
            @Override
            protected void onConfirm() {
                System.setProperty(PARAM_NAME, nameField.getText());
                Preferences.setPreference(PARAM_NAME, nameField.getText());

                System.setProperty(PARAM_URL, urlField.getText());
                Preferences.setPreference(PARAM_URL, urlField.getText());
                
                System.setProperty(PARAM_TEXT, paramTextField.getText());
                Preferences.setPreference(PARAM_TEXT, paramTextField.getText());
                
                System.setProperty(PARAM_SOURCE, paramSourceField.getText());
                Preferences.setPreference(PARAM_SOURCE, paramSourceField.getText());
                
                System.setProperty(PARAM_TARGET, paramTargetField.getText());
                Preferences.setPreference(PARAM_TARGET, paramTargetField.getText());
            }
        };

        dialog.panel.add(mtPanel);

        dialog.show();
    }

}
