import 'package:codechatui/src/models/openai/assistant.dart';
import 'package:codechatui/src/models/user.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/services/openai/assistant_service.dart';
import 'package:codechatui/src/services/user_service.dart';
import 'package:codechatui/src/utils/util.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

enum Reasoning { low, medium, high }

const List<String> languageModel = [
  'gpt_4o',
  'gpt_4o_mini',
  'gpt_3.5_turbo',
  'gpt_4',
  'gpt_4_turbo',
  'gpt_4o_realtime_preview',
  'o3_mini'
];

class AssistantForm extends StatefulWidget {
  final VoidCallback? onSave;
  final VoidCallback? onCancel;
  final int projectId; // Prop for initial value
  const AssistantForm({
    Key? key,
    required this.projectId,
    this.onSave,
    this.onCancel,
  }) : super(key: key);

  @override
  State<AssistantForm> createState() => _AssistantFormState();
}

class _AssistantFormState extends State<AssistantForm> {
  final TextEditingController _assistantNameController =
      TextEditingController();
  final TextEditingController _contextController = TextEditingController();
  final TextEditingController _temperatureController = TextEditingController();
  final TextEditingController _maxResultsController = TextEditingController();

  String _createdMessage = "Assistant changed successfully!";
  String? _errorMessage;
  String _selectedModel = 'gpt_4o';
  late Assistant _selectedAssistant = Assistant(
    name: '',
    primaryFunction: '',
    model: 'gpt_4o',
    temperature: 1.0,
    reasoningEffort: 'medium',
  );
  late Set<Reasoning> _selectedReasoning = {Reasoning.medium};
  late int _projectId;
  late User? _currentUser = null;
  late double _temperature = 1.0;

  @override
  void initState() {
    super.initState();
    _fetchUser();
    _fetchAssistantDetails();
    _projectId = widget.projectId;
  }

  @override
  void dispose() {
    _assistantNameController.dispose();
    _contextController.dispose();
    _temperatureController.dispose();
    _maxResultsController.dispose();
    super.dispose();
  }

  void _loadAssistantData() {
    _assistantNameController.text = _selectedAssistant!.name;
    _contextController.text =
        extractFunctionContent(_selectedAssistant.instruction ?? '');

    setState(() {
      _selectedModel = _selectedAssistant.model ?? 'gpt-4o';
      _temperature = _selectedAssistant.temperature ?? 1.0;
    });

    if (_selectedAssistant.reasoningEffort != null) {
      final reasoning = Reasoning.values.firstWhere(
        (e) =>
            e.toString().split('.').last == _selectedAssistant.reasoningEffort,
        orElse: () => Reasoning.medium,
      );
      setState(() => _selectedReasoning = {reasoning});
    }
  }

  Future<void> _fetchUser() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final userService = UserService(authProvider: authProvider);

