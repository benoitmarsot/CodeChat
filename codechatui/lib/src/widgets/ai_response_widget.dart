import 'package:flutter/material.dart';
import 'package:codechatui/src/models/message.dart';
import 'package:flutter_syntax_view/flutter_syntax_view.dart';
import 'package:flutter_markdown/flutter_markdown.dart';  // Add this import
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher.dart'; 
import 'package:open_file/open_file.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/services.dart';

class HoverCopyWidget extends StatefulWidget {
  final String text;
  final Widget child;
  final bool isCodeSection; // new parameter
  const HoverCopyWidget({super.key, required this.text, required this.child, this.isCodeSection = false});
  
  @override
  _HoverCopyWidgetState createState() => _HoverCopyWidgetState();
}

class _HoverCopyWidgetState extends State<HoverCopyWidget> {
  bool _hover = false;
  
  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      onEnter: (_) => setState(() => _hover = true),
      onExit: (_) => setState(() => _hover = false),
      child: Stack(
        children: [
          widget.child,
          if (_hover)
            Positioned(
              // changed code: for code sections, align 4px from the top left; otherwise use the original position
              top: widget.isCodeSection ? 4 : -12,
              right: widget.isCodeSection ? 4 : -12,
              child: IconButton(
                // changed code: if code section, use white icon color
                icon: Icon(Icons.copy, size: 16, color: Theme.of(context).colorScheme.outline),
                tooltip: 'Copy text',
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: widget.text));
                },
              
              ),
            ),
        ],
      ),
    );
  }
}

class AIResponseWidget extends StatelessWidget {
  final Message message;
  final dateFormat = DateFormat('HH:mm');

