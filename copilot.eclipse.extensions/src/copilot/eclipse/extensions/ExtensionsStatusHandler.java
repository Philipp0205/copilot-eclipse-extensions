package copilot.eclipse.extensions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;

import copilot.eclipse.extensions.sessions.CopilotSessions;

/**
 * Reports what this companion plugin has detected, so a user can tell whether the extra features are
 * actually wired up to their Copilot installation.
 */
public class ExtensionsStatusHandler extends AbstractHandler {

  private static final String COPILOT_CORE_BUNDLE = "com.microsoft.copilot.eclipse.core";
  private static final String COPILOT_UI_BUNDLE = "com.microsoft.copilot.eclipse.ui";

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    StringBuilder report = new StringBuilder();
    report.append("Copilot Extensions: ").append(bundleVersion(CopilotExtensionsPlugin.PLUGIN_ID))
        .append('\n');
    report.append("Copilot core: ").append(bundleVersion(COPILOT_CORE_BUNDLE)).append('\n');
    report.append("Copilot UI: ").append(bundleVersion(COPILOT_UI_BUNDLE)).append("\n\n");

    String sessionsStatus = CopilotSessions.unavailableReason();
    report.append("Chat sessions: ")
        .append(sessionsStatus == null
            ? "available (" + CopilotSessions.listSessions().size() + " saved)"
            : "unavailable - " + sessionsStatus)
        .append('\n');

    CopilotExtensionsPlugin plugin = CopilotExtensionsPlugin.getDefault();
    report.append("MCP contribution point: ")
        .append(plugin != null && plugin.isMcpContributionPointEnabled() ? "enabled" : "disabled")
        .append("\n\n");

    report.append("Where to find the features:\n");
    report.append("- Window > Show View > GitHub Copilot > Copilot Sessions\n");
    report.append("- Copilot Extensions menu (sessions and MCP refresh)\n");
    report.append("- Window > Preferences > GitHub Copilot > Extensions");

    MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Copilot Extensions Status",
        report.toString());
    return null;
  }

  private static String bundleVersion(String symbolicName) {
    Bundle bundle = Platform.getBundle(symbolicName);
    if (bundle == null) {
      return "not installed";
    }
    return bundle.getVersion().toString();
  }
}
