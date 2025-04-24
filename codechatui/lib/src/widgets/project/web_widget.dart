import 'package:flutter/material.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/services/codechat_service.dart';
import 'package:provider/provider.dart';

class WebForm extends StatefulWidget {
  final List<String> allowedDomains;
  final TextEditingController webURLController;
  final TextEditingController userNameController;
  final TextEditingController userPswController;
  final TextEditingController? maxPagesController;
  final TextEditingController? maxDepthController;
  final TextEditingController? requestsPerMinuteController;
  final int projectId;
  final bool isDisabled;
  final ValueChanged<List<String>>? onDomainsChanged;

  const WebForm({
    Key? key,
    required this.webURLController,
    required this.userNameController,
    required this.userPswController,
    required this.allowedDomains,
    this.maxPagesController,
    this.maxDepthController,
    this.requestsPerMinuteController,
    this.projectId = -1,
    this.isDisabled = false,
    this.onDomainsChanged,
  }) : super(key: key);

  @override
  _WebFormState createState() => _WebFormState();
}

class _WebFormState extends State<WebForm> {
  bool _showAuth = false;
  List<String> _allowedDomains = [];
  final int _defaultMaxPages = 100;
  final int _defaultMaxDepth = 2;
  final int _defaultRequestsPerMinute = 200;

  @override
  void initState() {
    super.initState();
    //le copy of widget.allowedDomains
    _allowedDomains = List<String>.from(widget.allowedDomains);
    //_allowedDomains = widget.allowedDomains;
    if (_allowedDomains.isEmpty) {
      _allowedDomains.add('');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Expanded(
            flex: 2,
            child: TextFormField(
              controller: widget.webURLController,
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return 'Please enter a Web URL';
                }
                return null;
              },
              decoration: InputDecoration(
                labelText: 'Web URL:',
                filled: true,
                fillColor: Theme.of(context).colorScheme.surfaceContainerLowest,
              ),
            ),
          ),
          SizedBox(width: 24.0)
        ]),
        const SizedBox(height: 16),
        if (_showAuth)
          Container(
            margin: EdgeInsets.all(20),
            child: Column(
              children: [
                Row(
                  children: [
                    Expanded(
                      flex: 2,
                      child: TextFormField(
                        controller: widget.userNameController,
                        enabled: !widget
                            .isDisabled, // Disable field if isDisabled is true
                        decoration: InputDecoration(
                          labelText: 'Username:',
                          filled: true,
                          fillColor: Theme.of(context)
                              .colorScheme
                              .surfaceContainerLowest,
                        ),
                      ),
                    ),
                    SizedBox(width: 12.0),
                    Expanded(
                      flex: 2,
                      child: TextFormField(
                        controller: widget.userPswController,
                        enabled: !widget
                            .isDisabled, // Disable field if isDisabled is true
                        obscureText: true,
                        decoration: InputDecoration(
                          labelText: 'Password:',
                          filled: true,
                          fillColor: Theme.of(context)
                              .colorScheme
                              .surfaceContainerLowest,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        SizedBox(height: 48.0),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Flexible(
              flex: 1,
              child: TextFormField(
                controller: widget.maxPagesController ??
                    TextEditingController(text: _defaultMaxPages.toString()),
                enabled:
                    !widget.isDisabled, // Disable field if isDisabled is true
                keyboardType: TextInputType.number,
                decoration: InputDecoration(
                  labelText: 'Max Pages:',
                  filled: true,
                  fillColor:
                      Theme.of(context).colorScheme.surfaceContainerLowest,
                ),
              ),
            ),
            SizedBox(width: 24.0),
            Flexible(
              flex: 1,
              child: TextFormField(
                controller: widget.maxDepthController ??
                    TextEditingController(text: _defaultMaxDepth.toString()),
                enabled:
                    !widget.isDisabled, // Disable field if isDisabled is true
                keyboardType: TextInputType.number,
                decoration: InputDecoration(
                  labelText: 'Max Depth:',
                  filled: true,
                  fillColor:
                      Theme.of(context).colorScheme.surfaceContainerLowest,
                ),
              ),
            ),
            SizedBox(width: 24.0),
            Flexible(
              flex: 1,
              child: TextFormField(
                controller: widget.requestsPerMinuteController ??
                    TextEditingController(
                        text: _defaultRequestsPerMinute.toString()),
                enabled:
                    !widget.isDisabled, // Disable field if isDisabled is true
                keyboardType: TextInputType.number,
                decoration: InputDecoration(
                  labelText: 'Requests Per Minute:',
                  filled: true,
                  fillColor:
                      Theme.of(context).colorScheme.surfaceContainerLowest,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 16.0),
        //_buildWebURLsInput()
        _buildDomainInput()
      ],
    );
  }

  Widget _buildDomainInput() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Allowed Domains:',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: Theme.of(context).colorScheme.onSurface,
          ),
        ),
        const SizedBox(height: 8),
        ListView.builder(
          shrinkWrap: true,
          physics:
              NeverScrollableScrollPhysics(), // Prevent scrolling inside the form
          itemCount: _allowedDomains.length,
          itemBuilder: (context, index) {
            bool isHovered = false; // Track hover state
            return StatefulBuilder(
              builder: (context, setState) {
                return Row(
                  children: [
                    Expanded(
                      child: TextFormField(
                        initialValue: _allowedDomains[index],
                        enabled: !widget
                            .isDisabled, // Disable field if isDisabled is true
                        onChanged: (value) {
                          setState(() {
                            _allowedDomains[index] = value;
                          });
                        },
                        decoration: InputDecoration(
                          labelText: 'Domain ${index + 1}',
                          filled: true,
                          fillColor: Theme.of(context)
                              .colorScheme
                              .surfaceContainerLowest,
                        ),
                      ),
                    ),
                    MouseRegion(
                      onEnter: (_) {
                        setState(() {
                          isHovered = true;
                        });
                      },
                      onExit: (_) {
                        setState(() {
                          isHovered = false;
                        });
                      },
                      child: IconButton(
                        icon: Icon(
                          Icons.delete,
                          color: isHovered
                              ? Colors.red
                              : Colors.grey, // Change color on hover
                        ),
                        onPressed: widget.isDisabled
                            ? null
                            : () {
                                setState(() {
                                  if (index < _allowedDomains.length) {
                                    _allowedDomains.removeAt(index);
                                    widget.onDomainsChanged
                                        ?.call(_allowedDomains);
                                  }
                                });
                              },
                      ),
                    ),
                  ],
                );
              },
            );
          },
        ),
        const SizedBox(height: 8),
        ElevatedButton.icon(
          onPressed: widget.isDisabled
              ? null
              : () {
                  setState(() {
                    _allowedDomains.add('');
                    widget.onDomainsChanged?.call(_allowedDomains);
                  });
                },
          icon: Icon(Icons.add),
          label: Text('Add Domain'),
        ),
      ],
    );
  }
}