    try {
      final user = await userService.getCurrentUser();
      if (mounted) {
        setState(() {
          _currentUser = user;
        });
      }
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to fetch user details: $e';
      });
    }
  }

  Future<void> _fetchAssistantDetails() async {
    if (widget.projectId <= 0) {
      setState(() {
        _errorMessage = 'Invalid project ID';
      });
      return;
    }
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final assistantService = AssistantService(authProvider: authProvider);

    try {
      final currentAssistant =
          await assistantService.getAssistant(widget.projectId);
      if (mounted) {
        currentAssistant.validatedAssistant();

        setState(() {
          _selectedAssistant = currentAssistant;
        });
      }

      _loadAssistantData();
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to fetch project details: $e';
        });
      }
    } finally {
      if (mounted && _errorMessage != null && _errorMessage!.isNotEmpty) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(_errorMessage ?? '')));
      }
    }
  }

  void _saveChanges() async {
    print('_saveChanges ${_assistantNameController.text}');

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final assistantService = AssistantService(authProvider: authProvider);
    setState(() => _createdMessage = '');
    try {
      // Update the assistant with the new values
      Assistant updatedAssistant = Assistant(
        name: _assistantNameController.text,
        primaryFunction: _contextController.text,
        model: _selectedModel,
        temperature: _temperature,
        reasoningEffort: _selectedReasoning.first.toString().split('.').last,
      );
      updatedAssistant = updatedAssistant.validatedAssistant();

      // Call the service to update the assistant
      final response = await assistantService.updateAssistant(
        _projectId,
        updatedAssistant.toJson(),
      );

      // Use the returned Assistant object if needed
      setState(() {
        _selectedAssistant =
            response; // Update the local state with the new assistant
        _createdMessage = 'Assistant updated successfully.';
      });
      print('response $response');
    } catch (e) {
      setState(() => _errorMessage = 'Failed to update assistant: $e');
      print('_errorMessage $_errorMessage');
    }

    bool hasErrors = _errorMessage != null && _errorMessage!.isNotEmpty;

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(hasErrors ? _errorMessage! : _createdMessage),
        backgroundColor: hasErrors ? Colors.redAccent : Colors.greenAccent,
      ));
    }
    if (widget.onSave != null) {
      widget.onSave!();
    }
  }

  void _cancelEdit() {
    if (widget.onCancel != null) {
      widget.onCancel!();
    }
    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final inputColor = Theme.of(context).colorScheme.surfaceContainerLowest;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 8.0),
        TextField(
          controller: _assistantNameController,
          decoration: InputDecoration(
            fillColor: inputColor,
            filled: true,
            labelText: 'Assistant Name:',
          ),
        ),
        const SizedBox(height: 16.0),
        Row(children: [
          Column(children: [
            Row(children: [
              Text(
                'Temperature:',
                style: TextStyle(
                  fontSize: 16,
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(width: 8.0),
              SizedBox(
                width: 200,
                child: isAttributeSupported(_selectedModel, 'temperature')
                    ? Slider(
                        value: _temperature,
                        min: 0.0,
                        max: 2.0,
                        divisions:
                            20, // Optional: Divides the slider into steps
                        label: _temperature
                            .toStringAsFixed(1), // Display the value
                        onChanged: (double value) {
                          setState(() {
                            _temperature = value;
                            _temperatureController.text =
                                value.toStringAsFixed(1);
                          });
                        },
                      )
                    : Slider(
                        value: 0.0, // Default value when disabled
                        min: 0.0,
                        max: 2.0,
                        divisions: 20,
                        label: 'Disabled for the current model',
                        onChanged: null, // Disable the slider
                      ),
              ),
            ]),
          ]),
          // Column(children: [
          //   Row(
          //     children: [
          //       const SizedBox(width: 16.0),
          //       SizedBox(
          //         width: 200,
          //         child: TextField(
          //           controller: _maxResultsController,
          //           keyboardType: TextInputType.number,
          //           decoration: const InputDecoration(
          //             labelText: 'Max Results',
          //           ),
          //         ),
          //       ),
          //     ],
          //   ),
          // ]),
          Column(children: [
            Row(children: [
              const SizedBox(height: 16.0),
              Column(children: [
                Row(
                  children: [
                    Text(
                      'Reasoning Effort:',
                      style: TextStyle(
                        fontSize: 16,
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(width: 8.0),
                    isAttributeSupported(_selectedModel, 'reasoningEffort')
                        ? SegmentedButton<Reasoning>(
                            segments: const <ButtonSegment<Reasoning>>[
                              ButtonSegment<Reasoning>(
                                  value: Reasoning.low, label: Text('Low')),
                              ButtonSegment<Reasoning>(
                                  value: Reasoning.medium,
                                  label: Text('Medium')),
                              ButtonSegment<Reasoning>(
                                  value: Reasoning.high, label: Text('High')),
                            ],
                            selected: _selectedReasoning,
                            onSelectionChanged: (Set<Reasoning> newSelection) {
                              setState(() {
                                _selectedReasoning = newSelection;
                              });
                            },
                          )
                        : SegmentedButton<Reasoning>(
                            segments: const <ButtonSegment<Reasoning>>[
                                ButtonSegment<Reasoning>(
                                    value: Reasoning.low, label: Text('Low')),
                                ButtonSegment<Reasoning>(
                                    value: Reasoning.medium,
                                    label: Text('Medium')),
                                ButtonSegment<Reasoning>(
                                    value: Reasoning.high, label: Text('High')),
                              ],
                            selected: _selectedReasoning,
                            onSelectionChanged: null),
                  ],
                ),
              ]),
            ]),
          ]),
        ]),
        const SizedBox(height: 25.0),
        Row(
          children: [
            Text(
              'LLM Model:',
              style: TextStyle(
                fontSize: 16,
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(width: 8.0),
            DropdownButton<String>(
              value: _selectedModel,
              onChanged: (String? newValue) {
                setState(() {
                  _selectedModel = newValue!;
                });
              },
              items: languageModel
                  .toSet() // Ensure unique values
                  .map<DropdownMenuItem<String>>((String value) {
                return DropdownMenuItem<String>(
                  value: value,
                  child: Text(value),
                );
              }).toList(),
            ),
          ],
        ),
        Container(
          padding: const EdgeInsets.all(8.0),
          margin: const EdgeInsets.only(bottom: 16.0, top: 24.0),
          width: double.infinity,
          decoration: BoxDecoration(
            color:
                Theme.of(context).colorScheme.primaryContainer.withOpacity(0.1),
            border: Border.all(color: Colors.orangeAccent),
            borderRadius: BorderRadius.circular(4.0),
          ),
          child: Row(
            children: [
              const Icon(Icons.warning, color: Colors.orangeAccent),
              const SizedBox(width: 8.0),
              Expanded(
                child: Text(
                  'Warning: Changing the LLM model may lead to unexpected behavior, performance issues, or incompatibility with existing project configurations. Proceed with caution.',
                  style: TextStyle(color: Colors.orangeAccent),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 24.0),
        (_currentUser != null && _currentUser!.role == 'ADMIN')
            ? TextField(
                controller: _contextController,
                maxLines: 5,
                maxLength: 2048,
                decoration: InputDecoration(
                  filled: true,
                  fillColor: inputColor,
                  labelText: 'AI Context:',
                  //border: OutlineInputBorder(),
                ),
              )
            : const SizedBox.shrink(),
        const SizedBox(height: 32.0),
        Align(
          alignment: Alignment.bottomRight,
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              ElevatedButton(
                onPressed: _cancelEdit,
                style: ElevatedButton.styleFrom(
                  textStyle: const TextStyle(fontSize: 20),
                ),
                child: const Text('Cancel'),
              ),
              const SizedBox(width: 8),
              ElevatedButton(
                onPressed: _saveChanges,
                style: ElevatedButton.styleFrom(
                  textStyle: const TextStyle(fontSize: 20),
                ),
                child: const Text('Save'),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
