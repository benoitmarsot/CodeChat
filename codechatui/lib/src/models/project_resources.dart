typedef Secrets = Map<String, String>;

class ProjectResource {
  final int resourceId;
  final int projectId;
  final String uri;
  final Map<String, Secret>? secrets;

  ProjectResource({
    required this.resourceId,
    required this.projectId,
    required this.uri,
    this.secrets,
  });

  factory ProjectResource.fromJson(Map<String, dynamic> json) {
    return ProjectResource(
      resourceId: json['prId'],
      projectId: json['projectId'],
      uri: json['uri'],
      secrets: json['secrets'] != null
          ? (json['secrets'] as Map<String, dynamic>).map(
              (key, value) => MapEntry(key, Secret.fromJson(value)),
            )
          : null,
    );
  }

  Map<String, dynamic> toJson() => {
        'resourceId': resourceId,
        'projectId': projectId,
        'uri': uri,
        'secrets': secrets,
      };
}

class Secret {
  final int userId;
  final String label;
  final String value;

  Secret({
    required this.userId,
    required this.label,
    required this.value,
  });

  factory Secret.fromJson(Map<String, dynamic> json) {
    return Secret(
      userId: json['userid'],
      label: json['label'],
      value: json['value'],
    );
  }

  Map<String, dynamic> toJson() => {
        'userid': userId,
        'label': label,
        'value': value,
      };
}
