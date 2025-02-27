import 'package:flutter/material.dart';

class AuthProvider with ChangeNotifier {
  String? _token;
  int? _userId;
  String? _sessionId;

  String? get token => _token;
  int? get userId => _userId;
  String? get sessionId => _sessionId; 

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
  void clearAll() {
    _token = null;
    _userId = null;
    _sessionId = null;
    notifyListeners();
  }

}