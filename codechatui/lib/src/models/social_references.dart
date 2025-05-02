class SocialReferences {
  final String overallDescription;
  final List<SocialReference> messages;

  SocialReferences({
    required this.overallDescription,
    required this.messages,
  });

  factory SocialReferences.fromJson(Map<String, dynamic> json) {
    return SocialReferences(
      overallDescription: json['overallDescription'] ?? '',
      messages: (json['messages'] as List<dynamic>? ?? [])
          .map((e) => SocialReference.fromJson(e))
          .toList(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'overallDescription': overallDescription,
      'messages': messages.map((e) => e.toJson()).toList(),
    };
  }
}
class SocialReference {
  final String name;
  final String description;
  final String url;

  SocialReference({
    required this.name,
    required this.description,
    required this.url,
  });

  factory SocialReference.fromJson(Map<String, dynamic> json) {
    return SocialReference(
      name: json['name'] ?? '',
      description: json['description'] ?? '',
      url: json['url'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'description': description,
      'url': url,
    };
  }
}