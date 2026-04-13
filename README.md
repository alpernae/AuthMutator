# Auth Mutator

Auth Mutator is a Burp Suite extension for controlled authentication mutation testing.

It helps you answer questions like:

- What happens if this request is replayed as another user?
- What happens if auth cookies are removed?
- Which responses differ when I alter specific headers/params/body values?

Auth Mutator keeps these experiments visible in one place by logging request variants, response variants, and diffs.

## Table of Contents

1. What Auth Mutator Does
2. Requirements
3. Default Configuration
4. Build and Install
5. Quick Start (First 5 Minutes)
6. Quick Cheat Sheet
7. UI Guide
8. Adding User Roles Correctly
9. Creating Replace Rules Correctly
10. Creating Highlight Rules Correctly
11. Request Processing Order (Important)
12. Replace Operation Reference
13. Filters, Columns, and Read-Only Viewers
14. Persistence, Import, and Export
15. Safety Guidance and Gotchas
16. Troubleshooting
17. License

## What Auth Mutator Does

Auth Mutator can:

- Apply enabled user roles (header/cookie token sets) to outgoing requests.
- Apply per-rule role targeting and operation-based request rewrites.
- Optionally create an unauthenticated variant by stripping cookies.
- Compare original vs modified request/response pairs with line-level diffs.
- Highlight interesting rows based on advanced response/request conditions.

Auth Mutator is built for analysis workflows where you need repeatable, visible, and configurable mutation behavior.

## Requirements

- Java 17 or newer.
- Burp Suite with Montoya API support.
- Gradle wrapper scripts are included.

The project uses Montoya API dependency version 2025.12 and targets Java 17.

## Default Configuration

Out-of-the-box defaults:

- Extension enabled: false
- Intercept enabled: true
- Auto modify requests: true
- In scope only: false
- Exclude static files: true
- Unauthenticated testing: false
- Apply rules to unauth request: false
- Apply to Proxy: false
- Apply to Repeater: true
- Apply to Intruder: true
- Apply to Scanner: false
- Preview in Proxy: true
- Max request log entries: 1000

## Build and Install

From project root:

```bash
./gradlew clean build
```

Windows PowerShell:

```powershell
gradlew.bat clean build
```

Output JAR:

```text
build/libs/Auth Mutator.jar
```

Install in Burp:

1. Burp Suite -> Extensions -> Installed -> Add.
2. Extension type: Java.
3. Select built JAR.
4. Confirm tab named Auth Mutator appears.

If build fails on Windows due Java version, set JAVA_HOME to a JDK 17 path and reopen terminal.

## Quick Start (First 5 Minutes)

1. Open Auth Mutator tab.
2. In Quick Controls, click Extension: Disabled to enable it.
3. Keep Affect Proxy off and Preview in Proxy on for safe initial testing.
4. Go to Rules and Roles and add one user role.
5. Add one replace rule targeting that role with one simple operation.
6. Send traffic in Repeater or Proxy and inspect Request Log rows.
7. Open Request/Response tabs and Diff tabs to compare behavior.

## Quick Cheat Sheet

Use this as a fast day-to-day reference.

- Safe baseline:
	- Extension: Enabled
	- Affect Proxy: Off
	- Preview in Proxy: On
	- Test in Repeater first
- Minimal role setup:
	- Add Role -> Name -> Add Cookie/Header tokens -> Save
	- Keep only one role enabled for deterministic tests
- Minimal rule setup:
	- Add Rule -> Optional target role -> One operation -> Save
	- Verify rule is enabled
- Unauth test setup:
	- Enable Unauthenticated testing
	- Optionally enable Apply rules to unauth request
	- Confirm request actually has cookies to strip
- Fast debugging sequence:
	- Confirm extension enabled
	- Confirm current Burp tool is enabled in Settings
	- Confirm scope/static filters are not excluding your target
	- Check Burp extension output for regex errors
- Viewer behavior reminders:
	- Request/response tabs are read-only
	- Diff tabs only activate when valid comparison data exists
	- Use Columns button to hide noisy columns

## UI Guide

### Request Log

The Request Log table includes:

- ID
- User Role
- Method
- URL
- Original Status Code
- Modified Status Code
- Cookies
- Parameters
- Optional Unauth column (shown when unauthenticated testing is enabled)

