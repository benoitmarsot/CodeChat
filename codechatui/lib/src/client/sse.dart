import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;

/// A lightweight SSE client for Flutter Web using Fetch + ReadableStream.
/// The StreamingClient class is a lightweight Server-Sent Events (SSE) client designed for Flutter applications,
/// particularly for Flutter Web. It provides an easy way to connect to an SSE endpoint, process incoming events, and handle custom event types.
/// The class is built using Dart's http package and supports features like custom headers, event handling, and stream cancellation.
///
class StreamingClient {
  final String url;
  final Map<String, String> headers;
  void Function(String event, String data)? onEvent;
  void Function(Object error)? onError;
  void Function()? onDone;
  void Function()? onConnected;

  StreamingClient(
      {required this.url,
      this.headers = const {},
      this.onEvent,
      this.onError,
      this.onDone,
      this.onConnected});

  bool _isCancelled = false;
  Future<void>? _connection;

  void connect() {
    _isCancelled = false;
    _connection = _startListening();
  }

  void cancel() {
    _isCancelled = true;
  }

  Future<void> _startListening() async {
    final client = http.Client();
    try {
      final request = http.Request('GET', Uri.parse(url));
      request.headers.addAll(headers);

      final response = await client.send(request);
      String buffer = '';
      onConnected?.call();
      final stream = response.stream.transform(utf8.decoder);
      await for (final chunk in stream) {
        if (_isCancelled) break;

        buffer += chunk;

        int boundary;
        while ((boundary = buffer.indexOf("\n\n")) != -1) {
          final eventChunk = buffer.substring(0, boundary);
          buffer = buffer.substring(boundary + 2);
          _processChunk(eventChunk);
        }
      }

      if (!_isCancelled && onDone != null) onDone!();
    } catch (e) {
      if (!_isCancelled && onError != null) onError!(e);
    } finally {
      client.close();
    }
  }

  void _processChunk(String chunk) {
    final lines = chunk.split('\n');
    String event = 'message';
    String data = '';

    for (final line in lines) {
      if (line.startsWith('event:')) {
        event = line.substring(6).trim();
      } else if (line.startsWith('data:')) {
        data += line.substring(5).trim() + '\n';
      }
    }

    data = data.trim();
    if (onEvent != null && data.isNotEmpty) {
      onEvent!(event, data);
    }
  }
}
