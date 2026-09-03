package copilot.eclipse.extensions.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import copilot.eclipse.extensions.CopilotExtensionsPlugin;

/**
 * Default values for Copilot Extensions preferences.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    CopilotExtensionsPlugin plugin = CopilotExtensionsPlugin.getDefault();
    if (plugin == null) {
      return;
    }
    IPreferenceStore store = plugin.getPreferenceStore();
    store.setDefault(PreferenceConstants.WORKSPACE_DISCOVERY, true);
    store.setDefault(PreferenceConstants.INCLUDE_GITHUB_MCP, false);
    store.setDefault(PreferenceConstants.FALLBACK_WRITE_COPILOT_PREFS, true);
    store.setDefault(PreferenceConstants.EXTRA_SERVERS_JSON, PreferenceConstants.DEFAULT_EXTRA_SERVERS_JSON);
  }
}
