import 'package:flutter/material.dart';

class GithubForm extends StatefulWidget {
  final TextEditingController prjRepoURLController;
  final TextEditingController branchNameController;
  final TextEditingController userNameController;
  final TextEditingController userPswController;
  final TextEditingController patController;

  const GithubForm({
    Key? key,
    required this.prjRepoURLController,
    required this.branchNameController,
    required this.userNameController,
    required this.userPswController,
    required this.patController,
  }) : super(key: key);

  @override
  _GithubFormState createState() => _GithubFormState();
}

class _GithubFormState extends State<GithubForm> {
  bool _showCredentials = false;
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              flex: 3,
              child: TextField(
                controller: widget.prjRepoURLController,
                decoration: const InputDecoration(
                  labelText: 'Git Repo URL (HTTPS):',
                ),
              ),
            ),
            SizedBox(width: 12.0),
            Expanded(
              flex: 1,
              child: TextField(
                controller: widget.branchNameController,
                decoration: const InputDecoration(
                  labelText: 'Branch name:',
                ),
              ),
            ),
          ],
        ),
        Row(
          children: [
            Padding(
              padding: const EdgeInsets.only(top: 32.0),
              child: Row(
                children: [
                  Text('Private Repo'),
                  Transform.scale(
                    scale: 0.8, // Adjust this value to control the size
                    child: Switch(
                      value: _showCredentials,
                      onChanged: (value) {
                        setState(() {
                          _showCredentials = value;
                        });
                      },
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        if (_showCredentials)
          Container(
            margin: EdgeInsets.all(20),
            child: Column(children: [
              Row(
                children: [
                  Expanded(
                    flex: 2,
                    child: TextField(
                      controller: widget.userNameController,
                      decoration: const InputDecoration(
                        labelText: 'Git user name (for private repo):',
                      ),
                    ),
                  ),
                  SizedBox(width: 12.0),
                  Expanded(
                    flex: 2,
                    child: TextField(
                      controller: widget.userPswController,
                      decoration: const InputDecoration(
                        labelText: 'Password:',
                      ),
                    ),
                  ),
                ],
              ),
              Row(
                children: [
                  Expanded(
                    flex: 4,
                    child: TextField(
                      controller: widget.patController,
                      decoration: const InputDecoration(
                        labelText: 'Or PAT (for private repo):',
                      ),
                    ),
                  ),
                ],
              ),
            ]),
          ),
      ],
    );
  }
}
