package copilot.eclipse.extensions.preferences;

/**
 * Preference keys for Copilot Extensions.
 */
public final class PreferenceConstants {

  public static final String WORKSPACE_DISCOVERY = "workspaceDiscovery";
  public static final String INCLUDE_GITHUB_MCP = "includeGithubMcp";
  public static final String FALLBACK_WRITE_COPILOT_PREFS = "fallbackWriteCopilotPrefs";
  public static final String EXTRA_SERVERS_JSON = "extraServersJson";

  public static final String DEFAULT_EXTRA_SERVERS_JSON = """
      {
        "servers": {
        }
      }
      """;

  private PreferenceConstants() {
  }
}
