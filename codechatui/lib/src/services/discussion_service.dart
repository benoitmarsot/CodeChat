import 'dart:convert';
import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:http/http.dart' as http;
import 'package:codechatui/src/models/discussion.dart';
import 'package:codechatui/src/models/message.dart';

class DiscussionService {
  final String baseUrl = '${AppConfig.apiVersionBaseUrl}/discussions';
  final AuthProvider authProvider;
  DiscussionService({required this.authProvider});
  Map<String, String> get _headers => {
        'Authorization': 'Bearer ${authProvider.token}',
        'Content-Type': 'application/json',
      };

  
  // Create a new discussion
  Future<Discussion> createDiscussion(int projectId, String? title) async {
    final response = await http.post(
      Uri.parse(baseUrl),
      headers: _headers,
      body: json.encode({
        'projectId': projectId,
        'name': title ?? 'Discussion',
      }),
    );
    
    if (response.statusCode == 200) {
      Discussion discussion = jsonDecode(response.body)['id'];
      return discussion;
    } else {
      throw Exception('Failed to create Discussion: ${response.body}');
    }
  }
  // Get all discussions for a project
  Future<List<Discussion>> getDiscussionsByProject(int projectId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/project/$projectId'),
      headers: _headers,
    );
    
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Discussion.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load discussions');
    }
  }
  
  // Get a specific discussion with its messages
  Future<Discussion> getDiscussion(int discussionId) async {
    final response = await http.get(Uri.parse('$baseUrl/$discussionId'));
    
    if (response.statusCode == 200) {
      return Discussion.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Failed to load discussion');
    }
  }
  
  // Get messages for a discussion
  Future<List<Message>> getDiscussionMessages(int discussionId) async {
    final response = await http.get(Uri.parse('$baseUrl/$discussionId/messages'));
    
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Message.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load messages');
    }
  }
}
