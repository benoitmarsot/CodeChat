/// Strips out content from `<function:` until the closing `>` tag in a given string.
String extractFunctionContent(String input) {
  final regex = RegExp(r'<Function:(.*?)>');
  final match = regex.firstMatch(input);
  return match != null ? match.group(1)! : '';
}
