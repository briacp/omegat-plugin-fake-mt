package net.briac.omegat;

import java.awt.GridBagConstraints;
import java.awt.Window;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import java.io.StringReader;
import java.io.InputStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;


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
    protected static final String PARAM_FORMAT = "fakemt.result.format";
    protected static final String PARAM_FORMAT_DEFAULT = "json";
    protected static final String PARAM_EXPR = "fakemt.result.expr";
    protected static final String PARAM_EXPR_DEFAULT = "$.translation";

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

        String tr = Preferences.getPreferenceDefault(PARAM_FORMAT, "json").equals("json") ? getJsonResults(v) : getXmlResults(v);

        if (tr == null) {
            return "";
        }

        putToCache(sLang, tLang, text, tr);
        return tr;
    }

    private static final Invocable jsonParser;
    static {
        ScriptEngine jsEngine = null;
        try {
            jsEngine = new ScriptEngineManager().getEngineByName("javascript");
            jsEngine.eval(new InputStreamReader(FakeMT.class.getResourceAsStream("/net/briac/omegat/path.js")));
            jsEngine.eval("function parse(json,expr) { return Java.asJSONCompatible(jsonPath(JSON.parse(json),expr)[0]) }");
        } catch (ScriptException e) {
            Logger.getLogger(FakeMT.class.getName()).log(Level.SEVERE, "Unable to initialize JSON parser", e);
        } finally {
            jsonParser = (Invocable) jsEngine;
        }
    }

    @SuppressWarnings("unchecked")
    protected String getJsonResults(String json) {
        try {
            return jsonParser.invokeFunction("parse", json, Preferences.getPreferenceDefault(PARAM_EXPR, PARAM_EXPR_DEFAULT)).toString();
        } catch (Exception e) {
            Log.logErrorRB(e, "MT_JSON_ERROR");
            return OStrings.getString("MT_JSON_ERROR");
        }
    }

    @SuppressWarnings("unchecked")
    protected String getXmlResults(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            XPathFactory xpathfactory = XPathFactory.newInstance();
            XPath xpath = xpathfactory.newXPath();
            XPathExpression expr = xpath.compile(Preferences.getPreferenceDefault(PARAM_EXPR, PARAM_EXPR_DEFAULT));
            return expr.evaluate(doc, XPathConstants.STRING).toString();
        } catch (Exception e) {
            Log.logErrorRB(e, "MT_JSON_ERROR");
            return OStrings.getString("MT_JSON_ERROR");
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
        
        // Format parameter
        JLabel resultFormatLabel = new JLabel("Result format:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        mtPanel.add(resultFormatLabel, gridBagConstraints);

        JPanel pFormats = new JPanel();
        JRadioButton jsonBox = new JRadioButton("JSON");
        jsonBox.setSelected(Preferences.getPreferenceDefault(PARAM_FORMAT, "json").equals("json"));
        pFormats.add(jsonBox);
        JRadioButton xmlBox = new JRadioButton("XML");
        xmlBox.setSelected(Preferences.getPreferenceDefault(PARAM_FORMAT, "json").equals("xml"));
        pFormats.add(xmlBox);
        ButtonGroup gFormats = new ButtonGroup();
        gFormats.add(jsonBox);
        gFormats.add(xmlBox);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        paramSourceLabel.setLabelFor(paramSourceField);
        mtPanel.add(pFormats, gridBagConstraints);
        uiRow++;

        // Text Parameter
        JLabel exprLabel = new JLabel(jsonBox.isSelected() ? "JSONPath expression:" : "XPath expression:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        mtPanel.add(exprLabel, gridBagConstraints);
        jsonBox.addChangeListener(ev -> exprLabel.setText(jsonBox.isSelected() ? "JSONPath expression:" : "XPath expression:"));

        JTextField exprField = new JTextField(Preferences.getPreferenceDefault(PARAM_EXPR, PARAM_EXPR_DEFAULT));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        paramSourceLabel.setLabelFor(paramSourceField);
        mtPanel.add(exprField, gridBagConstraints);
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

                System.setProperty(PARAM_FORMAT, xmlBox.isSelected() ? "xml" : "json");
                Preferences.setPreference(PARAM_FORMAT, xmlBox.isSelected() ? "xml" : "json");

                System.setProperty(PARAM_EXPR, exprField.getText());
                Preferences.setPreference(PARAM_EXPR, exprField.getText());

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
