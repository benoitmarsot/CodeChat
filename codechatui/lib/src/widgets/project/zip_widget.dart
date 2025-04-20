import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_dropzone/flutter_dropzone.dart';
import 'package:file_picker/file_picker.dart';

class ZipForm extends StatefulWidget {
  final Function(String, String) onFileSelected;
  final int projectId;
  final bool isLoading;
  final String message = "Adding files to your project. Please wait...";
  // final List<String>? debugMessages;

  const ZipForm({
    required this.onFileSelected,
    this.isLoading = false,
    this.projectId = -1, // Replace with actual project ID
    Key? key,
  }) : super(key: key);
  @override
  _ZipFormState createState() => _ZipFormState();
}

class _ZipFormState extends State<ZipForm> {
  late DropzoneViewController _dropzoneController;

  Future<void> selectFile() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['zip'], // Restrict to ZIP files
    );

    if (result != null && result.files.single.bytes != null) {
      final bytes = result.files.single.bytes!;
      final fileName = result.files.single.name;

      try {
        final base64Content = base64Encode(bytes);
        await widget.onFileSelected(base64Content, fileName);
      } catch (e) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text('Failed to upload file: $e'),
              backgroundColor: Colors.redAccent),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          height: 150,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey),
            borderRadius: BorderRadius.circular(8.0),
            color: Theme.of(context).colorScheme.primary.withOpacity(0.1),
          ),
          child: Stack(children: [
            DropzoneView(
              onCreated: (controller) => _dropzoneController = controller,
              onDropFile: (file) async {
                try {
                  final bytes = await _dropzoneController.getFileData(file);
                  final fileName = await _dropzoneController.getFilename(file);
                  final base64Content = base64Encode(bytes);
                  widget.onFileSelected(base64Content, fileName);
                } catch (e) {
                  if (mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                          content: Text('Failed to upload file: $e'),
                          backgroundColor: Colors.redAccent),
                    );
                  }
                }
              },
              onError: (error) {
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                        content: Text('Error: $error'),
                        backgroundColor: Colors.redAccent),
                  );
                }
              },
            ),
            Center(
                child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                  TapRegion(
                      onTapInside: (event) async {
                        await selectFile();
                      },
                      child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.cloud_upload,
                                size: 40, color: Colors.grey),
                            const SizedBox(height: 8),
                            Text(
                              'Drag & drop your Project (ZIP) files here, or click to upload.',
                              style: TextStyle(color: Colors.grey),
                            )
                          ]))
                ]))
          ]),
        ),
      ],
    );
  }
}
