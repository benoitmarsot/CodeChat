import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import 'package:file_picker/file_picker.dart';

class ZipForm extends StatefulWidget {
  final Function(String, String) onFileSelected;
  final int projectId; // Replace with actual project ID
  const ZipForm(
      {required this.onFileSelected,
      this.projectId = 8,
      Key? key}) //remove hardcoded id
      : super(key: key);

  @override
  _ZipFormState createState() => _ZipFormState();
}

class _ZipFormState extends State<ZipForm> {
  String? _selectedFilePath;

  int? _projectId;

  @override
  void initState() {
    super.initState();
    _projectId = widget.projectId;
  }

  Future<String> _encodeFileToBase64(String filePath) async {
    final file = File(filePath);
    final fileBytes = await file.readAsBytes();
    return base64Encode(fileBytes);
  }

  Future<Map<String, dynamic>> _prepareZipContentPayload(
      String filePath) async {
    final base64Content = await _encodeFileToBase64(filePath);
    return {
      "zipContent": base64Content,
    };
  }

  Future<void> _sendZipContent(String filePath) async {
    final payload = await _prepareZipContentPayload(filePath);

    final response = await http.post(
      Uri.parse('https://your-api-endpoint.com/upload'),
      headers: {
        'Content-Type': 'application/json',
      },
      body: jsonEncode(payload),
    );

    if (response.statusCode == 200) {
      print('Upload successful: ${response.body}');
    } else {
      print('Failed to upload: ${response.statusCode} ${response.body}');
    }
  }

  Future<void> selectFile() async {
    // Use FilePicker to select a file
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['zip'], // Restrict to ZIP files
    );

    if (result != null && result.files.single.bytes != null) {
      // Get the file bytes and name
      final bytes = result.files.single.bytes!;
      final fileName = result.files.single.name;
      try {
        // Encode the file content in Base64
        final base64Content = base64Encode(bytes);
        widget.onFileSelected(base64Content, fileName);
        // await codechatService.addProjectZip(
        //      _projectId!, base64Content, fileName);
      } catch (e) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to upload file: $e')),
        );
      }
    }
  }

  //  Future<void> _handleDrop(String? path) async {
  //   final authProvider = Provider.of<AuthProvider>(context, listen: false);
  //   final codechatService = CodechatService(authProvider: authProvider);
  //   if (path != null) {
  //     setState(() {
  //       _selectedFilePath = path;
  //     });

  //     try {
  //       await codechatService.addProjectZip(_projectId!, path);
  //       ScaffoldMessenger.of(context).showSnackBar(
  //         SnackBar(content: Text('File uploaded successfully!')),
  //       );
  //     } catch (e) {
  //       ScaffoldMessenger.of(context).showSnackBar(
  //         SnackBar(content: Text('Failed to upload file: $e')),
  //       );
  //     }
  //   }
  // }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        DragTarget<String>(
          onWillAcceptWithDetails: (data) => true,
          onAcceptWithDetails: (details) {
            print(details.data);
          },
          builder: (BuildContext context, List<String?> candidateData,
              List<dynamic> rejectedData) {
            return Container(
              height: 200,
              width: 300,
              decoration: BoxDecoration(
                border: Border.all(
                  color: candidateData.isNotEmpty ? Colors.blue : Colors.grey,
                  width: 2.0,
                ),
              ),
              child: Center(
                child: _selectedFilePath == null
                    ? Text('Drop Zip File Here /n (Currently unsupported)')
                    : Text('Selected file: $_selectedFilePath'),
              ),
            );
          },
        ),
        ElevatedButton(
          onPressed: () async {
            // Simulate file selection for testing

            await selectFile();
          },
          child: Text('Select Zip file for upload'),
        ),
      ],
    );
  }
}
