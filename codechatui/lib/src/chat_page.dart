import 'package:flutter/material.dart';
import 'package:codechatui/src/models/project.dart';

class ChatPage extends StatefulWidget {
  final Project project;

  const ChatPage({super.key, required this.project});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.project.name),
      ),
      body: Center(
        child: Text('Chat page for project: ${widget.project.name}'),
      ),
    );
  }
}
