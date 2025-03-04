import 'package:flutter/material.dart';
import 'package:codechatui/src/models/message.dart';
import 'package:flutter_syntax_view/flutter_syntax_view.dart';
import 'package:intl/intl.dart';

class AIResponseWidget extends StatelessWidget {
  final Message message;
  final dateFormat = DateFormat('HH:mm');

  AIResponseWidget({super.key, required this.message});

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
                color: Colors.grey[100],
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.grey[300]!),
              ),
              margin: const EdgeInsets.only(right: 60),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Question section (repeated from user, but kept for context)
                  Padding(
                    padding: const EdgeInsets.all(12.0),
                    child: Text(
                      "Question: ${aiResponse.question}",
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  const Divider(),
                  // Process each answer item
                  ...aiResponse.answers.map((answer) => _buildAnswerItem(context, answer)),
                  
                  // Timestamp
                  Padding(
                    padding: const EdgeInsets.only(left: 12.0, bottom: 4.0, top: 4.0),
                    child: Text(
                      dateFormat.format(message.timestamp),
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.grey[600],
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
      padding: const EdgeInsets.symmetric(vertical: 4.0),
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
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.grey[200],
                    borderRadius: BorderRadius.circular(18),
                  ),
                  child: Text(text),
                ),
                Padding(
                  padding: const EdgeInsets.only(left: 8.0, top: 2.0),
                  child: Text(
                    dateFormat.format(message.timestamp),
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey[600],
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
          // Explanation
          Text(answer.explanation),
          const SizedBox(height: 12),
          
          // Code block (if applicable)
          if (answer.code != null && answer.language != null)
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 8),
                Container(
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.grey[300]!),
                  ),
                  width: double.infinity,
                  child: Container(
                    decoration: BoxDecoration(
                      color: Color(0xFF1E1E1E),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.grey[700]!),
                    ),
                    width: double.infinity,
                    alignment: Alignment.topLeft, // Force left alignment
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
                const SizedBox(height: 8),
              ],
            ),
          
          // References
          if (answer.references.isNotEmpty)
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 8),
                const Text(
                  "References:",
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                ...answer.references.map((ref) => Padding(
                  padding: const EdgeInsets.only(bottom: 2.0),
                  child: Text(
                    "• $ref",
                    style: const TextStyle(
                      fontSize: 12,
                      color: Colors.blue,
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
