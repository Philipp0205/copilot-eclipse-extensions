package copilot.eclipse.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class McpJsonTest {

  @Test
  void extractsServersAndMcpServers() {
    String json = """
        {
          "servers": {
            "alpha": { "url": "https://example.com/a" }
          },
          "mcpServers": {
            "beta": { "command": "npx" }
          }
        }
        """;
    Map<String, JsonElement> servers = McpJson.extractServers(json);
    assertEquals(2, servers.size());
    assertTrue(servers.containsKey("alpha"));
    assertTrue(servers.containsKey("beta"));
  }

  @Test
  void stripsCommentsBeforeParsing() {
    String json = """
        {
          "servers": {
            // local example
            "local": {
              "type": "stdio",
              "command": "echo"
            }
          }
        }
        """;
    Map<String, JsonElement> servers = McpJson.extractServers(json);
    assertEquals(1, servers.size());
    assertTrue(servers.get("local").isJsonObject());
    assertEquals("echo", servers.get("local").getAsJsonObject().get("command").getAsString());
  }

  @Test
  void prefixesAndRemovesManagedServers() {
    Map<String, JsonElement> original = McpJson.extractServers("""
        { "servers": { "github": { "url": "https://api.githubcopilot.com/mcp/" } } }
        """);
    Map<String, JsonElement> managed = McpJson.withManagedPrefix(original);
    assertTrue(managed.containsKey("copilot-ext.github"));
    Map<String, JsonElement> mixed = McpJson.merge(
        McpJson.extractServers("""
            { "servers": { "user": { "url": "https://example.com" }, "copilot-ext.old": {} } }
            """),
        managed);
    Map<String, JsonElement> withoutManaged = McpJson.withoutManagedServers(mixed);
    assertTrue(withoutManaged.containsKey("user"));
    assertFalse(withoutManaged.containsKey("copilot-ext.old"));
    assertFalse(withoutManaged.containsKey("copilot-ext.github"));
  }

  @Test
  void writesServersDocument() {
    Map<String, JsonElement> servers = McpJson.extractServers("""
        { "mcpServers": { "memory": { "command": "npx" } } }
        """);
    String document = McpJson.toServersDocument(servers);
    JsonObject parsed = McpJson.parseObject(document);
    assertNotNull(parsed);
    assertTrue(parsed.has("servers"));
    assertTrue(parsed.getAsJsonObject("servers").has("memory"));
  }
}