Selecting a row shows read-only viewers:

- Original Request
- Modified Request
- Unauth Request (when available)
- Request Diff
- Original Response
- Modified Response
- Unauth Response (when available)
- Response Diff

Important behavior:

- Viewers are read-only by design.
- Diff tabs are enabled only when comparable data exists and there is a meaningful comparison path.
- In modified-request-sent scenarios, there may be no true original response available for diff.

### Quick Controls

Main toggles and actions:

- Affect Proxy (modify browser traffic)
- Preview in Proxy (compute and log differences without modifying live proxy traffic)
- In Scope Only
- Exclude static files
- Unauthenticated testing
- Apply rules to unauth request
- Import State
- Export State
- Clear Log
- Extension enable/disable button

### Rules and Roles

This tab is split into:

- User Roles (Auth Profiles)
- Highlight Rules
- Replacement Rules

All three sections support add, edit, delete, and enable/disable workflows.

### Settings

Settings provides persistent controls for:

- Scope and static-file exclusion
- Intercept and auto-modify behavior
- Unauthenticated workflow flags
- Retention limit for log rows
- Per-tool rule application (Proxy/Repeater/Intruder/Scanner)
- Safe Mode shortcut (Proxy preview mode)

## Adding User Roles Correctly

To add a role:

1. Rules and Roles -> User Roles -> Add Role.
2. Enter a role name.
3. Add one or more tokens.
4. Choose token type per row: HEADER or COOKIE.
5. Save role.

What to pay attention to:

- Role name is required.
- At least one token is required.
- Token Name cannot be blank (blank names are ignored).
- For HEADER token names, both Authorization and Authorization: are accepted and normalized.
- COOKIE tokens should use type COOKIE (not literal Cookie header text).
- Multiple enabled roles can all apply to one request, and later role values can override earlier ones for same key.

Best practice:

- Keep only one role enabled when you want deterministic identity simulation.
- Use explicit rule target roles instead of many globally enabled roles when testing specific paths.

## Creating Replace Rules Correctly

To add a rule:

1. Rules and Roles -> Replacement Rules -> Add Rule.
2. Enter rule name.
3. Optionally choose Apply User Role.
4. Add one or more operations.
5. Save.

Validation rules:

- Rule name is mandatory.
- A rule must include either a target role or at least one replacement operation.

What to pay attention to:

- Operations execute in listed order within each rule.
- Rules execute in table order (top to bottom).
- Regex-enabled operations can fail silently on bad patterns (error logs are emitted).
- For header-name matching operations, non-regex Match accepts both Authorization and Authorization:.
- Request Header operation has a special behavior: empty Match plus Replace formatted as Header-Name: value adds a header.

Recommended rule style:

- Keep rules small and single-purpose.
- Prefer one logical mutation goal per rule.
- Use rule names that state intent, such as Swap role cookie to admin, Strip bearer token, Force org_id.

## Creating Highlight Rules Correctly

To add a highlight rule:

1. Rules and Roles -> Highlight Rules -> Add Highlight Rule.
2. Set rule name and highlight color.
3. Choose logical mode: All conditions or Any condition.
4. Add one or more conditions.
5. Save.

Condition options include:

- Message version: Original, Modified, Any.
- Optional User Role scoping.
- Match parts: request text, response text, lengths, URL, status code, domain, protocol, method, file extension, user role.
- Relationships: contains, equals, regex, numeric comparisons, and their negations.

What to pay attention to:

- Highlight rules need at least one condition.
- Numeric comparisons only make sense for status code and length fields.
- Regex uses Java regex syntax.
- Role-scoped condition matching depends on applied role text captured in Request Log.

## Request Processing Order (Important)

For each request when extension is enabled and processing conditions are met:

1. Scope/static/per-tool checks run.
2. Enabled user roles are applied first.
3. Replace rules are applied next.
4. If configured, unauth request variant is built by stripping cookies.
5. In active tool mode, the request can be modified and sent.
6. In proxy preview mode, synthetic comparisons are logged without modifying live proxy traffic.

Response handling then maps back to pending request metadata and updates log entries.

## Replace Operation Reference

Supported operation types:

