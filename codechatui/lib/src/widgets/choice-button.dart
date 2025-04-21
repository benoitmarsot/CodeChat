import 'package:flutter/material.dart';

class ChoiceButton extends StatelessWidget {
  final String text;
  final IconData icon;
  final bool isSelected;
  final VoidCallback onPressed;
  final bool large;
  const ChoiceButton({
    Key? key,
    required this.text,
    required this.icon,
    required this.isSelected,
    required this.onPressed,
    this.large = false,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onPressed,
      icon: Icon(
        icon,
        size: large ? 28 : 16, // Increased icon size
        color: isSelected ? Colors.blue : Colors.grey,
      ),
      label: Text(
        text,
        style: TextStyle(
          fontSize: large ? 18 : 14,
          fontWeight: large ? FontWeight.bold : FontWeight.normal,
          color: isSelected ? Colors.blue : Colors.grey,
        ),
      ),
      style: OutlinedButton.styleFrom(
        side: BorderSide(
          color: isSelected ? Colors.blue : Colors.grey,
          width: 2.0, // Increased border width
        ),
        shape: RoundedRectangleBorder(
          borderRadius: large
              ? BorderRadius.circular(12.0)
              : BorderRadius.circular(8.0), // Larger border radius
        ),
        padding: large
            ? const EdgeInsets.symmetric(
                vertical: 16.0, // Increased vertical padding
                horizontal: 24.0, // Increased horizontal padding
              )
            : const EdgeInsets.symmetric(vertical: 12.0, horizontal: 16.0),
        backgroundColor: isSelected
            ? Colors.blue
                .withOpacity(0.1) // Subtle background for selected state
            : Colors.transparent,
      ),
    );
  }
}
