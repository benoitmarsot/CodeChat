import 'dart:convert';
import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:http/http.dart' as http;
import 'package:codechatui/src/models/message.dart';

class MessageService {
  final String baseUrl = '${AppConfig.apiVersionBaseUrl}/messages';
  final AuthProvider authProvider;
  
  MessageService({required this.authProvider});
  
  Map<String, String> get _headers => {
    'Authorization': 'Bearer ${authProvider.token}',
    'Content-Type': 'application/json',
  };

  // Add a new message to a discussion
  Future<Message> addMessage(MessageCreateRequest request) async {
    final response = await http.post(
      Uri.parse(baseUrl),
      headers: _headers,
      body: json.encode(request.toJson()),
    );
    
    if (response.statusCode == 200) {
      return Message.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Failed to add message: ${response.body}');
    }
  }

  // Get a message by ID
  Future<Message> getMessageById(int messageId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/$messageId'),
      headers: _headers,
    );
    
    if (response.statusCode == 200) {
      return Message.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Failed to load message: ${response.body}');
    }
  }

  // Get all messages for a discussion
  Future<List<Message>> getMessagesByDiscussionId(int discussionId) async {
    final response = await http.get(
      Uri.parse('$baseUrl?discussionId=$discussionId'),
      headers: _headers,
    );
    
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Message.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load messages: ${response.body}');
    }
  }

  // Update an existing message
  Future<void> updateMessage(Message message) async {
    final response = await http.put(
      Uri.parse('$baseUrl/${message.msgid}'),
      headers: _headers,
      body: json.encode(message.toJson()),
    );
    
    if (response.statusCode != 200) {
      throw Exception('Failed to update message: ${response.body}');
    }
  }

  // Delete a message
  Future<void> deleteMessage(int messageId) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/$messageId'),
      headers: _headers,
    );
    
    if (response.statusCode != 200) {
      throw Exception('Failed to delete message: ${response.body}');
    }
  }
}
