import 'package:codechatui/src/models/project_resources.dart';
import 'package:codechatui/src/models/user.dart';

class Project {
  final int projectId;
  final String name;
  final String description;
  final int authorId;
  final int assistantId;
  String? assistant;
  String? model;
  DateTime? createdAt = DateTime.now();
  DateTime? updatedAt = DateTime.now();
  String? author;
  List<ProjectResource> resources;

  Project(
      {required this.projectId,
      required this.name,
      required this.description,
      required this.authorId,
      required this.assistantId,
      this.assistant = '',
      this.model = 'gpt_4o',
      this.resources = const [],
      this.createdAt,
      this.updatedAt,
      this.author = 'John Doe'});

  factory Project.fromJson(Map<String, dynamic> json) {
    return Project(
      projectId: json['projectId'],
      name: json['name'],
      description: json['description'] ?? '',
      authorId: json['authorId'],
      assistantId: json['assistantId'],
      //author: json['author'],
      resources: (json['resources'] as List<dynamic>?)?.map((item) {
            // Safely parse restype
            ResourceType resourceType;
            try {
              resourceType = ResourceType.values.byName(item['restype']);
            } catch (e) {
              // Handle the case where the restype is not a valid enum value
              print('Invalid ResourceType: ${item['restype']}');
              resourceType = ResourceType.web; // Or any other default value
            }

            return ProjectResource(
              uri: item['uri'],
              restype: resourceType,
              resourceId: item['prid'] is int
                  ? item['prid']
                  : int.tryParse(item['prid'].toString()) ?? 0,
              // projectId: item['projectid'],
            );
          }).toList() ??
          [],
      // createdAt: DateTime.parse(json['createdAt']),
      // updatedAt: DateTime.parse(json['updatedAt']),
    );
  }

  Project copyWith({
    int? projectId,
    String? name,
    String? description,
    int? authorId,
    int? assistantId,
    List<ProjectResource>? resource,
  }) {
    return Project(
      projectId: projectId ?? this.projectId,
      name: name ?? this.name,
      description: description ?? this.description,
      authorId: authorId ?? this.authorId,
      assistantId: assistantId ?? this.assistantId,
      resources: resources ?? this.resources,
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
