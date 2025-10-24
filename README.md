# Auth Mutator

Auth Mutator is a Burp Suite extension that helps you experiment with mutated authentication requests while keeping the original traffic intact. It applies user-defined replace rules, removes authentication artefacts for unauthenticated probes, and highlights noteworthy responses so you can quickly spot interesting behaviour.

## Features

- Rich request log with side-by-side tabs for original, rule-modified, and unauthenticated variations.
- Inline request/response diff view that highlights only the changed lines (+ blue for additions, - red for removals).
- Quick controls to toggle scope, proxy impact, preview-only mode, and unauthenticated testing workflows.
- Replace rule editor for multi-step request rewrites (headers, parameters, body edits, removals, regex support, etc.).
- Highlight rule editor with advanced logical conditions for flagging interesting responses.
- Import/export and automatic persistence of configuration and rules to `~/.AuthMutator.json`.

## Requirements

- **Java 17** (matches the extension's source/target compatibility).
- **Burp Suite 2023.12+** (Montoya API 2025.7 or later).
- **Gradle** is optional; use the included wrapper scripts (`./gradlew` or `gradlew.bat`).

## Building the Extension

From the project root:

```bash
./gradlew clean build
```

On Windows:

```powershell
gradlew.bat clean build
```

The shaded JAR will be produced at:

```
build/libs/Auth Mutator.jar
```

Copy or rename the file as needed before loading it into Burp Suite.

## Installing in Burp Suite

1. Open **Burp Suite → Extensions → Installed → Add**.
2. Choose **Java** as the extension type.
3. Browse to the built JAR (`Auth Mutator.jar`) and load it.
4. The extension tab appears as **Auth Mutator**; logs confirming successful load are printed to the Burp output tab.

## Usage Overview

The **Auth Mutator** tab is organised into four sub-tabs.

### Request Log

- Displays every processed request with columns for original status, modified status, cookie count, parameter count, and optionally an **Unauth** flag.
- Selecting a row opens two viewers (Request / Response) with tabs:
	- **Original** – exact traffic Burp sent or received.
	- **Modified** – the request/response after replace rules were applied.
	- **Unauth** – the variant sent with cookies stripped (only present when unauthenticated testing is enabled and triggered).
	- **Diff** – colour-coded line diff highlighting only additions (`+` blue) and removals (`-` red).
- Quick column layout updates automatically when the Unaith toggle is enabled/disabled.

### Quick Controls

Located above the Request Log and mirrored in **Settings** for persistence.

- **Extension Enabled** – master toggle; disables all processing when off.
- **Affect Proxy** – allow replace rules to mutate live browser traffic. Disable to keep Proxy requests untouched.
- **Preview in Proxy** – compute diffs without modifying live Proxy traffic (synthetic requests are replayed off-band to populate the log).
- **In Scope Only** – restrict processing to URLs within Burp's target scope.
- **Unauthenticated Testing** – fire an additional request per entry with cookies stripped. When active:
	- **Apply rules to unauth request** determines whether replace rules run before cookie stripping for the unauth variant.

### Replace Rules

- Create sequences of operations (header edits, parameter rewrites, regex replacements, removals, etc.).
- Rules run in order; multiple operations within a rule can build complex transformations.
- Enable/disable rules quickly, import/export JSON bundles, and toggle entire rules via the table.

### Highlight Rules

- Define logical conditions (ANY/ALL) based on message version, parts (request/response), and relationships (equals, contains, regex, length, status code).
- Matches provide coloured row highlighting in the Request Log.

### Settings

- Mirrors quick controls for persistence.
- Adjust request log retention (minimum 100 entries; oldest entries drop automatically when the limit is hit).
- Configure tool scope (Proxy/Repeater/Intruder/Scanner) for rule application.
- Access **Safe Mode** to disable Proxy mutation while keeping preview active.

## Unauthenticated Testing Workflow

1. Enable **Unauthenticated testing** in Quick Controls.
2. Choose whether to apply replace rules to the unauthenticated variant.
3. For each processed request:
	 - The main traffic follows normal rule application (if allowed for the tool).
	 - A synthetic unauthenticated request is sent with cookies removed.
	 - The Request Log displays all three versions (original, rule-modified, unauth).
	 - Differences are visible via the Diff tab and status columns.

When **Preview in Proxy** is active, the original traffic still flows unchanged while modified and unauth variants are replayed out-of-band for comparison.

## Persistence & State

- Rules and configuration are stored in `~/.AuthMutator.json`.
- The **State Actions** panel (top-right of Request Log tab) offers **Import** and **Export** buttons for sharing or backing up state.
- Imported state is immediately saved and applied across all panels.

## Development Notes

- The project targets Java 17 and uses the Montoya API (`net.portswigger.burp.extensions:montoya-api:2025.7`).
- Swing UI components are located under `src/main/java/ui`.
- HTTP processing lives in `src/main/java/handler/RequestHandler.java`.
- Run `./gradlew build` to verify changes; the task compiles sources and assembles the distributable JAR.
- Logs emitted via `api.logging().logToOutput/Error` help diagnose rule behaviour; enable Burp's extender output to view them.

## Troubleshooting

- **No modified/unauth tabs appear** – ensure the relevant Quick Control toggles are enabled and that the request actually triggered a change.
- **Diff tab disabled** – it only activates when changes exist between the compared messages.
- **Preview mode not showing responses** – synthetic replay requires external network connectivity; errors are logged in Burp's Extender output.
- **State changes not persisting** – confirm Burp has permission to write the home directory; check for errors about `.AuthMutator.json` in the output log.

## Known Issues

- **Modified Response tab appears empty** – In some cases, when replace rules are applied and the modified request is sent, the Modified Response tab may appear empty in the extension UI, even though the response is successfully received and visible in Repeater or the Burp HTTP history. This appears to affect various response types and is under active investigation. 
  - **Workaround**: Check the Original Response tab (which may show the response when the modified request was sent) or resend the request in Repeater to view the actual response. 
  - **Diagnosis**: Debug logging has been added to trace the request/response flow - check Burp's Extension Output tab (Extensions → Auth Mutator → Output) for diagnostic information to help identify the root cause.

## License

Auth Mutator is distributed under the terms of the [GNU General Public License v3.0](LICENSE). You may redistribute and/or modify it under those terms.

Happy hunting!

