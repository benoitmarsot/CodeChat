import 'dart:async';
import 'package:codechatui/src/models/exceptions.dart';
import 'package:codechatui/src/models/project.dart';
import 'package:codechatui/src/models/project_resources.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/services/codechat_service.dart';
import 'package:codechatui/src/services/project_service.dart';
import 'package:codechatui/src/widgets/project/project_resources.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:codechatui/src/utils/error_handler.dart';
import 'package:flutter/scheduler.dart';

class ProjectForm extends StatefulWidget {
  final int projectId;
  final bool isEditing;

  final Future<void> Function(Project project)? onSave;
  final Future<void> Function()? onCancel;

  const ProjectForm(
      {super.key,
      this.projectId = -1,
      this.onSave,
      this.onCancel,
      required this.isEditing});

  @override
  _ProjectFormState createState() => _ProjectFormState();
}

class _ProjectFormState extends State<ProjectForm>
    with SingleTickerProviderStateMixin {
  final TextEditingController _prjNameController = TextEditingController();
  final TextEditingController _prjDescController = TextEditingController();
  ResourceType _selectedResourceType = ResourceType.web;
  bool _isLoading = false;

  int? _projectId;
  Future<void> Function(Project project)? _onSave;

  List<String> _debugMessages = [];

  String? _errorMessage;
  bool _isEditing = false;
  Project? _selectedProject;

  final _formKey = GlobalKey<FormState>();
  @override
  void initState() {
    super.initState();
    _isEditing = widget.isEditing;
    _projectId = widget.projectId;
    if (widget.onSave != null) _onSave = widget.onSave;
    _selectedProject = Project(
        name: _prjNameController.text,
        description: _prjDescController.text,
        projectId: _projectId ?? -1,
        authorId: -1,
        assistantId: -1); //todo do we need both _selectedProject and
    if (_isEditing) {
      _fetchProjectDetails();
    }
  }

  void _initProjectData() {
    if (_projectId != null) {
      _prjNameController.text = _selectedProject!.name;
      _prjDescController.text = _selectedProject!.description;
    } else {
      _prjNameController.clear();
      _prjDescController.clear();
    }
  }

  Future<void> _createdProject() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);
    if (_formKey.currentState!.validate()) {
      _buildLoadingDialog(); // Show loading dialog before starting the process
      try {
        final project = await codechatService.createEmtptyProject(
            _prjNameController.text, _prjDescController.text, null);

        _onSave!(project);
        setState(() {
          _projectId = project.projectId;
        });
      } on ForbiddenException catch (e) {
        // Handle 403 error
        ErrorHandler.handleForbiddenError(context, e.message);
      } catch (e) {
        // setState(() {
        //   _errorMessage = 'Failed to create empty project: $e';
        // });
        print('Failed to create project: $e');
        // if (mounted) {
        //   ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        //     content: Text(_errorMessage!),
        //     backgroundColor: Colors.red,
        //     duration: const Duration(seconds: 5),
        //   ));
        // }
      } finally {
        // Use SchedulerBinding to ensure the dialog is dismissed after the current frame
        // SchedulerBinding.instance.addPostFrameCallback((_) {
        //   Navigator.of(context).pop(); // Dismiss loading dialog
        // });
      }
    }
  }

  void _resetEditing() {
    setState(() {
      //isEditing = false;
      _prjNameController.clear();
      _prjDescController.clear();
    });
  }

  void _cancelEdit() {
    _resetEditing();

    widget.onSave!(_selectedProject!);
  }

  Future<void> _saveChanges() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);

    if (_formKey.currentState!.validate()) {
      _buildLoadingDialog(); // Show loading dialog before starting the process
      try {
        // Update the project with the new values
        final updatedProject = _selectedProject!.copyWith(
          name: _prjNameController.text,
          description: _prjDescController.text,
        );

        await projectService.updateProject(updatedProject);

        if (mounted) {
          if (_onSave != null) {
            _onSave!(updatedProject);
          }
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(
              content: Text('Project updated successfully!'),
              backgroundColor: Colors.greenAccent));
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(
              content: Text('Failed to update project: $e'),
              duration: const Duration(seconds: 5),
              backgroundColor: Colors.redAccent));
        }
      } finally {
        // Use SchedulerBinding to ensure the dialog is dismissed after the current frame
        SchedulerBinding.instance.addPostFrameCallback((_) {
          Navigator.of(context).pop(); // Dismiss loading dialog
          //Navigator.of(context).pop(); // Dismiss edit dialog
        });
      }
    }
  }

  Widget _buildDialogForm() {
    return Form(
        key: _formKey,
        child: AlertDialog(
          title: Text(_isEditing ? 'Edit Project' : 'Create Project'),
          content: SizedBox(
            width: 500,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(height: 16),
                TextFormField(
                  controller: _prjNameController,
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return 'Please enter a Project Name';
                    }
                    return null;
                  },
                  decoration: InputDecoration(
                    labelText: 'Project Name',
                    filled: true,
                    fillColor:
                        Theme.of(context).colorScheme.surfaceContainerLowest,
                  ),
                ),
                SizedBox(height: 16),
                TextFormField(
                  controller: _prjDescController,
                  keyboardType: TextInputType.multiline,
                  maxLines: 2,
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return 'Please enter a Description';
                    }
                    return null;
                  },
                  decoration: InputDecoration(
                    filled: true,
                    fillColor:
                        Theme.of(context).colorScheme.surfaceContainerLowest,
                    labelText: 'Description',
                  ),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              style: TextButton.styleFrom(
                foregroundColor: Theme.of(context).colorScheme.secondary,
              ),
              onPressed: () =>
                  widget.onCancel != null ? widget.onCancel!() : _cancelEdit(),
              child: const Text('Cancel'),
            ),
            TextButton(
              style: TextButton.styleFrom(
                foregroundColor: Theme.of(context).colorScheme.primary,
              ),
              onPressed: () async {
                _isEditing ? _saveChanges() : _createdProject();
              },
              child: _isEditing ? const Text('Save') : const Text('Create'),
            ),
          ],
        ));
  }

  Future<void> _fetchProjectDetails() async {
    if (_projectId != null && _projectId! <= 0) {
      setState(() {
        _errorMessage = 'Invalid project ID';
      });
      return;
    }
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);

    try {
      setState(() {
        _errorMessage = null; // Clear previous error
      });
      final updatedProject = await projectService.getProject(widget.projectId);

      setState(() {
        _selectedProject = updatedProject;
      });
      _initProjectData();
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to fetch project details: $e';
      });
    } finally {
      if (mounted && _errorMessage != null && _errorMessage!.isNotEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text(_errorMessage ?? ''),
          duration: const Duration(seconds: 5),
          backgroundColor: Colors.red,
        ));
      }
    }
  }

  void _buildLoadingDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return Dialog(
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                CircularProgressIndicator(),
                SizedBox(width: 16),
                Text("Creating your project..."),
              ],
            ),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return _buildDialogForm();
  }
}
