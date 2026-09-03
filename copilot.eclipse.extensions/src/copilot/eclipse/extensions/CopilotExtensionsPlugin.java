package copilot.eclipse.extensions;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.gson.JsonElement;
import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.CopilotUi;

import copilot.eclipse.extensions.preferences.PreferenceConstants;

/**
 * Companion plugin that contributes extra MCP servers to GitHub Copilot for Eclipse.
 */
public class CopilotExtensionsPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "copilot.eclipse.extensions";

  private static CopilotExtensionsPlugin plugin;

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  public static CopilotExtensionsPlugin getDefault() {
    return plugin;
  }

  public static void logError(String message, Throwable throwable) {
    if (plugin == null) {
      return;
    }
    plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, throwable));
  }

  public static void logInfo(String message) {
    if (plugin == null) {
      return;
    }
    plugin.getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
  }

  public CompletableFuture<String> loadMcpDocument() {
    return CompletableFuture.supplyAsync(WorkspaceMcpCollector::collectAsDocument);
  }

  public void applyFallbackToCopilotPreferences() {
    IPreferenceStore store = getPreferenceStore();
    if (!store.getBoolean(PreferenceConstants.FALLBACK_WRITE_COPILOT_PREFS)) {
      return;
    }
    if (CopilotUi.getPlugin() == null) {
      return;
    }
    Map<String, JsonElement> extraServers = McpJson.withManagedPrefix(WorkspaceMcpCollector.collect());
    IPreferenceStore copilotStore = CopilotUi.getPlugin().getPreferenceStore();
    Map<String, JsonElement> existing = McpJson.withoutManagedServers(
        McpJson.extractServers(copilotStore.getString(Constants.MCP)));
    Map<String, JsonElement> merged = McpJson.merge(existing, extraServers);
    copilotStore.setValue(Constants.MCP, McpJson.toServersDocument(merged));
  }

  public boolean isMcpContributionPointEnabled() {
    if (CopilotCore.getPlugin() == null || CopilotCore.getPlugin().getFeatureFlags() == null) {
      return false;
    }
    return CopilotCore.getPlugin().getFeatureFlags().isMcpContributionPointEnabled();
  }
}
