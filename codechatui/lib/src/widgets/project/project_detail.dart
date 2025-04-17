import 'dart:async';
// import 'package:sse_client/sse_client.dart';
//import 'package:eventsource/eventsource.dart';
//import 'package:flutter_client_sse/flutter_client_sse.dart';
//import 'package:flutter_client_sse/constants/sse_request_type_enum.dart'
import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/models/exceptions.dart';
import 'package:codechatui/src/models/project.dart';
import 'package:codechatui/src/models/project_resources.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/services/codechat_service.dart';
import 'package:codechatui/src/services/project_service.dart';
import 'package:codechatui/src/widgets/choice-button.dart';
import 'package:codechatui/src/widgets/project/assistant.dart';
import 'package:codechatui/src/widgets/project/github_widget.dart';
import 'package:codechatui/src/widgets/project/zip_widget.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:codechatui/src/utils/error_handler.dart';

enum DataSourceType { github, zip, web }

enum Temperature { extraSmall, small, medium, large, extraLarge }

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
  final TextEditingController _prjAssistantController = TextEditingController();
  final TextEditingController _prjContextController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  int? _projectId;
  Future<void> Function()? _onSave;
  List<String> _debugMessages = [];

  String _createdMessage = '';
  String? _errorMessage;
  bool get hasDataSource =>
      _prjRepoURLController.text.isNotEmpty ||
      _zipFilePathController.text.isNotEmpty ||
      _webURLController.text.isNotEmpty;

  DataSourceType _selectedDataSource = DataSourceType.github;
  bool _isEditing = false;
  bool _loadingFiles = false;
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
    _scrollController.dispose();
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

    if (_formKey.currentState!.validate()) {
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

      if (hasDataSource == false) {
        try {
          Navigator.of(context).pop(); //optimistic post
          final project = await codechatService.createEmtptyProject(
              _prjNameController.text, _prjDesctController.text);

          _prjNameController.clear();
          _prjDesctController.clear();

          _selectedProject = project;

          _onSave!();
        } on ForbiddenException catch (e) {
          // Handle 403 error
          ErrorHandler.handleForbiddenError(context, e.message);
        } catch (e) {
          setState(() {
            _errorMessage = 'Failed to create empty project: $e';
          });
          print('Failed to create project: $e');
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                content: Text(_errorMessage!), backgroundColor: Colors.red));
          }
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

    if (_formKey.currentState!.validate()) {
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
                content: Text(_createdMessage),
                backgroundColor: Colors.greenAccent));
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
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red, // Button color
              foregroundColor: Colors.white, // Text color
            ),
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
            backgroundColor: Colors.greenAccent));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text('Failed to refresh data source: $e'),
            backgroundColor: Colors.redAccent));
      }
    } finally {
      if (mounted) {
        Navigator.of(context).pop();
      }
    }
  }

  void _scrollToBottom() {
    Future.delayed(const Duration(milliseconds: 100), () {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  Widget _buildProjectDetail() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      // Project tab content
      Column(
        children: [
          TextFormField(
            controller: _prjNameController,
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter a Project Name';
              }
              return null;
            },
            decoration: InputDecoration(
                labelText: 'Project Name:',
                filled: true,
                fillColor: Theme.of(context).colorScheme.surfaceContainerLowest
                //border: OutlineInputBorder(),
                ),
          ),

          const SizedBox(height: 16.0),
          TextFormField(
            controller: _prjDesctController,
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter a Description';
              }
              return null;
            },
            decoration: InputDecoration(
                labelText: 'Description:',
                filled: true,
                fillColor: Theme.of(context).colorScheme.surfaceContainerLowest
                //border: OutlineInputBorder(),
                ),
          ),

          const SizedBox(height: 24.0),
          // ...existing code for data source selection...
          Container(
            margin: const EdgeInsets.only(bottom: 16.0, top: 24.0),
            child: Row(children: [
              Text('Define Data Source:',
                  style: TextStyle(
                      fontSize: 16,
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                      fontWeight: FontWeight.normal)),
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
                  isSelected: _selectedDataSource == DataSourceType.github,
                  onPressed: () {
                    setState(() {
                      _selectedDataSource = DataSourceType.github;
                    });
                  },
                ),
                const SizedBox(width: 16),
                ChoiceButton(
                  text: 'File(s)',
                  icon: Icons.archive,
                  isSelected: _selectedDataSource == DataSourceType.zip,
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
                  isSelected: _selectedDataSource == DataSourceType.web,
                  onPressed: () {
                    setState(() {
                      _selectedDataSource = DataSourceType.web;
                    });
                  },
                ),
              ],
            ),
          ),
          const SizedBox(height: 32),
          _buildDataSourceForm(),
          const SizedBox(height: 32),
          Align(
            alignment: Alignment.bottomRight,
            child: (_isEditing == true
                ? Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      // ElevatedButton(

                      //   onPressed: testSubscription,
                      //   style: ElevatedButton.styleFrom(
                      //     textStyle: TextStyle(fontSize: 20),
                      //   ),
                      //   child: Text(_loadingFiles ? 'Stop' : 'Start'),
                      // ),
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
          // Column(
          //   crossAxisAlignment: CrossAxisAlignment.start,
          //   children: _debugMessages.isNotEmpty
          //       ? [
          //           Container(
          //               height: 150,
          //               decoration: BoxDecoration(
          //                 border: Border.all(color: Colors.grey),
          //                 borderRadius: BorderRadius.circular(8.0),
          //                 color: Theme.of(context)
          //                     .colorScheme
          //                     .primary
          //                     .withOpacity(0.1),
          //               ),
          //               child: ListView.builder(
          //                 controller: _scrollController,
          //                 itemCount: _debugMessages.length,
          //                 itemBuilder: (context, index) {
          //                   return Padding(
          //                     padding: const EdgeInsets.all(4.0),
          //                     child: Text(_debugMessages[index]),
          //                   );
          //                 },
          //               )),
          //         ]
          //       : [],
          // ),
        ],
      ),
    ]);
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
        return (_projectId != null && _projectId! > 0)
            ? ZipForm(
                projectId: _projectId!,
                onFileSelected: addFilesToProject,
                isLoading: _loadingFiles,
                debugMessages: _debugMessages)
            : Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.cloud_upload,
                        size: 40, color: Colors.orangeAccent),
                    const SizedBox(height: 8),
                    Text(
                      'You need to create a project before you can add files to project',
                      style: TextStyle(color: Colors.orangeAccent),
                    ),
                  ],
                ),
              );

      case DataSourceType.web:
        return TextField(
          controller: _webURLController,
          decoration: InputDecoration(
              filled: true,
              fillColor: Theme.of(context).colorScheme.surfaceContainerLowest,
              labelText: 'Web URL:'),
        );
      default:
        return const SizedBox.shrink();
    }
  }

  final _formKey = GlobalKey<FormState>();
  Set<Temperature> selection = <Temperature>{
    Temperature.medium
  }; //todo: from settings
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(20),
      color: Theme.of(context).colorScheme.surface,
      child: Scaffold(
        appBar: AppBar(
          bottom: (_isEditing)
              ? TabBar(
                  controller: _tabController,
                  tabs: const [
                    Tab(text: 'Project'),
                    Tab(text: 'Assistant'),
                  ],
                )
              : null,
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
                  style: ElevatedButton.styleFrom(
                    foregroundColor: Colors.red,
                  ),
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
                child: Form(
                  key: _formKey,
                  child: _buildProjectDetail(),
                ),
              ),
            ),
            // Admins tab content
            SingleChildScrollView(
              padding: const EdgeInsets.all(24.0),
              child: Padding(
                padding: EdgeInsets.all(20.0),
                child:
                    AssistantForm(projectId: _projectId!), //_buildAssistanForm
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> addFilesToProject(String content, String fileName) async {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return AlertDialog(
          content: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Text("Adding $fileName to Project?"),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop(false);
                _handleFileAPI(content, fileName);
              },
              child: Text('Add',
                  style:
                      TextStyle(color: Theme.of(context).colorScheme.primary)),
            ),
          ],
        );
      },
    );
  }

  Future<void> _handleFileAPI(String content, String fileName) async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);

    setState(() {
      _loadingFiles = true;
      _errorMessage = null; // Clear previous error
    });
    try {
      subscribeToMessages();
      await codechatService.addProjectZip(_projectId!, content, fileName);

      // Dismiss loading dialog
      // if (mounted) {
      //   Navigator.of(context).pop();
      // }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text('File(s) uploaded successfully!'),
            backgroundColor: Colors.greenAccent),
      );
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to fetch project details: $e';
        });
      }
    } finally {
      if (mounted) {
        setState(() {
          _loadingFiles = false;
        });
        if (_errorMessage != null && _errorMessage!.isNotEmpty) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text(_errorMessage ?? '')));
        }
      }
    }
  }

  void subscribeToMessages() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);

    try {
      if (_loadingFiles) {
        await codechatService.subscribeToMessages('debug', (event, data) {
          setState(() {
            _debugMessages = [..._debugMessages!, data];
            //_debugMessages.add(data);
          });
          _scrollToBottom();
        }, (error) {
          if (mounted) {
            setState(() {
              _errorMessage = 'Error: $error';
            });
          }
        }, () {
          if (mounted) {
            setState(() {
              _loadingFiles = false;
            });
          }
        });
      } else {
        unSubscribeToMessages();
      }
    } catch (e) {
      print('Error connecting to SSE: $e');
    }
  }

  void unSubscribeToMessages() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);

    try {
      await codechatService.unSubscribeToMessages();
      setState(() {
        _loadingFiles = false;
      });
    } catch (e) {
      print('Error stopping SSE subscription: $e');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error stopping SSE subscription: $e'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  Future<void> testSubscription() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);

    if (!_loadingFiles) {
      setState(() {
        _loadingFiles = true;
      });

      await codechatService.subscribeToMessages('testdebug', (event, data) {
        setState(() {
          _debugMessages.add(data);
        });
      }, (error) {
        setState(() {
          _errorMessage = 'Error: $error';
        });
      }, () {
        setState(() {
          _loadingFiles = false;
        });
      });
    } else {
      unSubscribeToMessages();
      setState(() {
        _loadingFiles = false;
      });
    }
  }
}
