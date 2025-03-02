import 'dart:convert';

import 'package:flutter_syntax_view/flutter_syntax_view.dart';

class Message {
  final int msgid;
  final int discussionId;
  final String role; // 'user', 'assistant', etc.
  final int authorid;
  final String text; // this corresponds to 'message' in Java
  final DateTime timestamp; // kept for compatibility

  Message({
    this.msgid = 0,
    this.discussionId = 0,
    required this.text,
    this.role = 'user',
    this.authorid = 0,
    DateTime? timestamp,
  }) : timestamp = timestamp ?? DateTime.now();

  bool get isUserMessage => role == 'user';

  factory Message.fromJson(Map<String, dynamic> json) {
    return Message(
      msgid: json['msgid'] ?? 0,
      discussionId: json['discussionId'] ?? 0,
      text: json['message'] ?? json['text'] ?? '',
      role: json['role'] ?? (json['isUserMessage'] == true ? 'user' : 'assistant'),
      authorid: json['authorid'] ?? 0,
      timestamp: json['timestamp'] != null 
          ? DateTime.parse(json['timestamp']) 
          : DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'msgid': msgid,
      'discussionId': discussionId,
      'role': role,
      'authorid': authorid,
      'message': text,
      'timestamp': timestamp.toIso8601String(),
    };
  }

  AIResponse? get aiResponse {
    if (isUserMessage) return null;
    try {
      return AIResponse.fromJson(jsonDecode(text));
    } catch (e) {
      return null;
    }
  }
}

class AIResponse {
  final String question;
  final List<AIAnswerItem> answers;

  AIResponse({required this.question, required this.answers});

  factory AIResponse.fromJson(Map<String, dynamic> json) {
    return AIResponse(
      question: json['question'],
      answers: (json['answers'] as List)
          .map((item) => AIAnswerItem.fromJson(item))
          .toList(),
    );
  }
}

class AIAnswerItem {
  final String explanation;
  final String? language;
  final String? code;
  final List<String> references;

  AIAnswerItem({
    required this.explanation, 
    this.language, 
    this.code, 
    required this.references
  });

  factory AIAnswerItem.fromJson(Map<String, dynamic> json) {
    return AIAnswerItem(
      explanation: json['explanation'],
      language: json['language'],
      code: json['code'],
      references: (json['references'] as List).map((e) => e.toString()).toList(),
    );
  }
  
  // Get the appropriate syntax language for the syntax highlighter
  Syntax getSyntaxLanguage() {
    if (language == null) return Syntax.JAVA;
    
    // Convert language to lowercase for case-insensitive matching
    final lang = language!.toLowerCase();
    
    // Map common languages to their syntax identifier
    switch (lang) {
      case 'javascript':
      case 'js':
      case 'typescript':
      case 'ts':
        return Syntax.JAVASCRIPT;
      case 'python':
      case 'py':
        return Syntax.JAVASCRIPT;
      case 'java':
        return Syntax.JAVA;
      case 'c#':
      case 'csharp':
      case 'cs':
        return Syntax.CPP;
      case 'c++':
      case 'cpp':
        return Syntax.CPP;
      case 'c':
        return Syntax.C;
      case 'go':
        return Syntax.RUST;
      case 'rust':
      case 'rs':
        return Syntax.RUST;
      case 'ruby':
      case 'rb':
        return Syntax.RUST;
      case 'php':
        return Syntax.CPP;
      case 'swift':
        return Syntax.SWIFT;
      case 'kotlin':
      case 'kt':
        return Syntax.KOTLIN;
      case 'html':
        return Syntax.YAML;
      case 'css':
        return Syntax.YAML;
      case 'sql':
        return Syntax.C;
      case 'bash':
      case 'sh':
        return Syntax.C;
      case 'yaml':
      case 'yml':
        return Syntax.YAML;
      case 'json':
        return Syntax.JAVASCRIPT;
      case 'markdown':
      case 'md':
        return Syntax.C;
      case 'dart':
        return Syntax.DART;
      case 'xml':
        return Syntax.YAML;
      default:
        // Default to TEXT for unrecognized languages
        return Syntax.JAVA;
    }
  }
}
