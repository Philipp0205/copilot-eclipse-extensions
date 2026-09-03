package copilot.eclipse.extensions;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.microsoft.copilot.eclipse.ui.extensions.IMcpRegistrationProvider;

/**
 * Contributes extra MCP servers through Copilot's {@code mcpRegistration} extension point.
 */
public class ExtensionsMcpRegistrationProvider implements IMcpRegistrationProvider {

  @Override
  public CompletableFuture<String> getMcpServerConfigurations() {
    CopilotExtensionsPlugin plugin = CopilotExtensionsPlugin.getDefault();
    if (plugin == null) {
      return CompletableFuture.completedFuture(McpJson.toServersDocument(Map.of()));
    }
    return plugin.loadMcpDocument();
  }
}
