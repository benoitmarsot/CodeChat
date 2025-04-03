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
        'Content-Type': 'application/json; charset=UTF-8',
      };

  
  // Create a new open ai thread
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
      Discussion discussion = Discussion.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
      return discussion;
    } else {
      throw Exception('Failed to create Discussion: ${utf8.decode(response.bodyBytes)}');
    }
  }

  // Add a new message to a discussion
  // Add to the OpenAi thread
  Future<Message> askQuestion(MessageCreateRequest request) async {
    final response = await http.post(
      Uri.parse('$baseUrl/ask-question'),
      headers: _headers,
      body: json.encode(request.toJson()),
    );
    
    if (response.statusCode == 200) {
      return Message.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
    } else {
      throw Exception('Failed to ask question: ${utf8.decode(response.bodyBytes)}');
    }
  }
  // Create an openAI run 
  // Add the answer to the OpenAi thread
  Future<Message> answerQuestion(int did) async {
    final response = await http.post(
      Uri.parse('$baseUrl/$did/answer-question'),
      headers: _headers,
    );
    
    if (response.statusCode == 200) {
      return Message.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
    } else {
      throw Exception('Failed to answer question: ${utf8.decode(response.bodyBytes)}');
    }
  }
  // Update an existing discussion
  Future<Discussion> updateDiscussion(DiscussionUpdateRequest discussion) async {
    final response = await http.put(
      Uri.parse('$baseUrl/${discussion.did}'),
      headers: _headers,
      body: json.encode(discussion.toJson()),
    );
    
    if (response.statusCode != 200) {
      throw Exception('Failed to update discussion: ${utf8.decode(response.bodyBytes)}');
    }
    return Discussion.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
  }

  // Get all discussions for a project
  Future<List<Discussion>> getDiscussionsByProject(int projectId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/project/$projectId'),
      headers: _headers,
    );
    
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(utf8.decode(response.bodyBytes));
      return data.map((json) => Discussion.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load discussions');
    }
  }
  
  // Get a specific discussion with its messages
  Future<Discussion> getDiscussion(int discussionId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/$discussionId'),
      headers: _headers,
  );
    
    if (response.statusCode == 200) {
      return Discussion.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
    } else {
      throw Exception('Failed to load discussion');
    }
  }
  
  //Use AI to get a name and description sugestion for a discussion 
  Future<List<DiscussionNameSuggestion>> getNamesSuggestion(int did) async {
    final response = await http.get(
      Uri.parse('$baseUrl/$did/suggest'),
      headers: _headers,
    );
    
    if (response.statusCode == 200) {
      final String answer = utf8.decode(response.bodyBytes);
      final List<dynamic> data = jsonDecode(answer);
      return data.map((json) => DiscussionNameSuggestion.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load suggestions');
    }
    

  }

  // Delete a discussion
  Future<void> deleteDiscussion(int did) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/$did'),
      headers: _headers,
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to delete discussion: ${utf8.decode(response.bodyBytes)}');
    }
  }

}
