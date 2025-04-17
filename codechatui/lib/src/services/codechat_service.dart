import 'dart:convert';
//import 'dart:io';
//import 'package:path/path.dart';
import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:http/http.dart' as http;
import '../models/project.dart';
import 'package:file_picker/file_picker.dart';

class CodechatService {
  static final String baseUrl = '${AppConfig.apiVersionBaseUrl}/codechat';
  final AuthProvider authProvider;

  CodechatService({required this.authProvider});
  Map<String, String> get _headers => {
        'Authorization': 'Bearer ${authProvider.token}',
        'Content-Type': 'application/json; charset=UTF-8',
      };

  Future<void> refreshRepo(int projectId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/$projectId/refresh-repo'),
      headers: _headers,
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to refresh repository for project $projectId');
    }
  }

  Future<Project> createEmtptyProject(String name, String description) async {
    final Map<String, dynamic> requestBody = {
      'name': name,
      'description': description,
    };

    final response = await http.post(
      Uri.parse('$baseUrl/create-empty-project'),
      headers: _headers,
      body: json.encode(requestBody),
    );

    if (response.statusCode == 200) {
      return Project.fromJson(json.decode(utf8.decode(response.bodyBytes)));
    } else {
      throw Exception(
          'Failed to create project: ${utf8.decode(response.bodyBytes)}');
    }
  }

  Future<Project> createProject(
      String name, String description, String repoURL, String branchName,
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
      headers: _headers,
      body: json.encode(requestBody),
    );

    if (response.statusCode == 200) {
      return Project.fromJson(json.decode(utf8.decode(response.bodyBytes)));
    } else {
      throw Exception(
          'Failed to create project: ${utf8.decode(response.bodyBytes)}');
    }
  }

  Future<void> addProjectZip(
      int projectId, String base64Content, fileName) async {
    if (base64Content.isNotEmpty) {
      // Create the request body
      final Map<String, dynamic> requestBody = {
        'projectId': projectId,
        'zipName': fileName,
        'zipContent': base64Content,
      };

      // Send the POST request
      final response = await http.post(
        Uri.parse('$baseUrl/add-project-zip'),
        headers: _headers,
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

  Future<void> deleteAll() async {
    final response = await http.delete(
      Uri.parse('$baseUrl/delete-all'),
      headers: _headers,
    );

    if (response.statusCode != 200) {
      throw Exception(
          'Failed to delete all data: ${utf8.decode(response.bodyBytes)}');
    }
  }
}
