# Copilot Extensions for Eclipse

Companion Eclipse plugin that adds extra MCP integrations for [GitHub Copilot for Eclipse](https://github.com/microsoft/copilot-for-eclipse). It is intentionally **not** part of the official Copilot plugin.

Requires GitHub Copilot for Eclipse **0.19.0 or later**.

## What it does

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

## Install

1. Install [GitHub Copilot](https://marketplace.eclipse.org/content/github-copilot) in Eclipse.
2. **Help → Install New Software…** and add this update site after you publish one, or install from a local build:
   - Build: `./mvnw clean verify`
   - Local site: `copilot.eclipse.extensions.repository/target/repository/`
3. Select **Copilot Extensions** and complete installation.
4. Restart Eclipse.
5. Open **Window → Preferences → GitHub Copilot → Extensions**.

If Copilot prompts you to approve MCP servers from a contributing plugin, approve **Copilot Extensions**.

## Build

Prerequisites: Java 17+, Maven 3.8+ (or the included `./mvnw`).

```shell
./mvnw clean verify
```

The build resolves GitHub Copilot from the official update site:

`https://azuredownloads-g3ahgwb5b8bkbxhd.b01.azurefd.net/github-copilot/`
