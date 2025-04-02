import 'dart:convert';
import 'package:codechatui/src/models/user.dart';
import 'package:http/http.dart' as http;
import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/services/auth_provider.dart';

class UserService {
  static final String baseUrl = '${AppConfig.apiVersionBaseUrl}/users';
  final AuthProvider authProvider;

  UserService({required this.authProvider});

  Map<String, String> get _headers => {
        'Authorization': 'Bearer ${authProvider.token}',
        'Content-Type': 'application/json',
      };

  Future<User> getCurrentUser() async {
    final response = await http.get(
      Uri.parse('$baseUrl/current-user'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      return User.fromJson(json.decode(response.body));
    } else if (response.statusCode == 401) {
      throw Exception('Unauthorized. Please log in again.');
    } else {
      throw Exception('Failed to fetch current user');
    }
  }
}
