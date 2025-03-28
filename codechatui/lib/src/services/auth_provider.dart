import 'package:codechatui/src/services/secure_storage.dart';
import 'package:flutter/material.dart';
import 'package:jwt_decoder/jwt_decoder.dart';

class AuthProvider with ChangeNotifier {
  String? _token;
  int? _userId;
  String? _sessionId;
  final SecureStorageService _secureStorage = SecureStorageService();

  String? get token => _token;
  int? get userId => _userId;
  String? get sessionId => _sessionId;

  bool get isAuthenticated {
    return _token != null && isTokenExpired == false;
  }

  bool get isTokenExpired {
    return JwtDecoder.isExpired(_token ?? '');
  }

  void setToken(String? newToken) {
    _token = newToken;
    notifyListeners();
  }

  void setUserId(int? newUserId) {
    _userId = newUserId;
    notifyListeners();
  }

  void setSessionId(String? newSessionId) {
    _sessionId = newSessionId;
    notifyListeners();
  }

  Future<void> clearAll() async {
    _token = null;
    _userId = null;
    _sessionId = null;
    // Clear SecureStorageService
    await _secureStorage.clearAll();

    notifyListeners();
  }

  void _authStateChanged() {
    if (!isAuthenticated) {
      clearAll();
    }
  }

  /// Public method to trigger authentication state check
  void checkAuthState() {
    _authStateChanged();
  }
}
