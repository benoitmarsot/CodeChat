import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/login_page.dart';
import 'package:codechatui/src/models/openai/assistant.dart';

class ErrorHandler {
  static void handleForbiddenError(BuildContext context, String message) {
    // Log the user out and clear all authentication data
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    authProvider.clearAll();

    // Show a snackbar with the error message
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.red),
    );

    // Redirect to the login page
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (context) => const LoginPage()),
    );
  }
}
