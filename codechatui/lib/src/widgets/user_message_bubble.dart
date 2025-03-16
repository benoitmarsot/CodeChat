import 'package:flutter/material.dart';
import 'package:codechatui/src/models/message.dart';
import 'package:intl/intl.dart';
import 'package:flutter/services.dart';

class UserMessageBubble extends StatefulWidget {
  final Message message;
  final DateFormat dateFormat = DateFormat('HH:mm');

  UserMessageBubble({super.key, required this.message});

  @override
  _UserMessageBubbleState createState() => _UserMessageBubbleState();
}

class _UserMessageBubbleState extends State<UserMessageBubble> {
  bool _isHovering = false;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          const Spacer(),
          Flexible(
            flex: 3,
            child: MouseRegion(
              onEnter: (_) => setState(() => _isHovering = true),
              onExit: (_) => setState(() => _isHovering = false),
              child: Stack(
                children: [
                  Padding(
                    padding: const EdgeInsets.only(top: 24), // leave space for icon
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Theme.of(context).colorScheme.surfaceContainerHighest,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      
                        child: SelectableText(
                          widget.message.text,
                        ),
                  
                    ),
                  ),
                  if (_isHovering)
                    Positioned(
                      top: 14,
                      right: -11,
                      child: IconButton(
                        icon:  Icon(Icons.copy, size: 18, color: Theme.of(context).colorScheme.outline),
                        tooltip: 'Copy text',
                        onPressed: () {
                          Clipboard.setData(ClipboardData(text: widget.message.text));
                          // Optionally, add a SnackBar for confirmation
                        },
                      ),
                    ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
