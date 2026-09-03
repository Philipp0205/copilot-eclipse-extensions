package copilot.eclipse.extensions.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import copilot.eclipse.extensions.CopilotExtensionsPlugin;
import copilot.eclipse.extensions.McpJson;

/**
 * Preference page for extra Copilot MCP integrations that live outside the official plugin.
 */
public class ExtensionsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  private JsonFieldEditor extraServersEditor;

  public ExtensionsPreferencePage() {
    super(GRID);
    setPreferenceStore(CopilotExtensionsPlugin.getDefault().getPreferenceStore());
    setDescription("Extra Copilot MCP servers maintained as a companion plugin, not part of the official GitHub Copilot feature.");
  }

  @Override
  public void init(IWorkbench workbench) {
  }

  @Override
  protected void createFieldEditors() {
    Composite parent = getFieldEditorParent();
    Label status = new Label(parent, SWT.WRAP);
    status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    boolean contributionEnabled = CopilotExtensionsPlugin.getDefault().isMcpContributionPointEnabled();
    status.setText(contributionEnabled
        ? "Copilot MCP contribution point policy is enabled. Extra servers are offered through the official extension point."
        : "Copilot MCP contribution point policy is disabled. Use the preference fallback below so servers still reach Copilot.");

    addField(new BooleanFieldEditor(PreferenceConstants.WORKSPACE_DISCOVERY,
        "Discover MCP configs from workspace files (.vscode/mcp.json, .cursor/mcp.json, mcp.json)", parent));
    addField(new BooleanFieldEditor(PreferenceConstants.INCLUDE_GITHUB_MCP,
        "Include the GitHub remote MCP server (https://api.githubcopilot.com/mcp/)", parent));
    addField(new BooleanFieldEditor(PreferenceConstants.FALLBACK_WRITE_COPILOT_PREFS,
        "Write discovered servers into GitHub Copilot MCP preferences (prefix: copilot-ext.)", parent));

    extraServersEditor = new JsonFieldEditor(PreferenceConstants.EXTRA_SERVERS_JSON, "Extra MCP servers JSON:", parent);
    addField(extraServersEditor);
  }

  @Override
  public boolean performOk() {
    boolean ok = super.performOk();
    if (ok) {
      CopilotExtensionsPlugin.getDefault().applyFallbackToCopilotPreferences();
    }
    return ok;
  }

  private static final class JsonFieldEditor extends org.eclipse.jface.preference.StringFieldEditor {
    JsonFieldEditor(String name, String label, Composite parent) {
      super(name, label, UNLIMITED, 12, VALIDATE_ON_KEY_STROKE, parent);
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
      super.doFillIntoGrid(parent, numColumns);
      getTextControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    @Override
    protected boolean doCheckState() {
      String value = getStringValue();
      if (value == null || value.isBlank()) {
        return true;
      }
      if (McpJson.parseObject(value) == null) {
        setErrorMessage("Extra MCP JSON is not a valid object.");
        return false;
      }
      return true;
    }
  }
}
