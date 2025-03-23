class Project {
  final int projectId;
  final String name;
  final String description;
  final int authorId;
  final int assistantId;
  Project({
    required this.projectId,
    required this.name,
    required this.description,
    required this.authorId,
    required this.assistantId,
  });

  factory Project.fromJson(Map<String, dynamic> json) {
    return Project(
      projectId: json['projectId'],
      name: json['name'],
      description: json['description'],
      authorId: json['authorId'],
      assistantId: json['assistantId'],
    );
  }

  Project copyWith({
    int? projectId,
    String? name,
    String? description,
    int? authorId,
    int? assistantId,
  }) {
    return Project(
      projectId: projectId ?? this.projectId,
      name: name ?? this.name,
      description: description ?? this.description,
      authorId: authorId ?? this.authorId,
      assistantId: assistantId ?? this.assistantId,
    );
  }

  Map<String, dynamic> toJson() => {
        'projectId': projectId,
        'name': name,
        'description': description,
        'authorId': authorId,
        'assistantId': assistantId,
      };
}
