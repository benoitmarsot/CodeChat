import 'dart:async';

import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/models/exceptions.dart';
import 'package:codechatui/src/models/project.dart';
import 'package:codechatui/src/models/project_resources.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/services/codechat_service.dart';
import 'package:codechatui/src/services/project_service.dart';
import 'package:codechatui/src/widgets/choice-button.dart';
import 'package:codechatui/src/widgets/github_widget.dart';
import 'package:codechatui/src/widgets/zip_widget.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart';
import 'package:provider/provider.dart';
import 'package:codechatui/src/utils/error_handler.dart';

enum DataSourceType { github, zip, web }

class ProjectDetail extends StatefulWidget {
  final int projectId; // Prop for initial value
  final bool isEditing;
  final Future<void> Function()? onSave;
  //final ValueChanged<Project?>? onProjectChanged; // Callback for changes

  const ProjectDetail(
      {super.key, this.projectId = -1, this.onSave, required this.isEditing});

  @override
  _ProjectDetailState createState() => _ProjectDetailState();
}

class _ProjectDetailState extends State<ProjectDetail>
    with SingleTickerProviderStateMixin {
  final TextEditingController _prjNameController = TextEditingController();
  final TextEditingController _prjDesctController = TextEditingController();
  final TextEditingController _prjRepoURLController = TextEditingController();

  final TextEditingController _branchNameController =
      TextEditingController(text: 'main');
  final TextEditingController _userNameController = TextEditingController();
  final TextEditingController _userPswController = TextEditingController();
  final TextEditingController _patController = TextEditingController();
  final TextEditingController _zipFilePathController = TextEditingController();
  final TextEditingController _webURLController = TextEditingController();

  int? _projectId;
  Future<void> Function()? _onSave;
  //Object _model =

  String _createdMessage = '';
  String? _errorMessage;
  bool get hasDataSource =>
      _prjRepoURLController.text.isNotEmpty ||
      _zipFilePathController.text.isNotEmpty ||
      _webURLController.text.isNotEmpty;

  DataSourceType _selectedDataSource = DataSourceType.github;
  bool _isEditing = false;
  Project? _selectedProject;
  List<ProjectResource> _selectedProjectResources = [];
  late TabController _tabController;
  String _selectedModel = 'gpt-4o'; // Default selection for the dropdown

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _projectId = widget.projectId;
    if (widget.onSave != null) _onSave = widget.onSave;
    _isEditing = widget.isEditing;
    if (_isEditing) {
      _fetchProjectDetails();
      _fetchProjectResources();
    }
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  void _loadProjectData() {
    if (_projectId != null) {
      _prjNameController.text = _selectedProject!.name;
      _prjDesctController.text = _selectedProject!.description;
      // You might need to fetch other project details here based on your data model
    } else {
      _prjNameController.clear();
      _prjDesctController.clear();
    }
  }

  void _loadResourceData() {
    if (_projectId != null) {
      _prjRepoURLController.text = _selectedProjectResources[0].uri;
    } else {
      _prjRepoURLController.clear();
    }
  }

  Future<void> _createdProject() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);

    print('Token: ${authProvider.token}');
    setState(() {
      _errorMessage = null; // Clear previous error
    });
    if (_prjNameController.text.isEmpty) {
      setState(() {
        _errorMessage = 'Project name is required';
      });
      return;
    }
    // if (_prjRepoURLController.text.isEmpty) {
    //   setState(() {
    //     _errorMessage = 'Repo URL is required';
    //   });
    //   return;
    // }
    if (hasDataSource == false) {
      try {
        final project = await codechatService.createEmtptyProject(
            _prjNameController.text, _prjDesctController.text);

        setState(() {
          _createdMessage = 'Emtpy Project created';
        });
        _prjNameController.clear();
        _prjDesctController.clear();

        _selectedProject = project;
        _onSave!();
        Navigator.of(context).pop();
      } on ForbiddenException catch (e) {
        // Handle 403 error
        ErrorHandler.handleForbiddenError(context, e.message);
      } catch (e) {
        setState(() {
          _errorMessage = 'Failed to create empty project: $e';
        });
        print('Failed to create project: $e');
      }
    } else {
      final uri = '${AppConfig.openaiBaseUrl}/files/uploadDir?projectId=1';
      print('Uploading to URI: $uri');

      // Show loading dialog
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
                  Text("Creating project...\nIt will take a while..."),
                ],
              ),
            ),
          );
        },
      );
      try {
        // Handle authentication parameters
        String? username;
        String? password;

        if (_userNameController.text.isEmpty &&
            _patController.text.isNotEmpty) {
          // If username is empty but PAT is provided, use PAT as username
          username = _patController.text;
        } else if (_userNameController.text.isNotEmpty) {
          // If username is provided, use username and password
          username = _userNameController.text;
          password = _userPswController.text;
        }

        final project = await codechatService.createProject(
          _prjNameController.text,
          _prjDesctController.text,
          _prjRepoURLController.text,
          _branchNameController.text,
          username,
          password,
        );

        setState(() {
          _createdMessage = 'Project created';
        });

        // Clear the form
        _prjNameController.clear();
        _prjDesctController.clear();
        _prjRepoURLController.clear();

        // Dismiss loading dialog
        if (mounted) {
          Navigator.of(context).pop();
        }
        _selectedProject = project;
        _onSave!();
        // ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        //     content: Text(_createdMessage!), backgroundColor: Colors.green));
        Navigator.of(context).pop();
      } catch (e) {
        setState(() {
          _errorMessage = 'Failed to upload directory: $e';
        });
        print('Failed to upload directory: $e');
        // ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        //     content: Text(_errorMessage!), backgroundColor: Colors.red));
      }
    }
  }

  void _resetEditing() {
    setState(() {
      //isEditing = false;
      _prjNameController.clear();
      _prjDesctController.clear();
    });
  }

  void _cancelEdit() {
    _resetEditing();
    Navigator.pop(context);
    // GoRouter.of(context).go('/projects');
  }

  Future<void> _saveChanges() async {
    print('_saveChanges $_prjNameController.text');

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);

    try {
      // Update the project with the new values
      final updatedProject = _selectedProject!.copyWith(
        name: _prjNameController.text,
        description: _prjDesctController.text,
      );

      await projectService.updateProject(updatedProject);
      if (hasDataSource) {
        // If there's a data source, you might want to refresh or update it here
        //await projectService.refreshRepo(_selectedProject!.projectId);
      }
      setState(() {
        //_isEditing = false;
        _createdMessage = 'Project updated successfully!';

        Navigator.pop(context);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(
              content: Text(_createdMessage), backgroundColor: Colors.green));
        }
      });
      _onSave!();
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to update project: $e';
      });
      print('_errorMessage $_errorMessage');
    }
  }

  Future<void> _deleteProject() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);
    if (_projectId == null || _projectId! < 0) return;
    try {
      // Adjust the method below to match your project service code
      final updatedProject = await projectService.deleteProject(_projectId!);
      _onSave!(); // Call the onSave callback to refresh the project list
      Navigator.of(context).pop(true);
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to delete project: $e';
      });
    }
  }

  Future<void> _handleDelete() async {
    final confirmDelete = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Confirm Deletion'),
        content: Text(
            'Are you sure you want to delete the project "${_selectedProject!.name}"?'),
        actions: [
          TextButton(
            onPressed: _deleteProject,
            child: Text('Delete'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text('Cancel'),
          ),
        ],
      ),
    );

    if (confirmDelete == true) {
      final authProvider = Provider.of<AuthProvider>(context, listen: false);
      final projectService = ProjectService(authProvider: authProvider);
      try {
        await projectService.deleteProject(_selectedProject!.projectId);
        Navigator.of(context).pop(); // Close the detail page
      } catch (e) {
        setState(() {
          _errorMessage = 'Failed to delete project: $e';
        });
      }
    }
  }

  Future<void> _fetchProjectDetails() async {
    if (widget.projectId <= 0) {
      setState(() {
        _errorMessage = 'Invalid project ID';
      });
      return;
    }
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);

    try {
      final updatedProject = await projectService.getProject(widget.projectId);
      setState(() {
        _selectedProject = updatedProject;
        _loadProjectData();
        _createdMessage = 'Project details updated successfully!';
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to fetch project details: $e';
      });
    } finally {
      if (mounted && _errorMessage != null && _errorMessage!.isNotEmpty) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(_errorMessage ?? '')));
      }
    }
  }

  Future<void> _fetchProjectResources() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);

    if (widget.projectId <= 0) {
      setState(() {
        _errorMessage = 'Invalid project ID';
      });
      return;
    }

    try {
      final updatedResources =
          await projectService.getProjectResources(widget.projectId);
      if (updatedResources.isNotEmpty) {
        setState(() {
          _selectedProjectResources = updatedResources;
          _loadResourceData();
        });
      }
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to fetch project details: $e';
      });
      print('Error fetching project details: $e');
    }
    // finally {
    //   if (mounted && _errorMessage!.isNotEmpty) {
    //     ScaffoldMessenger.of(context)
    //         .showSnackBar(SnackBar(content: Text(_errorMessage ?? '')));
    //   }
    // }
  }

  Future<void> _refreshDataSource() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechattService = CodechatService(authProvider: authProvider);

    // Show loading dialog
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
                Text("Refreshing project repo...\nIt will take a while..."),
              ],
            ),
          ),
        );
      },
    );
    try {
      await codechattService.refreshRepo(_projectId!);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text('Refreshed project data source'),
            backgroundColor: Colors.green));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text('Failed to refresh data source: $e'),
            backgroundColor: Colors.red));
      }
    } finally {
      if (mounted) {
        Navigator.of(context).pop();
      }
    }
  }

  Widget _buildDataSourceForm() {
    switch (_selectedDataSource) {
      case DataSourceType.github:
        return GithubForm(
          prjRepoURLController: _prjRepoURLController,
          branchNameController: _branchNameController,
          userNameController: _userNameController,
          userPswController: _userPswController,
          patController: _patController,
        );
      case DataSourceType.zip:
        return ZipForm(
          onFileSelected: (path) {
            _zipFilePathController.text = path ?? '';
          },
        );
      case DataSourceType.web:
        return TextField(
          controller: _webURLController,
          decoration: const InputDecoration(labelText: 'Web URL:'),
        );
      default:
        return const SizedBox.shrink();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(20),
      color: Theme.of(context).colorScheme.surface,
      child: Scaffold(
        appBar: AppBar(
          bottom: TabBar(
            controller: _tabController,
            tabs: const [
              Tab(text: 'Project'),
              Tab(text: 'Admin'),
            ],
          ),
          actions: [
            if (_isEditing == true)
              Tooltip(
                message: hasDataSource
                    ? 'Refresh Data Source'
                    : 'You need to define a project URL to be able to refresh it.',
                child: ElevatedButton.icon(
                  label: const Text('Refresh'),
                  icon: Icon(Icons.refresh),
                  onPressed: hasDataSource ? _refreshDataSource : null,
                ),
              ),
            if (_isEditing == true && _selectedProject != null)
              Tooltip(
                message: 'Delete Project',
                child: ElevatedButton.icon(
                  label: const Text('Delete Project'),
                  icon: Icon(Icons.delete),
                  // color: Colors.red,
                  onPressed: _handleDelete,
                ),
              ),
          ],
          title: Text(_isEditing == true
              ? 'Edit Project'
              : _selectedProject != null
                  ? 'Project: ${_selectedProject!.name}'
                  : 'Create a project'),
        ),
        body: TabBarView(
          controller: _tabController,
          children: [
            SingleChildScrollView(
              padding: const EdgeInsets.all(24.0),
              child: Padding(
                padding: EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Project tab content
                    Column(
                      children: [
                        TextField(
                          controller: _prjNameController,
                          decoration: const InputDecoration(
                            labelText: 'Project name:',
                          ),
                        ),
                        const SizedBox(height: 16.0),
                        TextField(
                          controller: _prjDesctController,
                          decoration: const InputDecoration(
                            labelText: 'Description:',
//                            border: OutlineInputBorder(),
                          ),
                        ),
                        const SizedBox(height: 24.0),
                        // ...existing code for data source selection...
                        Container(
                          margin:
                              const EdgeInsets.only(bottom: 16.0, top: 16.0),
                          child: Row(children: [
                            Text('Define Data Source',
                                style: TextStyle(
                                    fontSize: 20, fontWeight: FontWeight.bold)),
                          ]),
                        ),
                        Container(
                          margin: const EdgeInsets.only(bottom: 16.0),
                          child: Row(
                            children: [
                              const SizedBox(width: 8),
                              ChoiceButton(
                                text: 'GitHub',
                                icon: Icons.code,
                                isSelected: _selectedDataSource ==
                                    DataSourceType.github,
                                onPressed: () {
                                  setState(() {
                                    _selectedDataSource = DataSourceType.github;
                                  });
                                },
                              ),
                              const SizedBox(width: 16),
                              ChoiceButton(
                                text: 'Zip File',
                                icon: Icons.archive,
                                isSelected:
                                    _selectedDataSource == DataSourceType.zip,
                                onPressed: () {
                                  setState(() {
                                    _selectedDataSource = DataSourceType.zip;
                                  });
                                },
                              ),
                              const SizedBox(width: 16),
                              ChoiceButton(
                                text: 'Web URL',
                                icon: Icons.web,
                                isSelected:
                                    _selectedDataSource == DataSourceType.web,
                                onPressed: () {
                                  setState(() {
                                    _selectedDataSource = DataSourceType.web;
                                  });
                                },
                              ),
                            ],
                          ),
                        ),
                        _buildDataSourceForm(),
                        Align(
                          alignment: Alignment.bottomRight,
                          child: (_isEditing == true
                              ? Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    ElevatedButton(
                                      onPressed: _cancelEdit,
                                      style: ElevatedButton.styleFrom(
                                        textStyle: TextStyle(fontSize: 20),
                                      ),
                                      child: Text('Cancel'),
                                    ),
                                    SizedBox(width: 8),
                                    ElevatedButton(
                                      onPressed: _saveChanges,
                                      style: ElevatedButton.styleFrom(
                                        textStyle: TextStyle(fontSize: 20),
                                      ),
                                      child: Text('Save'),
                                    ),
                                  ],
                                )
                              : Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    ElevatedButton(
                                      onPressed: _cancelEdit,
                                      style: ElevatedButton.styleFrom(
                                        textStyle: TextStyle(fontSize: 20),
                                      ),
                                      child: Text('Cancel'),
                                    ),
                                    SizedBox(width: 8),
                                    ElevatedButton(
                                      onPressed: _createdProject,
                                      style: ElevatedButton.styleFrom(
                                        textStyle: TextStyle(fontSize: 20),
                                      ),
                                      child: Text('Create'),
                                    ),
                                  ],
                                )),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            // Admins tab content
            SingleChildScrollView(
              padding: const EdgeInsets.all(24.0),
              child: Padding(
                padding: EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Admintrator Actions',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16.0),
                    Text(
                      'Select a model:',
                      style: TextStyle(fontSize: 16),
                    ),
                    const SizedBox(height: 8.0),
                    DropdownButton<String>(
                      value: _selectedModel,
                      onChanged: (String? newValue) {
                        setState(() {
                          _selectedModel = newValue!;
                        });
                      },
                      items: [
                        'gpt-4o',
                        'gpt-4o-mini',
                        'gpt-3.5-turbo',
                        'gpt-4',
                        'gpt-4-turbo',
                        'gpt-4o-realtime-preview',
                        'o3-mini',
                      ].map<DropdownMenuItem<String>>((String value) {
                        return DropdownMenuItem<String>(
                          value: value,
                          child: Text(value),
                        );
                      }).toList(),
                    ),
                    // Add more admin-specific widgets here if needed
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
