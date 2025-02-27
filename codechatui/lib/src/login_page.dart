import 'dart:convert'; // Import the dart:convert package
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/services/auth_service.dart';
import 'package:codechatui/src/services/secure_storage.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  String? _errorMessage;
  final SecureStorageService _secureStorage = SecureStorageService();
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final AuthService _authService = AuthService();

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
        final token = responseData['token'];
        final userId = responseData['userId'];
        print('Login successful:\n  userId: $userId,\n token: $token');
        await _secureStorage.storeToken(token);
        await _secureStorage.storeUserId(userId);
        if (mounted) {  // Check if the widget is still in the tree
          var authProvider = Provider.of<AuthProvider>(context, listen: false);
          authProvider.setToken(token);
          authProvider.setUserId(
            userId == null ? null : int.parse(userId)
          );
          print('Debug - Token set in Login._handleLogin: $token');
          Navigator.of(context).pushReplacementNamed('home');  // or whatever your main route is named
        }
      } else if (response.statusCode == 401) {
        setState(() {
          _errorMessage = 'Login failed: Invalid credentials';
        });
      } else if (response.statusCode == 403) {
        setState(() {
          _errorMessage = 'Login failed: Token expired';
        });
        // Implement token refresh logic here
        await _refreshToken();
      } else {
        setState(() {
          _errorMessage = 'Login failed: Unknown error';
        });
      }
    } catch (e) {
      setState(() {
        print(e);
        _errorMessage = 'Connection error: Unable to reach server';
      });
    }
  }

  Future<void> _refreshToken() async {
    try {
      final refreshToken = await _secureStorage.getRefreshToken();
      if (refreshToken != null) {
        final response = await _authService.refreshToken(refreshToken);
        if (response.statusCode == 200) {
          final responseData = jsonDecode(response.body);
          final newToken = responseData['token'];
          await _secureStorage.storeToken(newToken);
          var authProvider = Provider.of<AuthProvider>(context, listen: false);
          authProvider.setToken(newToken);
          print('Debug - Token refreshed: $newToken');
          Navigator.of(context).pushReplacementNamed('home');
        } else {
          setState(() {
            _errorMessage = 'Token refresh failed: Invalid refresh token';
          });
        }
      } else {
        setState(() {
          _errorMessage = 'Token refresh failed: No refresh token available';
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