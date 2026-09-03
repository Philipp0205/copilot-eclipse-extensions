package copilot.eclipse.extensions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Parses and merges MCP server JSON documents in the formats used by GitHub Copilot
 * ({@code servers}) and VS Code ({@code mcpServers}).
 */
public final class McpJson {

  public static final String SERVERS = "servers";
  public static final String MCP_SERVERS = "mcpServers";
  public static final String SERVER_NAME_PREFIX = "copilot-ext.";

  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
  private static final Pattern LINE_COMMENT = Pattern.compile("(?m)^\\s*//.*$");
  private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

  private McpJson() {
  }

  public static Map<String, JsonElement> extractServers(String json) {
    Map<String, JsonElement> servers = new LinkedHashMap<>();
    JsonObject root = parseObject(json);
    if (root == null) {
      return servers;
    }
    addServers(servers, root.get(SERVERS));
    addServers(servers, root.get(MCP_SERVERS));
    return servers;
  }

  public static String toServersDocument(Map<String, JsonElement> servers) {
    JsonObject root = new JsonObject();
    JsonObject serversObject = new JsonObject();
    if (servers != null) {
      servers.forEach(serversObject::add);
    }
    root.add(SERVERS, serversObject);
    return GSON.toJson(root);
  }

  public static Map<String, JsonElement> merge(Map<String, JsonElement> base, Map<String, JsonElement> overlay) {
    Map<String, JsonElement> merged = new LinkedHashMap<>();
    if (base != null) {
      merged.putAll(base);
    }
    if (overlay != null) {
      merged.putAll(overlay);
    }
    return merged;
  }

  public static Map<String, JsonElement> prefixKeys(Map<String, JsonElement> servers, String prefix) {
    Map<String, JsonElement> prefixed = new LinkedHashMap<>();
    if (servers == null || prefix == null || prefix.isEmpty()) {
      return prefixed;
    }
    servers.forEach((name, value) -> prefixed.put(prefix + name, value));
    return prefixed;
  }

  public static Map<String, JsonElement> withManagedPrefix(Map<String, JsonElement> servers) {
    return prefixKeys(servers, SERVER_NAME_PREFIX);
  }

  public static Map<String, JsonElement> withoutManagedServers(Map<String, JsonElement> servers) {
    Map<String, JsonElement> filtered = new LinkedHashMap<>();
    if (servers == null) {
      return filtered;
    }
    servers.forEach((name, value) -> {
      if (name == null || !name.startsWith(SERVER_NAME_PREFIX)) {
        filtered.put(name, value);
      }
    });
    return filtered;
  }

  public static JsonObject parseObject(String json) {
    if (json == null) {
      return null;
    }
    String stripped = stripComments(json).trim();
    if (stripped.isEmpty()) {
      return null;
    }
    try {
      JsonElement element = JsonParser.parseString(stripped);
      return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    } catch (JsonSyntaxException e) {
      return null;
    }
  }

  static String stripComments(String json) {
    String withoutBlocks = BLOCK_COMMENT.matcher(json).replaceAll("");
    return LINE_COMMENT.matcher(withoutBlocks).replaceAll("");
  }

  private static void addServers(Map<String, JsonElement> target, JsonElement serversElement) {
    if (serversElement == null || !serversElement.isJsonObject()) {
      return;
    }
    JsonObject servers = serversElement.getAsJsonObject();
    for (Map.Entry<String, JsonElement> entry : servers.entrySet()) {
      if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null) {
        target.put(entry.getKey(), entry.getValue());
      }
    }
  }
}
