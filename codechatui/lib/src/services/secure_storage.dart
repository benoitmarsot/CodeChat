import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SecureStorageService {
  final _storage = const FlutterSecureStorage();
  static const String _refreshTokenKey = 'refresh_token';
  
  Future<void> storeToken(String token) async {
    await _storage.write(key: 'jwt_token', value: token);
  }

  Future<void> storeUserId(String userId) async {
    await _storage.write(key: 'user_id', value: userId);
  }

  Future<String?> getToken() async {
    return await _storage.read(key: 'jwt_token');
  }

  Future<int?> getUserId() async {
    String? stUserId= await _storage.read(key: 'user_id');
    return stUserId != null?int.parse(stUserId):null;
  }

  Future<void> storeRefreshToken(String token) async {
    await _storage.write(key: _refreshTokenKey, value: token);
  }

  Future<String?> getRefreshToken() async {
    return await _storage.read(key: _refreshTokenKey);
  }

  Future<void> clearAll() async {
    await _storage.deleteAll();
  }
}