import 'dart:convert';
//import 'dart:io';
//import 'package:path/path.dart';
import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/client/sse.dart';
import 'package:codechatui/src/models/project_resources.dart';
import 'package:codechatui/src/services/auth_provider.dart';
// import 'package:flutter_client_sse/flutter_client_sse.dart';
import 'package:http/http.dart' as http;
import '../models/project.dart';
// import 'package:file_picker/file_picker.dart';
import 'dart:async';
//import 'package:eventsource/eventsource.dart';

class CodechatService {
  static final String baseUrl = '${AppConfig.apiVersionBaseUrl}/codechat';
  static final String sseBaseUrl = '${AppConfig.apiVersionBaseUrl}/sse';
  final AuthProvider authProvider;
  StreamingClient? _sseClient;

  CodechatService({required this.authProvider});

  Map<String, String> getHeader(String? clientId) {
    return {
      'Authorization': 'Bearer ${authProvider.token}',
      'Content-Type': 'application/json; charset=UTF-8',
      'Sse-Client-ID': clientId ?? '',
    };
  }

  Future<void> refreshRepo(int projectId, String? clientId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/$projectId/refresh-repo'),
      headers: getHeader(clientId),
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to refresh repository for project $projectId');
    }
  }

  // Get a client ID from the server for SSE connection
  Future<String> _getClientId() async {
    final response = await http.get(
      Uri.parse('$sseBaseUrl/connect'),
      headers: getHeader(null),
    );

    if (response.statusCode == 200) {
      final Map<String, dynamic> data =
          json.decode(utf8.decode(response.bodyBytes));
      return data['clientId'];
    } else {
      throw Exception(
          'Failed to get SSE client ID: ${utf8.decode(response.bodyBytes)}');
    }
  }

  Future<String?> subscribeToMessages(
      {void Function(String, String)? onEvent,
      void Function(dynamic)? onError,
      void Function()? onDone,
      void Function()? onConnected}) async {
    // Get client ID first
    final clientId = await _getClientId();

    // Add Sse-Client-ID header for server identification
    final headers = {
      'Accept': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Content-Type': 'application/json; charset=UTF-8',
      'Authorization': 'Bearer ${authProvider.token}',
      'Sse-Client-ID': clientId
    };

    _sseClient = StreamingClient(
        url: '$sseBaseUrl/debug/$clientId',
        headers: headers,
        onEvent: onEvent,
        onError: onError,
        onDone: onDone,
        onConnected: onConnected);

    try {
      _sseClient!.connect();
      print('Connecting to SSE with client ID: $clientId');
      return clientId;
    } catch (e) {
      print('Request failed: $e');
      return null;
    }
  }

  Future<void> unSubscribeToMessages(String clientId) async {
    try {
      print('Stopping SSE subscription...');

      // Close client connection if it exists
      _sseClient?.cancel();

      // Notify server to stop emitting events
      final response = await http.get(
        Uri.parse('$sseBaseUrl/debug/stop/$clientId'),
        headers: getHeader(clientId),
      );

      if (response.statusCode == 200) {
        print('SSE subscription stopped successfully.');
      } else {
        throw Exception(
            'Failed to stop SSE subscription: ${utf8.decode(response.bodyBytes)}');
      }
    } catch (e) {
      print('Error stopping SSE subscription: $e');
      throw Exception('Error stopping SSE subscription: $e');
    } finally {
      _sseClient = null;
    }
  }

  Future<Project> createEmtptyProject(
      String name, String description, String? clientId) async {
    final Map<String, dynamic> requestBody = {
      'name': name,
      'description': description,
    };

    final response = await http.post(
      Uri.parse('$baseUrl/create-empty-project'),
      headers: getHeader(clientId),
      body: json.encode(requestBody),
    );

    if (response.statusCode == 200) {
      return Project.fromJson(json.decode(utf8.decode(response.bodyBytes)));
    } else {
      throw Exception(
          'Failed to create project: ${utf8.decode(response.bodyBytes)}');
    }
  }

  Future<Project> createProject(String name, String description, String repoURL,
      String branchName, String? clientId,
      [String? username, String? password]) async {
    final Map<String, dynamic> requestBody = {
      'name': name,
      'description': description,
      'repoURL': repoURL,
      'branch': branchName,
    };

    // Add authentication parameters only if provided
    if (username != null && username.isNotEmpty) {
      requestBody['username'] = username;
      if (password != null && password.isNotEmpty) {
        requestBody['password'] = password;
      }
    }

    final response = await http.post(
      Uri.parse('$baseUrl/create-project'),
      headers: getHeader(clientId),
      body: json.encode(requestBody),
    );

    if (response.statusCode == 200) {
      return Project.fromJson(json.decode(utf8.decode(response.bodyBytes)));
    } else {
      throw Exception(
          'Failed to create project: ${utf8.decode(response.bodyBytes)}');
    }
  }

  Future<ProjectResource> addProjectWeb(
      {required int projectId,
      required String seedUrl,
      int? maxPages,
      int? maxDepth,
      int? requestsPerMinute,
      List<String>? allowedDomains,
      String? userName,
      String? password,
      String? clientId}) async {
    // Create the request body
    final Map<String, dynamic> requestBody = {
      'projectId': projectId,
      'seedUrl': seedUrl,
    };

    // Add optional parameters if provided
    if (maxPages != null) requestBody['maxPages'] = maxPages;
    if (maxDepth != null) requestBody['maxDepth'] = maxDepth;
    if (requestsPerMinute != null)
      requestBody['requestsPerMinute'] = requestsPerMinute;
    if (allowedDomains != null && allowedDomains.isNotEmpty) {
      requestBody['allowedDomains'] = allowedDomains;
    }
    if (userName != null && userName.isNotEmpty) {
      requestBody['userName'] = userName;
      if (password != null && password.isNotEmpty) {
        requestBody['password'] = password;
      }
    }
    var headers = getHeader(clientId);
    // Send the POST request
    final response = await http.post(
      Uri.parse('$baseUrl/add-project-web'),
      headers: headers,
      body: json.encode(requestBody),
    );

    // Check the response status
    if (response.statusCode == 200) {
      return ProjectResource.fromJson(
          json.decode(utf8.decode(response.bodyBytes)));
    } else {
      throw Exception(
          'Failed to add project web resource: ${utf8.decode(response.bodyBytes)}');
    }
  }

  Future<void> addProjectZip(
      int projectId, String base64Content, fileName, String? clientId) async {
    if (base64Content.isNotEmpty) {
      // Create the request body
      final Map<String, dynamic> requestBody = {
        'projectId': projectId,
        'zipName': fileName,
        'zipContent': base64Content,
      };
      var headers = getHeader(clientId);
      // Send the POST request
      final response = await http.post(
        Uri.parse('$baseUrl/add-project-zip'),
        headers: headers,
        body: json.encode(requestBody),
      );

      // Check the response status
      if (response.statusCode != 200) {
        throw Exception(
            'Failed to add project zip: ${utf8.decode(response.bodyBytes)}');
      }
    } else {
      throw Exception('No file selected or file is empty.');
    }
  }

// CODE FOR NATIVE CLIENTS
  // Future<void> addProjectZip(int projectId, String zipFilePath) async {
  //   // Read the file and encode its content in Base64
  //   final bytes = await File(zipFilePath).readAsBytes();
  //   final base64Content = base64Encode(bytes);
  //   // Create the request body
  //   final Map<String, dynamic> requestBody = {
  //     'projectId': projectId,
  //     'zipName': basename(zipFilePath), // Extract the file name from the path
  //     'zipContent': base64Content,
  //   };
  //   // Send the POST request
  //   final response = await http.post(
  //     Uri.parse('$baseUrl/add-project-zip'),
  //     headers: _headers,
  //     body: json.encode(requestBody),
  //   );
  //   // Check the response status
  //   if (response.statusCode != 200) {
  //     throw Exception(
  //         'Failed to add project zip: ${utf8.decode(response.bodyBytes)}');
  //   }
  // }

  Future<void> deleteAll(String clientId) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/delete-all'),
      headers: getHeader(clientId),
    );

    if (response.statusCode != 200) {
      throw Exception(
          'Failed to delete all data: ${utf8.decode(response.bodyBytes)}');
    }
  }
}
