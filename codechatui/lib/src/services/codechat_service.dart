import 'dart:convert';
import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:http/http.dart' as http;
import '../models/project.dart';

class CodechatService {
  static final String baseUrl = '${AppConfig.apiVersionBaseUrl}/codechat';
  final AuthProvider authProvider;

  CodechatService({required this.authProvider});
  Map<String, String> get _headers => {
        'Authorization': 'Bearer ${authProvider.token}',
        'Content-Type': 'application/json',
      };
  Future<Project> createProject(String name, String description, String sourcePath) async {
    final response = await http.post(
      Uri.parse('$baseUrl/create-project'),
      headers: _headers,
      body: json.encode({
        'name': name,
        'description': description,
        'sourcePath': sourcePath,
      }),
    );

    if (response.statusCode == 200) {
      return Project.fromJson(json.decode(response.body));
    } else {
      throw Exception('Failed to create project: ${response.body}');
    }
  }

  Future<void> deleteAll() async {
    final response = await http.delete(
      Uri.parse('$baseUrl/delete-all'),
      headers: _headers,);

    if (response.statusCode != 200) {
      throw Exception('Failed to delete all data: ${response.body}');
    }
  }
}