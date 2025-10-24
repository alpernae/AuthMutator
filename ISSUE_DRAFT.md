# GitHub Issue Draft

**Title:** Modified Response tab shows empty content after replace rules are applied

**Labels:** `bug`, `investigation-needed`

---

## Description
When replace rules are applied to requests and the modified request is sent to the server, the Modified Response tab in the extension UI appears empty, even though the response is successfully received and can be viewed in Burp's Repeater or HTTP history.

## Steps to Reproduce
1. Enable the Auth Mutator extension
2. Configure replace rules that modify request parameters/headers/body
3. Enable "Automatically apply replace rules" in settings
4. Send a request from Proxy or Repeater where rules are applied
5. Click on the entry in the Auth Mutator request log
6. Navigate to the "Modified Response" tab

## Expected Behavior
The Modified Response tab should display the response received after sending the modified request.

## Actual Behavior
The Modified Response tab appears empty or shows no content, even though:
- The request was successfully modified (visible in Modified Request tab)
- A response was received (visible in Repeater/HTTP history)
- The Modified Status column shows "0" or "None"

## Environment
- Burp Suite version: 2023.12+
- Auth Mutator version: [current version]
- Java version: 17

## Additional Context
- The issue appears to affect various response types and status codes
- When the modified request is resent in Repeater, the response displays correctly
- The Original Response tab sometimes shows the response that should be in Modified Response
- Debug logging has been added to help diagnose this issue (check Extension Output tab)

## Workaround
- Check the Original Response tab (may contain the response when modified request was sent)
- Resend the request in Repeater to view the actual response

## Investigation Notes
The issue may be related to how responses are being captured and stored when `modifiedRequestSent` is true. Debug logging suggests the response is being received but may not be properly associated with the table entry.

Relevant code locations:
- `src/main/java/handler/RequestHandler.java` - Response handling logic
- `src/main/java/ui/RequestTablePanel.java` - UI display logic
- `src/main/java/model/RequestLogEntry.java` - Data model
