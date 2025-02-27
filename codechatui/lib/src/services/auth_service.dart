import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/app_config.dart';

class AuthService {
  static final String baseUrl = AppConfig.authBaseUrl;

  Future<http.Response> register(String name, String email, String password, String role) async {
    final url = Uri.parse('$baseUrl/register');
    final response = await http.post(
      url,
      headers: <String, String>{
        'Content-Type': 'application/json; charset=UTF-8',
        'User-Agent': 'Dart/2.14 (dart:io)',
      },
      body: jsonEncode(<String, String>{
        'name': name,
        'email': email,
        'password': password,
        'role': role,
      }),
    );
    return response;
  }

  Future<http.Response> login(String email, String password) async {
    final url = Uri.parse('$baseUrl/login');
    final response = await http.post(
      url,
      headers: <String, String>{
        'Content-Type': 'application/json; charset=UTF-8',
        'User-Agent': 'Dart/2.14 (dart:io)',
      },
      body: jsonEncode(<String, String>{
        'email': email,
        'password': password,
      }),
    );

    // Log the request and response headers
    print('Request: ${response.request}');
    response.headers.forEach((key, value) {
      print('$key: $value');
    });

    // Check for JSESSIONID cookie
    if (response.headers.containsKey('set-cookie')) {
      final cookies = response.headers['set-cookie'];
      print('Set-Cookie: $cookies');
      if (cookies != null && cookies.contains('JSESSIONID')) {
        print('JSESSIONID cookie received');
      }
    } else {
      print('Set-Cookie header not found');
    }

    return response;
  }

  Future<http.Response> refreshToken(String refreshToken) async {
    final url = Uri.parse('$baseUrl/refresh');
    return await http.post(
      url,
      headers: <String, String>{
        'Content-Type': 'application/json; charset=UTF-8',
      },
      body: jsonEncode(<String, String>{
        'refreshToken': refreshToken,
      }),
    );
  }
}