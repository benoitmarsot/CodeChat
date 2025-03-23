import 'package:flutter/material.dart';

class WebForm extends StatefulWidget {
  final TextEditingController prjRepoURLController;
  final TextEditingController userNameController;
  final TextEditingController userPswController;

  const WebForm({
    Key? key,
    required this.prjRepoURLController,
    required this.userNameController,
    required this.userPswController,
  }) : super(key: key);

  @override
  _WebFormState createState() => _WebFormState();
}

class _WebFormState extends State<WebForm> {
  bool _showAuth = false;
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              flex: 4,
              child: TextField(
                controller: widget.prjRepoURLController,
                decoration: const InputDecoration(
                  labelText: 'Web URL',
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
                  Text('Authentication'),
                  Switch(
                    value: _showAuth,
                    onChanged: (value) {
                      setState(() {
                        _showAuth = value;
                      });
                    },
                  ),
                ],
              ),
            ),
          ],
        ),
        if (_showAuth)
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
                        labelText: 'Username:',
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
            ]),
          ),
      ],
    );
  }
}
