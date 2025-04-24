class Project {
  final int projectId;
  final String name;
  final String description;
  final int authorId;
  final int assistantId;
  String? assistant;
  String? model;
  List<String> resourceUris;
  List<String> allowedDomains;

  Project({
    required this.projectId,
    required this.name,
    required this.description,
    required this.authorId,
    required this.assistantId,
    this.assistant = '',
    this.model = 'gpt_4o',
    this.resourceUris = const [],
    this.allowedDomains = const [],
  });

  factory Project.fromJson(Map<String, dynamic> json) {
    return Project(
      projectId: json['projectId'],
      name: json['name'],
      description: json['description'] ?? '',
      authorId: json['authorId'],
      assistantId: json['assistantId'],
      resourceUris: (json['resourceUris'] as List<dynamic>?)
              ?.map((uri) => uri as String)
              .toList() ??
          [],
    );
  }

  Project copyWith({
    int? projectId,
    String? name,
    String? description,
    int? authorId,
    int? assistantId,
    List<String>? resourceUris,
  }) {
    return Project(
      projectId: projectId ?? this.projectId,
      name: name ?? this.name,
      description: description ?? this.description,
      authorId: authorId ?? this.authorId,
      assistantId: assistantId ?? this.assistantId,
      resourceUris: resourceUris ?? this.resourceUris,
    );
  }

  Map<String, dynamic> toJson() => {
        'projectId': projectId,
        'name': name,
        'description': description,
        'authorId': authorId,
        'assistantId': assistantId,
        // 'resourceUris': resourceUris,
      };
}
