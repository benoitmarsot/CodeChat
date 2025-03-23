import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import 'package:universal_io/io.dart'; // Import for File

class ZipForm extends StatefulWidget {
  final Function(String?) onFileSelected;

  const ZipForm({Key? key, required this.onFileSelected}) : super(key: key);

  @override
  _ZipFormState createState() => _ZipFormState();
}

class _ZipFormState extends State<ZipForm> {
  String? _selectedFilePath;

  Future<void> _pickFile() async {
    FilePickerResult? result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['zip'],
    );

    if (result != null) {
      setState(() {
        _selectedFilePath = result.files.single.path;
        widget.onFileSelected(_selectedFilePath); // Notify parent
      });
    } else {
      // User canceled the picker
      widget.onFileSelected(null); // Notify parent
    }
  }

  void _handleDrop(String? path) {
    setState(() {
      _selectedFilePath = path;
      widget.onFileSelected(_selectedFilePath); // Notify parent
    });
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        DragTarget<String>(
          onWillAcceptWithDetails: (data) => true,
          onAcceptWithDetails: (details) {
            _handleDrop(details.data);
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
                    ? Text('Drop Zip File Here')
                    : Text('Selected file: $_selectedFilePath'),
              ),
            );
          },
        ),
        ElevatedButton(
          onPressed: _pickFile,
          child: Text('Select Zip File'),
        ),
        if (_selectedFilePath != null)
          Padding(
            padding: const EdgeInsets.only(top: 8.0),
            child: Text('Selected file: $_selectedFilePath'),
          ),
      ],
    );
  }
}
