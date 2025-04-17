class User {
  final String userid;
  final String name;
  final String email;
  final String role;

  User({
    required this.userid,
    required this.name,
    required this.email,
    required this.role,
  });

  // Factory constructor to create a User instance from a JSON map
  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      userid: json['userid'],
      name: json['name'],
      email: json['email'],
      role: json['role'],
    );
  }

  // Converts this instance to a JSON map
  Map<String, dynamic> toJson() => {
        'userid': userid,
        'name': name,
        'email': email,
        'role': role,
      };
}
