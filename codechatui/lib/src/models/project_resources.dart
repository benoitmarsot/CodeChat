typedef Secrets = Map<String, String>;

enum ResourceType { git, zip, web }

class ProjectResourceBase {
  ResourceType restype;
  String? uri;

  ProjectResourceBase({required this.restype, this.uri});
}

class ProjectResource extends ProjectResourceBase {
  final int? resourceId;
  final int? projectId;
  final Map<String, Secret>? secrets;

  ProjectResource({
    this.resourceId,
    this.projectId,
    required String uri,
    required ResourceType restype,
    this.secrets,
  }) : super(restype: restype, uri: uri);

  factory ProjectResource.fromJson(Map<String, dynamic> json) {
    // ResourceType determineResourceType(String type) {
    //   return ResourceType.values.firstWhere(
    //     (e) => e.toString().split('.').last == type,
    //     orElse: () => ResourceType.web,
    //   );
    // }

    return ProjectResource(
      resourceId: json['prid'],
      projectId: json['projectid'],
      uri: json['uri'] ?? '',
      restype: json['restype'] != null
          ? ResourceType.values.byName(json['restype'])
          : ResourceType.web,
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
        'restype': restype.toString().split('.').last,
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
