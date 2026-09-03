package copilot.eclipse.extensions;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;

import copilot.eclipse.extensions.preferences.PreferenceConstants;

/**
 * Collects MCP server definitions from workspace files, optional GitHub MCP, and extra JSON.
 */
public final class WorkspaceMcpCollector {

  static final List<String> DISCOVERY_PATHS = List.of(
      ".vscode/mcp.json",
      ".cursor/mcp.json",
      "mcp.json",
      ".github/mcp.json");

  private WorkspaceMcpCollector() {
  }

  public static Map<String, JsonElement> collect() {
    CopilotExtensionsPlugin plugin = CopilotExtensionsPlugin.getDefault();
    if (plugin == null) {
      return new LinkedHashMap<>();
    }
    IPreferenceStore store = plugin.getPreferenceStore();
    Map<String, JsonElement> servers = new LinkedHashMap<>();
    if (store.getBoolean(PreferenceConstants.WORKSPACE_DISCOVERY)) {
      servers.putAll(collectFromWorkspace());
    }
    if (store.getBoolean(PreferenceConstants.INCLUDE_GITHUB_MCP)) {
      servers.putAll(githubMcpServer());
    }
    servers.putAll(McpJson.extractServers(store.getString(PreferenceConstants.EXTRA_SERVERS_JSON)));
    return servers;
  }

  public static String collectAsDocument() {
    return McpJson.toServersDocument(collect());
  }

  static Map<String, JsonElement> collectFromWorkspace() {
    Map<String, JsonElement> servers = new LinkedHashMap<>();
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    for (IProject project : root.getProjects()) {
      if (!project.isAccessible()) {
        continue;
      }
      for (String path : DISCOVERY_PATHS) {
        IFile file = project.getFile(path);
        if (!file.exists()) {
          continue;
        }
        String json = readFile(file);
        Map<String, JsonElement> discovered = McpJson.extractServers(json);
        String prefix = project.getName() + ".";
        servers.putAll(McpJson.prefixKeys(discovered, prefix));
      }
    }
    return servers;
  }

  static Map<String, JsonElement> githubMcpServer() {
    String json = """
        {
          "servers": {
            "github": {
              "url": "https://api.githubcopilot.com/mcp/"
            }
          }
        }
        """;
    return McpJson.extractServers(json);
  }

  private static String readFile(IFile file) {
    try (InputStream in = file.getContents(true)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (CoreException | java.io.IOException e) {
      CopilotExtensionsPlugin.logError("Failed to read MCP configuration from " + file.getFullPath(), e);
      return "";
    }
  }
}
