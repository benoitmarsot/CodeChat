import 'dart:convert';
import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/models/openai/assistant.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:http/http.dart' as http;

class AssistantService {
  static final String baseUrl = '${AppConfig.apiVersionBaseUrl}/assistants';
  final AuthProvider authProvider;

  AssistantService({required this.authProvider});

  Map<String, String> get _headers => {
        'Authorization': 'Bearer ${authProvider.token}',
        'Content-Type': 'application/json',
      };

  Future<Assistant> updateAssistant(
      int projectId, Map<String, dynamic> assistantData) async {
    final response = await http.post(
      Uri.parse('$baseUrl/$projectId'),
      headers: _headers,
      body: jsonEncode(assistantData),
    );

    if (response.statusCode == 200) {
      final responseData = jsonDecode(response.body);
      return Assistant.fromJson(responseData);
    } else {
      throw Exception('Failed to update assistant: ${response.body}');
    }
  }

  Future<Assistant> getAssistant(int projectId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/$projectId'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      final res = jsonDecode(response.body);
      return Assistant.fromJson(res);
    } else {
      throw Exception('Failed to fetch assistant: ${response.body}');
    }
  }
}

// // Assistant class acts as a higher-level abstraction which uses AssistantService to communicate with the API.
// class Assistant {
//   final AssistantService service;

//   // Constructor requires an instance of AssistantService
//   Assistant({required this.service});

//   // Factory constructor to create an Assistant instance from a JSON map
//   factory Assistant.fromJson(Map<String, dynamic> json) {
//     return Assistant(
//       service: AssistantService.fromJson(json['service']),
//     );
//   }

//   // Converts this instance to a JSON map
//   Map<String, dynamic> toJson() => {
//         'service': service.toJson(),
//       };

//   /// Sends a user message to the assistant API and returns the processed reply as a String.
//   Future<String> getResponse(String message) async {
//     try {
//       final data = await service.sendRequest(message);
//       // Assuming the API returns a field named 'reply' with the assistant's response
//       return data['reply'] ?? 'No reply received';
//     } catch (error) {
//       return 'Error fetching response: $error';
//     }
//   }
// }

// class Assistant {
//   final String name;
//   final String? primaryFunction;
//   final String? reasoningEffort;
//   final String model;
//   final double? temperature;
//   final int? maxResults;

//   Assistant({
//     required this.name,
//     required this.model,
//     this.reasoningEffort,
//     this.primaryFunction,
//     this.temperature,
//     this.maxResults = 10,
//   });
// }

// Example usage (uncomment to run):
// void main() async {
//   // Create an instance of AssistantService
//   final assistantService = AssistantService(
//       baseUrl: 'https://api.example.com',
//       apiKey: 'YOUR_API_KEY');
//
//   // Create an Assistant instance using the service
//   final assistant = Assistant(service: assistantService);
//
//   // Get and print the assistant's response
//   final response = await assistant.getResponse('Hello!');
//   print('Assistant reply: $response');
//
//   // Converting Assistant instance to JSON
//   final jsonData = assistant.toJson();
//   print('Assistant as JSON: $jsonData');
//
//   // Creating an Assistant instance from JSON
//   final newAssistant = Assistant.fromJson(jsonData);
//   final newResponse = await newAssistant.getResponse('Hi again!');
//   print('New Assistant reply: $newResponse');
// }
