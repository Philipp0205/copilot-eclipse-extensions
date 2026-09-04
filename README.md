# Copilot Extensions for Eclipse

Companion Eclipse plugin that adds multiple chat sessions and extra MCP integrations to [GitHub Copilot for Eclipse](https://github.com/microsoft/copilot-for-eclipse). It is intentionally **not** part of the official Copilot plugin.

Requires GitHub Copilot for Eclipse **0.19.0 or later**; the session features are built and verified against **0.21.0**.

## What it does

### Multiple chat sessions

Copilot for Eclipse saves every conversation, but the only way back to one is the chat history panel. This plugin turns those conversations into named sessions you can move between directly:

- **Window → Show View → GitHub Copilot → Copilot Sessions** lists every saved session, most recently used first, with the current one marked
- Double-click a session to continue it in the chat view
- New, rename and delete sessions from the view toolbar or its context menu
- Key bindings: `Ctrl+Alt+M` (`Cmd+Alt+M` on macOS) to pick a session, `Ctrl+Alt+Page Down` / `Ctrl+Alt+Page Up` to cycle through them
- Same commands under the **Copilot Extensions** menu, and rebindable under **Preferences → General → Keys**

Sessions run one at a time in Copilot's single chat view; switching swaps which conversation that view shows. Copilot's own chat services (agent tools, working set, todo list) bind to one chat view instance, so two live chat views would fight over them — see [Why not side-by-side sessions](#why-not-side-by-side-sessions).

### Extra MCP servers

- Discovers MCP configs from Eclipse projects:
  - `.vscode/mcp.json`
  - `.cursor/mcp.json`
  - `mcp.json`
  - `.github/mcp.json`
- Optionally registers the GitHub remote MCP server (`https://api.githubcopilot.com/mcp/`)
- Accepts extra MCP JSON on **Window → Preferences → GitHub Copilot → Extensions**
- Contributes those servers through Copilot's official MCP extension point when policy allows it
- When the contribution point is disabled, can merge the same servers into Copilot MCP preferences using the `copilot-ext.` name prefix (user servers are left intact)
- **Copilot Extensions → Refresh Copilot Extensions MCP** reloads the discovered configuration

### Checking that it is installed

**Copilot Extensions → Copilot Extensions Status** reports the detected plugin and Copilot versions, whether sessions are available, and where each feature lives in the UI. Start here if you installed the plugin but do not see the extra features.

## Install

1. Install [GitHub Copilot](https://marketplace.eclipse.org/content/github-copilot) in Eclipse (0.19.0 or later).
2. **Help → Install New Software…** and add the p2 update site:
   ```
   https://philipp0205.github.io/copilot-eclipse-extensions/
   ```
   The site is published from `main` via GitHub Pages (`Settings → Pages → GitHub Actions`).
3. Select **Copilot Extensions** and complete installation.
4. Restart Eclipse.
5. Confirm it is active with **Copilot Extensions → Copilot Extensions Status**, then open **Window → Show View → GitHub Copilot → Copilot Sessions**.

The extra features live in their own menus and views rather than inside Copilot's chat view, because a companion plugin cannot add toolbar buttons to a view it does not own. If the **Copilot Extensions** menu is missing entirely, the plugin is not installed or did not resolve; check **Help → About → Installation Details → Plug-ins** for `copilot.eclipse.extensions` and the error log for resolution failures.

To install from a local build instead:

```shell
./mvnw clean verify
```

Then add `copilot.eclipse.extensions.repository/target/repository/` as a local site, or use the zipped p2 repository `copilot.eclipse.extensions.repository/target/copilot-eclipse-extensions-1.0.0-SNAPSHOT.zip`.

If Copilot prompts you to approve MCP servers from a contributing plugin, approve **Copilot Extensions**.

## Why not side-by-side sessions

Two Copilot chat views running at the same time is not something a companion plugin can add safely on Copilot 0.21, so this plugin does not pretend to. The chat view class is public and could be subclassed into a view declared with `allowMultiple="true"`, but its collaborators are not designed for more than one instance:

- Creating a chat view calls `bindChatView(this)` on the shared `AgentToolService`, `FileToolService`, `TodoListService` and `UserPreferenceService`. A second view rebinds them, so agent tool calls, file edits and the working set would be routed to whichever view was created last.
- Send and history events travel over application-wide `IEventBroker` topics with no view identity attached, so every open chat view reacts to one send.

Both would need per-view scoping upstream. Until then, one chat view with fast session switching is the honest version of the feature.

## Build

Prerequisites: Java 21+, Maven 3.8+ (or the included `./mvnw`).

```shell
./mvnw clean verify
```

The build resolves GitHub Copilot from the official update site:

`https://azuredownloads-g3ahgwb5b8bkbxhd.b01.azurefd.net/github-copilot/`
