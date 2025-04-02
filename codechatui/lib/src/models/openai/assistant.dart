class Assistant {
  final String name;
  final String? instruction;
  final String? primaryFunction;
  final String? reasoningEffort;
  final String model;
  final double? temperature;
  final int? maxResults;

  Assistant({
    required this.name,
    this.instruction,
    required this.model,
    this.primaryFunction,
    this.temperature,
    this.reasoningEffort,
    this.maxResults = 10,
  });
  // Factory constructor to create an Assistant instance from a JSON map
  factory Assistant.fromJson(Map<String, dynamic> json) {
    return Assistant(
      name: json['name'],
      instruction: json['instruction'],
      reasoningEffort: json['reasoningEffort'],
      primaryFunction: json['primaryFunction'],
      model: json['model'],
      temperature: json['temperature']?.toDouble(),
      maxResults: json['maxResults'],
    );
  }

  // Converts this instance to a JSON map
  Map<String, dynamic> toJson() {
    var json = {
      'name': name,
      //'instruction': instruction, // Exclude in request
      //'reasoningEffort': reasoningEffort,
      'primaryFunction': primaryFunction,
      'model': model,
      'maxResults': maxResults,
    };

    // Add temperature only if it is supported for the model
    if (isAttributeSupported(model, 'temperature') && temperature != null) {
      json['temperature'] = temperature;
    }
    if (isAttributeSupported(model, 'reasoningEffort') &&
        reasoningEffort != null) {
      json['reasoningEffort'] = reasoningEffort;
    }
    return json;
  }

  Assistant copyWith({
    String? name,
    String? instruction,
    String? reasoningEffort,
    String? model,
    double? temperature,
    int? maxResults,
  }) {
    return Assistant(
      name: name ?? this.name,
      instruction: instruction ?? this.instruction,
      reasoningEffort: reasoningEffort ?? this.reasoningEffort,
      model: model ?? this.model,
      temperature: temperature ?? this.temperature,
      maxResults: maxResults ?? this.maxResults,
    );
  }

  Assistant validatedAssistant({
    String? name,
    String? primaryFunction,
    String? reasoningEffort,
    String? model,
    double? temperature,
    int? maxResults,
  }) {
    final usedModel = model ?? this.model;
    return Assistant(
      name: name ?? this.name,
      primaryFunction: primaryFunction ?? this.primaryFunction,
      reasoningEffort: isAttributeSupported(usedModel, 'reasoningEffort')
          ? reasoningEffort ?? this.reasoningEffort
          : null,
      model: usedModel,
      temperature: isAttributeSupported(usedModel, 'temperature')
          ? temperature ?? this.temperature
          : null,
      maxResults: maxResults ?? this.maxResults,
    );
  }
}

bool isAttributeSupported(String model, String attribute) {
  return !(unsupportedModelAttributes[model]?.contains(attribute) ?? false);
}

final Map<String, Set<String>> unsupportedModelAttributes = {
  'o3_mini': {'temperature'},
  'gpt_4': {'reasoningEffort'},
  'gpt_4o': {'reasoningEffort'},
  'gpt_4o_mini': {'reasoningEffort'}
};
