class ProjectResource {
  final String id;
  final String name;
  final String resourceUrl;
  final String description;

  ProjectResource({
    required this.id,
    required this.name,
    required this.resourceUrl,
    this.description = '',
  });

  factory ProjectResource.fromMap(Map<String, dynamic> map) {
    return ProjectResource(
      id: map['id'] as String,
      name: map['name'] as String,
      resourceUrl: map['resourceUrl'] as String,
      description: map['description'] as String? ?? '',
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'resourceUrl': resourceUrl,
      'description': description,
    };
  }

  @override
  String toString() {
    return 'ProjectResource(id: $id, name: $name, resourceUrl: $resourceUrl, description: $description)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ProjectResource &&
        other.id == id &&
        other.name == name &&
        other.resourceUrl == resourceUrl &&
        other.description == description;
  }

  @override
  int get hashCode {
    return id.hashCode ^
        name.hashCode ^
        resourceUrl.hashCode ^
        description.hashCode;
  }
}
