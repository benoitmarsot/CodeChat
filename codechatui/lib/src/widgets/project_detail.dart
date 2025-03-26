import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/models/project.dart';
import 'package:codechatui/src/project_page.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/services/codechat_service.dart';
import 'package:codechatui/src/services/project_service.dart';
import 'package:codechatui/src/widgets/choice-button.dart';
import 'package:codechatui/src/widgets/github_widget.dart';
import 'package:codechatui/src/widgets/web_widget.dart';
import 'package:codechatui/src/widgets/zip_widget.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';

enum DataSourceType { github, zip, web }

class ProjectDetail extends StatefulWidget {
  final int projectId; // Prop for initial value
  final bool? isEditing;
  //final ValueChanged<Project?>? onProjectChanged; // Callback for changes

  const ProjectDetail({super.key, this.projectId = -1, this.isEditing});

  @override
  _ProjectDetailState createState() => _ProjectDetailState();
}

class _ProjectDetailState extends State<ProjectDetail> {
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

  String _createdMessage = '';
  String? _errorMessage;

  DataSourceType _selectedDataSource = DataSourceType.github;
  bool? _isEditing;
  Project? _selectedProject; // Internal state variable

  @override
  void initState() {
    super.initState();
    _projectId = widget.projectId;
    _isEditing = widget.isEditing ?? false;
    _fetchProjectDetails();
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
    if (_prjRepoURLController.text.isEmpty) {
      setState(() {
        _errorMessage = 'Repo URL is required';
      });
      return;
    }

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

      if (_userNameController.text.isEmpty && _patController.text.isNotEmpty) {
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
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to upload directory: $e';
      });
      print('Failed to upload directory: $e');
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

      setState(() {
        //_isEditing = false;
        _createdMessage = 'Project updated successfully!';

        Navigator.pop(context);
        if (mounted) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text(_createdMessage)));
        }
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to update project: $e';
      });
      print('_errorMessage $_errorMessage');
    }
  }

  Future<void> _fetchProjectDetails() async {
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
      print('Error fetching project details: $e');
    }
  }

  Future<void> _refreshDataSource() async {
    // Implement the logic to refresh the data source
    // This might involve re-fetching data from the Git repo, Zip file, or Web URL
    // You'll need to adapt this code based on your specific data source implementation
    print('Refreshing data source...');
    setState(() {
      _createdMessage = 'Refreshing data source...';
    });
    await Future.delayed(Duration(seconds: 2)); // Simulate a long process
    setState(() {
      _createdMessage = 'Data source refreshed!';
    });
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
          decoration: const InputDecoration(
            labelText: 'Web URL:',
          ),
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
          title: Text(_isEditing == true
              ? 'Edit Project'
              : _selectedProject != null
                  ? 'Project: ${_selectedProject!.name}'
                  : 'Create a project'),
        ),
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Padding(
            padding: EdgeInsets.all(20.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
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
                      ),
                    ),
                    const SizedBox(height: 24.0),
                    // Data Source Type Selection

                    Container(
                      margin: const EdgeInsets.only(bottom: 16.0, top: 16.0),
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
                            isSelected:
                                _selectedDataSource == DataSourceType.github,
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
                    // Conditional Input Fields based on selected data source

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
      ),
    );
  }
}
