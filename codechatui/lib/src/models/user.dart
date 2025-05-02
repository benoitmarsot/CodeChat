class User {
  final String userid;
  final String name;
  final String email;
  final String role;
  final bool accountNonExpired;
  final DateTime createdAt;

  User({
    required this.userid,
    required this.name,
    required this.email,
    required this.role,
    required this.accountNonExpired,
    required this.createdAt,  
  });

  // Factory constructor to create a User instance from a JSON map
  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      userid: json['userid'],
      name: json['name'],
      email: json['email'],
      role: json['role'],
      accountNonExpired: json['accountNonExpired']=="true" ? true:false,
      createdAt: DateTime.parse(json['createdAt'] ?? DateTime.now().toString()),
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
