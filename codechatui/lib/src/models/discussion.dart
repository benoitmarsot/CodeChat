import 'package:codechatui/src/models/message.dart';

class Discussion {
  final int did;
  final int projectId;
  final String name;
  final DateTime created;
  final bool isFavorite;
  final String description;
  final List<Message>? messages;

  Discussion({
    required this.did,
    required this.projectId,
    required this.name,
    required this.created,
    this.isFavorite = false,
    this.description = '',
    this.messages,
  });

  factory Discussion.fromJson(Map<String, dynamic> json) {
    return Discussion(
      did: json['did'],
      projectId: json['projectId'],
      name: json['name'],
      created: DateTime.fromMillisecondsSinceEpoch(json['created']),
      isFavorite: json['favorite'] ?? false,
      description: json['description'] ?? '',
      messages: json['messages'] != null
          ? (json['messages'] as List)
              .map((m) => Message.fromJson(m))
              .toList()
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'did': did,
      'projectId': projectId,
      'title': name,
      'createdAt': created.toIso8601String(),
      'favorite': isFavorite,
      'description': description,
    };
  }
}
class DiscussionNameSuggestion {
  final String name;
  final String description;

  DiscussionNameSuggestion({required this.name, required this.description});

  factory DiscussionNameSuggestion.fromJson(Map<String, dynamic> json) {
    return DiscussionNameSuggestion(
      name: json['name'],
      description: json['description'],
    );
  }
}
class DiscussionUpdateRequest {
  final int did;
  final String name;
  final String description;

  DiscussionUpdateRequest({required this.did, required this.name, required this.description});

  Map<String, dynamic> toJson() {
    return {
      'did': did,
      'name': name,
      'description': description,
    };
  }
}
