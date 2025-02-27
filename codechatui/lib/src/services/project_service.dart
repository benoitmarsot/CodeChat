import 'dart:convert';
import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:http/http.dart' as http;
import '../models/project.dart';

class ProjectService {
  static final String baseUrl = '${AppConfig.apiVersionBaseUrl}/projects';
  final AuthProvider authProvider;

  ProjectService({required this.authProvider});

  Map<String, String> get _headers => {
      'Authorization': 'Bearer ${authProvider.token}',
      'Content-Type': 'application/json',
    };

  Future<List<Project>> getAllProjects() async {
    final response = await http.get(
      Uri.parse(baseUrl),
      headers: _headers,
    );
    if (response.statusCode == 200) {
      List<dynamic> jsonList = json.decode(response.body);
      return jsonList.map((json) => Project.fromJson(json)).toList();
    }
    throw Exception('Failed to load projects');
  }

  Future<Project> getProject(int projectId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/$projectId'),
      headers: _headers,
    );
    if (response.statusCode == 200) {
      return Project.fromJson(json.decode(response.body));
    }
    throw Exception('Failed to load project');
  }

  Future<void> createProject(Project project) async {
    final response = await http.post(
      Uri.parse(baseUrl),
      headers: _headers,
      body: json.encode(project.toJson()),
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to create project');
    }
  }

  Future<void> updateProject(Project project) async {
    final response = await http.put(
      Uri.parse('$baseUrl/${project.projectId}'),
      headers: _headers,
      body: json.encode(project.toJson()),
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to update project');
    }
  }

  Future<void> deleteProject(int projectId) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/$projectId'),
      headers: _headers,
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to delete project');
    }
  }

  Future<List<int>> getUsersWithAccess(int projectId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/$projectId/users'),
      headers: _headers,
    );
    if (response.statusCode == 200) {
      List<dynamic> jsonList = json.decode(response.body);
      return jsonList.map((json) => json as int).toList();
    }
    throw Exception('Failed to load users with access');
  }

  Future<void> grantAccess(int projectId, int userId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/$projectId/users/$userId'),
      headers: _headers,
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to grant access');
    }
  }

  Future<void> revokeAccess(int projectId, int userId) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/$projectId/users/$userId'),
      headers: _headers,
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to revoke access');
    }
  }
}