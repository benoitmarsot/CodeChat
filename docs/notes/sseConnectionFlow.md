# SSE Connection Flow in CodeChat

Here is the complete sequence of events for the SSE connection in your application:

## 1. Initiate subscription process
- User triggers an action requiring real-time updates
- Application calls `subscribeToMessages()` in `project_detail.dart`

## 2. Obtain client ID from server
- Flutter client calls `_getClientId()` in `codechat_service.dart`
- Makes GET request to `/api/v1/sse/connect` endpoint
- Server generates UUID via `sseService.getNewClientId()`
- Server returns client ID in response

## 3. Establish SSE connection
- Flutter client creates `StreamingClient` with URL `/api/v1/sse/debug/{clientId}`
- Adds required headers including `Sse-Client-ID: {clientId}`
- Calls `_sseClient!.connect()` to initiate connection

## 4. Server-side connection handling
- Request hits `SseController.streamDebugMessages()`
- Server creates `SseEmitter` via `sseService.createEmitter(clientId)`
- Sets proper SSE headers (keep-alive, no-cache, etc.)
- Registers completion/timeout/error callbacks

## 5. Connection confirmation
- Server sends initial "Starting server communication..." message
- Client receives message via `onEvent` callback
- Client calls `onConnected` callback

## 6. Message exchange
- Server calls `sseService.sendMessage(clientId, message)` during operations
- Messages are sent through the emitter to the specific client
- Client processes messages via the `onEvent` callback
- UI updates with debug messages in the scrollable container

## 7. Client includes client ID in API requests
- All API requests include `Sse-Client-ID` header
- Server uses this ID to send progress updates during long operations

## 8. Connection termination
- Client calls `unSubscribeToMessages(clientId)` when done
- Makes request to `/api/v1/sse/debug/stop/{clientId}`
- Server removes the emitter via `sseService.removeEmitter(clientId)`
- Client clears its connection resources

## 9. Automatic cleanup
- Connection times out after 15 minutes of inactivity
- Server cleans up on errors, timeouts, or application shutdown
- Client handles disconnection gracefully

This implementation provides a robust real-time communication channel with proper resource management and error handling on both client and server sides.