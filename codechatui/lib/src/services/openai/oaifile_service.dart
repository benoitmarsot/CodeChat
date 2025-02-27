import 'dart:convert';
import 'package:http/http.dart' as http;
import 'dart:typed_data';
import '../../config/app_config.dart';
import '../auth_provider.dart';

class OaiFile {
  final int id;
  final int userId;
  final int projectId;
  final String fileId;
  final String fileName;
  final String parentPath;
  final String fullPath;
  final String purpose;
  final int lineCount;

  OaiFile({
    required this.id,
    required this.userId,
    required this.projectId,
    required this.fileId,
    required this.fileName,
    required this.parentPath,
    required this.fullPath,
    required this.purpose,
    required this.lineCount,
  });

  factory OaiFile.fromJson(Map<String, dynamic> json) {
    return OaiFile(
      id: json['id'],
      userId: json['userId'],
      projectId: json['projectId'],
      fileId: json['fileId'],
      fileName: json['fileName'],
      parentPath: json['parentPath'],
      fullPath: json['fullPath'],
      purpose: json['purpose'],
      lineCount: json['lineCount'],
    );
  }
}

class OaiFileService {
  static final String openaiBaseUrl=AppConfig.openaiBaseUrl;
  final AuthProvider authProvider;

  OaiFileService({required this.authProvider});

  Map<String, String> get _headers => {
        'Authorization': 'Bearer ${authProvider.token}',
        'Content-Type': 'application/json',
        //'Cookie': 'JSESSIONID=${authProvider.sessionId}',
      };

  Future<List<OaiFile>> getMyRepoFiles(int projectId) async {
    final response = await http.get(
      Uri.parse('$openaiBaseUrl/files/myrepo?projectId=$projectId'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      final List<dynamic> jsonList = json.decode(response.body);
      return jsonList.map((json) => OaiFile.fromJson(json)).toList();
    }
    throw Exception('Failed to get repo files');
  }
  Future<String> test() async {
    final uri = Uri.parse('$openaiBaseUrl/files/test');
    print('Debug - Full request details:');
    print('URL: $uri');
    print('Token present: ${authProvider.token != null}');
    print('Headers: ${_headers.toString()}');
    final response = await http.get(
      uri,
      headers: _headers,
    );

    print('Response received:');
    print('Status code: ${response.statusCode}');
    print('Response headers: ${response.headers}');
    print('Response body: ${response.body}');

    if (response.statusCode == 200) {
      final String body = response.body;
      return body;
    }
    throw Exception('Failed to get test');
  }

  Future<List<OaiFile>> getAllFiles() async {
    final response = await http.get(
      Uri.parse('$openaiBaseUrl/files'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      final List<dynamic> jsonList = json.decode(response.body);
      return jsonList.map((json) => OaiFile.fromJson(json)).toList();
    }
    throw Exception('Failed to get all files');
  }

  Future<Map<String, OaiFile>> uploadDirectory(
      String rootDir, String extension, String purpose, int projectId
  ) async {
    final uri = '$openaiBaseUrl/files/uploadDir?projectId=$projectId';
    final requestBody = {
      'rootDir': rootDir,
      'extension': extension,
      'purpose': purpose,
    };

    try {
      final response = await http.post(
        Uri.parse(uri),
        headers: _headers,
        body: json.encode(requestBody),
      );      

      if (response.statusCode == 200) {
        final Map<String, dynamic> jsonMap = json.decode(response.body);
        return Map.fromEntries(
          jsonMap.entries.map(
            (entry) => MapEntry(
              entry.key,
              OaiFile.fromJson(entry.value as Map<String, dynamic>),
            ),
          ),
        );
      }
      throw Exception('Failed to upload directory: ${response.statusCode} - ${response.body}');
    } catch (e, stackTrace) {
      print('Exception type: ${e.runtimeType}');
      print('Exception message: $e');
      print('Stack trace: $stackTrace');
      rethrow;
    }
  }

  Future<OaiFile> uploadFile(
      String filepath, String purpose, int projectId) async {
    final response = await http.put(
      Uri.parse('$openaiBaseUrl/files/uploadFile?projectId=$projectId'),
      headers: _headers,
      body: json.encode({
        'filepath': filepath,
        'purpose': purpose,
      }),
    );

    if (response.statusCode == 200) {
      return OaiFile.fromJson(json.decode(response.body));
    }
    throw Exception('Failed to upload file');
  }

  Future<OaiFile> getFile(String fileId) async {
    final response = await http.get(
      Uri.parse('$openaiBaseUrl/files/$fileId'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      return OaiFile.fromJson(json.decode(response.body));
    }
    throw Exception('Failed to get file');
  }

  Future<void> deleteFile(String fileId) async {
    final response = await http.delete(
      Uri.parse('$openaiBaseUrl/files/$fileId'),
      headers: _headers,
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to delete file');
    }
  }

  Future<void> deleteAllFiles() async {
    final response = await http.delete(
      Uri.parse('$openaiBaseUrl/files/all'),
      headers: _headers,
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to delete all files');
    }
  }

  Future<List<OaiFile>> getFilesFromRootDir(
      String rootDir, int projectId) async {
    final response = await http.get(
      Uri.parse(
          '$openaiBaseUrl/files/rootDir?rootDir=$rootDir&projectId=$projectId'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      final List<dynamic> jsonList = json.decode(response.body);
      return jsonList.map((json) => OaiFile.fromJson(json)).toList();
    }
    throw Exception('Failed to get files from root directory');
  }

  Future<Map<String, dynamic>> getFileInfo(String fileId) async {
    final response = await http.get(
      Uri.parse('$openaiBaseUrl/files/fileinfo/$fileId'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      return json.decode(response.body);
    }
    throw Exception('Failed to get file info');
  }

  Future<void> downloadFileInto(
      String fileId, String outPath) async {
    final response = await http.get(
      Uri.parse(
          '$openaiBaseUrl/files/downloadInto/$fileId?outPath=$outPath'),
      headers: _headers,
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to download file');
    }
  }

  Future<Uint8List> downloadFile(String fileId) async {
    final response = await http.get(
      Uri.parse('$openaiBaseUrl/files/download/$fileId'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      return response.bodyBytes;
    }
    throw Exception('Failed to download file');
  }
}
