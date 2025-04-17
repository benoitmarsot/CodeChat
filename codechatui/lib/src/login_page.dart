import 'dart:convert';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/services/auth_service.dart';
import 'package:codechatui/src/services/secure_storage.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:google_sign_in/google_sign_in.dart';

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
    // if (token != null && mounted) {
    //   Navigator.of(context).pushReplacementNamed('');
    // }
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
        final responseData = jsonDecode(utf8.decode(response.bodyBytes));
        final token = responseData['token'];
        final userId = responseData['userId'];
        print('Login successful:\n  userId: $userId,\n token: $token');
        await _secureStorage.storeToken(token);
        await _secureStorage.storeUserId(userId);
        if (mounted) {
          // Check if the widget is still in the tree
          var authProvider = Provider.of<AuthProvider>(context, listen: false);
          authProvider.setToken(token);
          authProvider.setUserId(userId == null ? null : int.parse(userId));
          print('Debug - Token set in Login._handleLogin: $token');
          Navigator.of(context)
              .pushReplacementNamed('home'); // Navigate to home page
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
          final responseData = jsonDecode(utf8.decode(response.bodyBytes));
          final newToken = responseData['token'];
          await _secureStorage.storeToken(newToken);
          if (mounted) {
            var authProvider =
                Provider.of<AuthProvider>(context, listen: false);
            authProvider.setToken(newToken);
            print('Debug - Token refreshed: $newToken');
            Navigator.of(context).pushReplacementNamed('home');
          }
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

  // Google Sign-In
  Future<void> _handleGoogleSignIn() async {
    try {
      final GoogleSignInAccount? googleUser = await GoogleSignIn().signIn();
      if (googleUser == null) {
        // User cancelled the sign-in flow.
        return;
      }

      final GoogleSignInAuthentication googleAuth =
          await googleUser.authentication;
      final String? accessToken = googleAuth.accessToken;

      // Send the accessToken to your backend for verification and token exchange
      final response = await _authService.googleLogin(accessToken!);

      if (response.statusCode == 200) {
        final responseData = jsonDecode(utf8.decode(response.bodyBytes));
        final token = responseData['token'];
        final userId = responseData['userId'];

        await _secureStorage.storeToken(token);
        await _secureStorage.storeUserId(userId);

        if (mounted) {
          var authProvider = Provider.of<AuthProvider>(context, listen: false);
          authProvider.setToken(token);
          authProvider.setUserId(userId == null ? null : int.parse(userId));
          Navigator.of(context).pushReplacementNamed('home');
        }
      } else {
        setState(() {
          _errorMessage = 'Google login failed: ${utf8.decode(response.bodyBytes)}';
        });
      }
    } catch (error) {
      setState(() {
        _errorMessage = 'Google sign-in failed: $error';
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
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Image.asset(
            //   'assets/logo-ragtime.png',
            //   width: 200,
            //   height: 200,
            //   fit: BoxFit.contain,
            // ),
            SvgPicture.asset(
              'logo.svg',
              semanticsLabel: 'My SVG Image',
              width: 200, // Optional: specify width
              height: 200, // Optional: specify height
              fit: BoxFit
                  .contain, // Optional: specify how the SVG should be scaled
            ),
            RichText(
              text: TextSpan(
                children: [
                  TextSpan(
                    text: 'RAG',
                    style: GoogleFonts.scada(
                      fontSize: 48,
                      fontWeight: FontWeight.w700,
                      color: const Color(0xFF1E4396), // Original blue color
                    ),
                  ),
                  TextSpan(
                    text: 'time',
                    style: GoogleFonts.scada(
                      fontSize: 48,
                      fontWeight: FontWeight.w700,
                      fontStyle: FontStyle.italic,
                      color: const Color(0xFFFF9800), // Amber/orange color that complements blue
                    ),
                  ),
                ],
              ),
            ),
            // Text(
            //   'SAMTAL AI',
            //   style: GoogleFonts.scada(
            //     textStyle: Theme.of(context).textTheme.displayLarge,
            //     fontSize: 48,
            //     fontWeight: FontWeight.w700,
            //     color: const Color(0xFF1E4396),
            //   ),
            // ),
            SizedBox(height: 24.0),
            Card(
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
                    const SizedBox(height: 16.0),
                    // ElevatedButton(
                    //   // Google Sign-In Button
                    //   onPressed: _handleGoogleSignIn,
                    //   child: const Text('Sign in with Google'),
                    // ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