  AIResponseWidget({super.key, required this.message});

  
  void openLink(String? text, String? href, String? title, [BuildContext? context]) {
    if (href == null) return;
    
    if (href.startsWith('file://')) {
      final filePath = href.replaceFirst('file://', '');
      print("Opening local file: $filePath");
      
      // Handle platform-specific file opening
      if (kIsWeb) {
        print("File opening not supported on web. Path: $filePath");
        
        // If context is available, show a snackbar
        if (context != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('File links cannot be opened in web version: $filePath'),
              duration: const Duration(seconds: 3),
            ),
          );
        }
      } else {
        try {
          OpenFile.open(filePath);
        } catch (e) {
          print("Error opening file: $e");
          if (context != null) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('Error opening file: $filePath'),
                duration: const Duration(seconds: 2),
              ),
            );
          }
        }
      }
    } else {
      // Launch web URLs
      try {
        launchUrl(Uri.parse(href), mode: LaunchMode.externalApplication);
      } catch (e) {
        print("Error opening URL: $e");
      }
    }
  }
  @override
  Widget build(BuildContext context) {
    // Parse the AI response from the message text
    final aiResponse = message.aiResponse;

    if (aiResponse == null) {
      // Fallback for regular text if parsing fails
      return _buildSimpleMessage(context, message.text);
    }

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          // AI response container
          Flexible(
            flex: 3,
            child: Container(
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.surfaceContainerLow,
                borderRadius: BorderRadius.circular(8),
              ),
              margin: const EdgeInsets.only(right: 60),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Process each answer item
                  ...aiResponse.answers.map((answer) => _buildAnswerItem(context, answer)),
                  
                  // Conversational guidance
                  if (aiResponse.conversationalGuidance != null)
                  const Divider(),
                    Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: HoverCopyWidget(
                        text: aiResponse.conversationalGuidance!,
                        child: SizedBox(
                          width: double.infinity,
                          child: MarkdownBody(
                            data: aiResponse.conversationalGuidance!,
                            selectable: true,
                            styleSheet: MarkdownStyleSheet(
                              p: Theme.of(context).textTheme.bodyMedium,
                              code: TextStyle(
                                backgroundColor: Theme.of(context).colorScheme.surfaceContainerHighest,
                                fontFamily: 'monospace',
                                fontSize: 14,
                              ),
                            ),
                            onTapLink: (text, href, title) {
                              openLink(text, href, title, context);
                            },
                          ),
                        ),
                      ),
                    ),
                  // Timestamp
                  Padding(
                    padding: const EdgeInsets.only(left: 12.0, bottom: 4.0, top: 4.0),
                    child: Text(
                      dateFormat.format(message.timestamp),
                      style: TextStyle(
                        fontSize: 12,
                        color:  Theme.of(context).colorScheme.surfaceContainer,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const Spacer(),
        ],
      ),
    );
  }

  Widget _buildSimpleMessage(BuildContext context, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          CircleAvatar(
            backgroundColor: Colors.blue[100],
            child: const Icon(Icons.smart_toy_outlined, color: Colors.blue),
          ),
          const SizedBox(width: 8),
          Flexible(
            flex: 3,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                HoverCopyWidget(
                  text: text,
                  child: Container(
                    width: double.infinity, // ensures stack covers full bubble
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surfaceContainerLowest,
                      borderRadius: BorderRadius.circular(18),
                    ),
                    child: MarkdownBody(
                      data: text,
                      selectable: true,
                      styleSheet: MarkdownStyleSheet(
                        p: Theme.of(context).textTheme.bodyMedium,
                        code: TextStyle(
                          backgroundColor: Theme.of(context).colorScheme.surfaceContainerLow,
                          fontFamily: 'monospace',
                          fontSize: 14,
                        ),
                      ),
                      onTapLink: (text, href, title) {
                        openLink(text, href, title, context);
                      },
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(left: 8.0, top: 2.0),
                  child: Text(
                    dateFormat.format(message.timestamp),
                    style: TextStyle(
                      fontSize: 12,
                      color: Theme.of(context).colorScheme.surfaceContainerHighest,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const Spacer(),
        ],
      ),
    );
  }

  Widget _buildAnswerItem(BuildContext context, AIAnswerItem answer) {
    return Padding(
      padding: const EdgeInsets.all(12.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Replace Text with Markdown for explanation
          HoverCopyWidget(
            text: answer.explanation,
            child: Container(
              width: double.infinity,
              child: MarkdownBody(
                data: answer.explanation,
                selectable: true,
                styleSheet: MarkdownStyleSheet(
                  p: Theme.of(context).textTheme.bodyMedium,
                  code: TextStyle(
                    backgroundColor: Theme.of(context).colorScheme.surfaceContainerHighest,
                    fontFamily: 'monospace',
                    fontSize: 14,
                  ),
                  h1: Theme.of(context).textTheme.headlineMedium,
                  h2: Theme.of(context).textTheme.titleLarge,
                  h3: Theme.of(context).textTheme.titleMedium,
                  blockquote: TextStyle(
                    color: Theme.of(context).colorScheme.surfaceContainerHigh,
                    fontStyle: FontStyle.italic,
                  ),
                  blockquoteDecoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surfaceContainerLowest,
                    borderRadius: BorderRadius.circular(2),
                    border: Border(
                      left: BorderSide(color: Colors.grey[400]!, width: 4),
                    ),
                  ),
                ),
                onTapLink: (text, href, title) {
                  openLink(text, href, title, context);
                },
              ),
            ),
          ),
          
          if (answer.code != null && answer.language != null)
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 8),
                HoverCopyWidget(
                  isCodeSection: true, // changed code: mark as code section for white icon and left alignment
                  text: answer.code!,
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.grey[300]!),
                    ),
                    width: double.infinity,
                    child: Container(
                      decoration: BoxDecoration(
                        color: const Color(0xFF1E1E1E),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: Colors.grey[700]!),
                      ),
                      width: double.infinity,
                      alignment: Alignment.topLeft,
                      child: SyntaxView(
                        code: answer.code!,
                        syntax: answer.getSyntaxLanguage(),
                        syntaxTheme: SyntaxTheme.vscodeDark(),
                        withZoom: true,
                        withLinesCount: true,
                        fontSize: 12,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                HoverCopyWidget(
                  text: answer.codeExplanation ?? '',
                  child: SizedBox(
                    width: double.infinity,
                    child: MarkdownBody(
                      data: answer.codeExplanation ?? '',
                      selectable: true,
                      styleSheet: MarkdownStyleSheet(
                        p: Theme.of(context).textTheme.bodyMedium,
                        code: TextStyle(
                          backgroundColor: Theme.of(context).colorScheme.surfaceContainerHighest,
                          fontFamily: 'monospace',
                          fontSize: 14,
                        ),
                      ),
                      onTapLink: (text, href, title) {
                        openLink(text, href, title, context);
                      },
                    ),
                  ),
                ),
              ],
            ),
          
          if (answer.references != null && answer.references!.isNotEmpty)
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 8),
                const Text(
                  "References:",
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                ...answer.references!.map((ref) => Padding(
                  padding: const EdgeInsets.only(bottom: 2.0),
                  child: HoverCopyWidget(
                    text: ref,
                    child: Container(
                      width: double.infinity,
                      child: MarkdownBody(
                        data: ref,
                        selectable: true,
                        styleSheet: MarkdownStyleSheet(
                          p: Theme.of(context).textTheme.bodyMedium,
                          code: TextStyle(
                            backgroundColor: Colors.grey[300],
                            fontFamily: 'monospace',
                            fontSize: 14,
                          ),
                        ),
                        onTapLink: (text, href, title) {
                          openLink(text, href, title, context);
                        },
                        onSelectionChanged: (text, selection, cause) => print('Selection: $text'),
                      ),
                    ),
                  ),
                )),
              ],
            ),
        ],
      ),
    );
  }
}
