import 'dart:convert'; // Import the dart:convert package
import 'package:flutter/material.dart';
import 'services/auth_service.dart';
import 'services/secure_storage.dart';

class Login extends StatefulWidget {
  const Login({super.key});

  @override
  State<Login> createState() => _LoginState();
}

class _LoginState extends State<Login> {
  String? _errorMessage;
  final SecureStorageService _secureStorage = SecureStorageService();
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final AuthService _authService = AuthService(baseUrl: 'http://localhost:8080/api/v1/auth');

  @override
  void initState() {
    super.initState();
    _checkExistingToken();
  }

  void _checkExistingToken() async {
    final token = await _secureStorage.getToken();
    if (token != null && mounted) {
      Navigator.of(context).pushReplacementNamed('home');
    }
  }
  void _handleLogin() async {
    setState(() {
      _errorMessage = null; // Clear previous error
    });

    try {
      final response = await _authService.login(
        _usernameController.text,
        _passwordController.text,
      );

      if (response.statusCode == 200) {
        final responseData = jsonDecode(response.body);
        print('Login successful:  userId: ${responseData['userId']}, token: ${responseData['token']}');
        await _secureStorage.storeToken(responseData['token']);
        await _secureStorage.storeUserId(responseData['userId']);
        if (mounted) {  // Check if the widget is still in the tree
          Navigator.of(context).pushReplacementNamed('home');  // or whatever your main route is named
        }
      } else {
        setState(() {
          _errorMessage = 'Login failed: Invalid credentials';
        });
      }
    } catch (e) {
      setState(() {
        print(e);
        _errorMessage = 'Connection error: Unable to reach server';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Login'),
      ),
      body: Center(
        child: Card(
          shape: RoundedRectangleBorder(
            side: BorderSide(
              color: theme.colorScheme.primary,
            ),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Container(
            width: 300, // Set a fixed width for the Card
            padding: const EdgeInsets.all(16.0),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          TextField(
            controller: _usernameController,
            decoration: const InputDecoration(
              labelText: 'Username',
            ),
            onSubmitted: (_) => _handleLogin(),
          ),
          const SizedBox(height: 16.0),
          TextField(
            controller: _passwordController,
            decoration: const InputDecoration(
              labelText: 'Password',
            ),
            obscureText: true,
            onSubmitted: (_) => _handleLogin(),
          ),
          const SizedBox(height: 16.0),
          if (_errorMessage != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 16.0),
              child: Text(
                _errorMessage!,
                style: TextStyle(
                  color: theme.colorScheme.error,
                  fontSize: 14,
                ),
              ),
            ),
                ElevatedButton(
                  onPressed: _handleLogin,
                  child: const Text('Login'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}