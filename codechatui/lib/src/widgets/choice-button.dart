import 'package:flutter/material.dart';

class ChoiceButton extends StatelessWidget {
  final String text;
  final IconData icon;
  final bool isSelected;
  final VoidCallback onPressed;

  const ChoiceButton({
    Key? key,
    required this.text,
    required this.icon,
    required this.isSelected,
    required this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon, color: isSelected ? Colors.blue : Colors.grey),
      label: RichText(
        text: TextSpan(
          text: text,
          style: TextStyle(color: isSelected ? Colors.blue : Colors.grey),
          // Add additional TextSpan children here for more rich text customization if needed.
        ),
      ),
      style: OutlinedButton.styleFrom(
        side: BorderSide(
          color: isSelected ? Colors.blue : Colors.grey,
        ),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8.0),
        ),
        padding: const EdgeInsets.symmetric(vertical: 12.0, horizontal: 16.0),
      ),
    );
  }
}
