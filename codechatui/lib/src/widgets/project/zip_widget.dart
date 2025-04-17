import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_dropzone/flutter_dropzone.dart';
import 'package:file_picker/file_picker.dart';

class ZipForm extends StatefulWidget {
  final Function(String, String) onFileSelected;
  final int projectId;
  final bool isLoading;
  final String message = "Adding files to your project. Please wait...";
  final List<String>? debugMessages;

  const ZipForm({
    required this.onFileSelected,
    this.isLoading = false,
    this.debugMessages,
    this.projectId = -1, // Replace with actual project ID
    Key? key,
  }) : super(key: key);
  @override
  _ZipFormState createState() => _ZipFormState();
}

class _ZipFormState extends State<ZipForm> {
  String? _selectedFilePath;
  //List<String>? _debugMessages;
  final ScrollController _scrollController = ScrollController();
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

  void _scrollToBottom() {
    Future.delayed(const Duration(milliseconds: 100), () {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  void didUpdateWidget(covariant ZipForm oldWidget) {
    super.didUpdateWidget(oldWidget);

// Compare the contents of the lists
    if (widget.debugMessages != null &&
        oldWidget.debugMessages != null &&
        !listEquals(widget.debugMessages, oldWidget.debugMessages)) {
      _scrollToBottom();
    }
    // Check if debugMessages has changed
    // if (widget.debugMessages != oldWidget.debugMessages) {
    //   _scrollToBottom();
    // }
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
          child: Stack(
              children: widget.debugMessages!.isEmpty
                  ? [
                      DropzoneView(
                        onCreated: (controller) =>
                            _dropzoneController = controller,
                        onDropFile: (file) async {
                          try {
                            final bytes =
                                await _dropzoneController.getFileData(file);
                            final fileName =
                                await _dropzoneController.getFilename(file);
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
                    ]
                  : [
                      ListView.builder(
                        controller: _scrollController,
                        itemCount: widget.debugMessages!.length,
                        itemBuilder: (context, index) {
                          return Padding(
                              padding: const EdgeInsets.all(4.0),
                              child: Text(widget.debugMessages![index],
                                  style: TextStyle(
                                      color: Theme.of(context)
                                          .colorScheme
                                          .secondary)));
                        },
                      )
                    ]),
        ),
      ],
    );
  }
}