- Request String
- Request Header
- Request Body
- Request Param Name
- Request Param Value
- Request Cookie Name
- Request Cookie Value
- Remove Parameter By Name
- Remove Parameter By Value
- Remove Cookie By Name
- Remove Cookie By Value
- Remove Header By Name
- Remove Header By Value
- Match Param Name, Replace Value
- Match Cookie Name, Replace Value
- Match Header Name, Replace Value

Practical grouping:

- Broad replace: Request String, Request Body.
- Header-focused: Request Header, Remove Header By Name/Value, Match Header Name, Replace Value.
- Parameter-focused: rename/replace/remove URL/BODY params.
- Cookie-focused: rename/replace/remove cookies.

How each operation works:

- Request String: Searches the entire raw request text (request line, headers, and body) and replaces matches.
- Request Header: Matches header names and updates their value. If Match is empty, Replace can be entered as Header-Name: value to add a new header.
- Request Body: Searches only the request body and replaces matches.
- Request Param Name: Renames URL/BODY parameter names that match.
- Request Param Value: Replaces URL/BODY parameter values that match.
- Request Cookie Name: Renames cookie names that match.
- Request Cookie Value: Replaces cookie values that match.
- Remove Parameter By Name: Removes URL/BODY parameters with matching names.
- Remove Parameter By Value: Removes URL/BODY parameters with matching values.
- Remove Cookie By Name: Removes cookies with matching names.
- Remove Cookie By Value: Removes cookies with matching values.
- Remove Header By Name: Removes headers with matching names.
- Remove Header By Value: Removes headers with matching values.
- Match Param Name, Replace Value: Keeps parameter names and sets a new value when the name matches.
- Match Cookie Name, Replace Value: Keeps cookie names and sets a new value when the name matches.
- Match Header Name, Replace Value: Keeps header names and sets a new value when the name matches.

Matching and format notes:

- Header-name matching is case-insensitive.
- For non-regex header-name operations, both X-User and X-User: are accepted and treated as the same header name.
- Regex mode uses the Match field exactly as written (no normalization).
- Parameter operations target URL and BODY parameters, not cookies.
- Cookie operations target cookie parameters only.

## Filters, Columns, and Read-Only Viewers

Table filters:

- Method filter
- Status bucket filter (2xx/3xx/4xx/5xx)
- URL contains filter
- ID exact filter
- Role filter input

Behavior:

- Filters are combined (AND behavior).
- Invalid ID input is highlighted.

Column visibility:

- Use Columns button with filter icon in Request Log.
- Checked column names are shown.
- Unchecked column names are hidden.
- At least one column remains visible.

Request/response viewers:

- Read-only by design.
- Intended for inspection and diffing, not editing.

## Persistence, Import, and Export

Auto-persisted state file:

```text
~/.AuthMutator.json
```

State includes:

- Extension config
- Replace rules
- Highlight rules
- User roles

Import/Export notes:

- Import updates in-memory state and persists it as new default.
- Export writes current in-memory state to selected file.
- Use Import State/Export State in Quick Controls or Import JSON/Export JSON in Replacement Rules.

## Safety Guidance and Gotchas

Recommended safe baseline:

- Affect Proxy: off
- Preview in Proxy: on
- Test mostly in Repeater first

Important gotchas:

- Extension is disabled by default. Enable it explicitly.
- Exclude static files also treats extensions like .json and .xml as static. Disable this option if your API endpoints rely on such suffixes.
- Multiple enabled roles can cause token overrides and hard-to-explain outcomes.
- Proxy preview mode may send synthetic requests for comparison, which can still hit real targets.

## Troubleshooting

No entries are logged:

- Check Extension is enabled.
- Check Enable request interception in Settings.
- Check per-tool enablement for your current tool.
- Check In Scope Only and static-file exclusion filters.

Rule appears not to work:

- Confirm rule is enabled.
- Confirm role referenced by rule exists and is enabled.
- Confirm match pattern actually matches request content.
- Check regex syntax errors in Burp extension output logs.

Unauth column/variant missing:

- Enable Unauthenticated testing.
- Ensure cookies exist to strip; otherwise unauth variant may not differ.

Diff tab disabled:

- Diff requires a valid comparable pair and meaningful comparison path.
- For some modified-send flows there is no original response counterpart.

Build issues on Windows:

- Use Java 17+ and set JAVA_HOME accordingly.

## License

Auth Mutator is distributed under the MIT License. See [LICENSE](LICENSE).

