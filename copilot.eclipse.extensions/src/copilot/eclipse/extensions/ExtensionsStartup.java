package copilot.eclipse.extensions;

import org.eclipse.ui.IStartup;

/**
 * Applies the Copilot preference fallback after the workbench is up.
 */
public class ExtensionsStartup implements IStartup {

  @Override
  public void earlyStartup() {
    CopilotExtensionsPlugin plugin = CopilotExtensionsPlugin.getDefault();
    if (plugin != null) {
      plugin.applyFallbackToCopilotPreferences();
    }
  }
}
