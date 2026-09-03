package copilot.eclipse.extensions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Reloads extra MCP servers and optionally writes them into Copilot preferences.
 */
public class RefreshMcpHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    CopilotExtensionsPlugin plugin = CopilotExtensionsPlugin.getDefault();
    if (plugin == null) {
      return null;
    }
    plugin.applyFallbackToCopilotPreferences();
    String document = WorkspaceMcpCollector.collectAsDocument();
    Shell shell = HandlerUtil.getActiveShell(event);
    String contributionStatus = plugin.isMcpContributionPointEnabled()
        ? "Copilot MCP contribution point policy is enabled. Approve extra servers in Copilot MCP settings if prompted."
        : "Copilot MCP contribution point policy is currently disabled. Enable the preference fallback or paste the JSON into Copilot MCP settings.";
    MessageDialog.openInformation(shell, "Copilot Extensions",
        contributionStatus + "\n\nDiscovered MCP configuration:\n\n" + document);
    return null;
  }
}
