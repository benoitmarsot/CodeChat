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
import 'package:codechatui/src/widgets/project/web_widget.dart';
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

  final TextEditingController _maxPagesController =
      TextEditingController(text: '100');
  final TextEditingController _maxDepthController =
      TextEditingController(text: '2');
  final TextEditingController _requestsPerMinuteController =
      TextEditingController(text: '30');
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

  DataSourceType _selectedDataSource = DataSourceType.web;
  bool _isEditing = false;
  bool _loadingFiles = false;
  bool _showDetails = false;
  Project? _selectedProject;
  //List<ProjectResource> _selectedProjectResources = [];
  late List<String> _domains = [];
  late TabController _tabController;

  final _formKey = GlobalKey<FormState>();
  Set<Temperature> selection = <Temperature>{Temperature.medium};
  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _projectId = widget.projectId;
    if (widget.onSave != null) _onSave = widget.onSave;
    _isEditing = widget.isEditing;
    if (_isEditing) {
      _fetchProjectDetails();
      //_fetchProjectResources();
    }
  }

  @override
  void dispose() {
    // TODO: Make a loop for client ids?
    // if (_isSubscribed) {
    //   unSubscribeToMessages();
    // }
    _tabController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _loadProjectData() {
    if (_projectId != null) {
      _prjNameController.text = _selectedProject!.name;
      _prjDesctController.text = _selectedProject!.description;
      _webURLController.text = _selectedProject!.resourceUris.isNotEmpty
          ? _selectedProject!
              .resourceUris[_selectedProject!.resourceUris.length - 1]
          : '';
      _domains = _selectedProject!.allowedDomains;
      // You might need to fetch other project details here based on your data model
    } else {
      _prjNameController.clear();
      _prjDesctController.clear();
      _prjRepoURLController.clear();
      //_webURLs.clear();
    }
  }

  void _updateDomains(List<String> newDomains) {
    setState(() {
      _domains = newDomains;
    });
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

      if (_selectedDataSource != DataSourceType.zip) {
        try {
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
                      Text("Creating project..."),
                    ],
                  ),
                ),
              );
            },
          );

          final project = await codechatService.createEmtptyProject(
              _prjNameController.text, _prjDesctController.text, 'not-used');

          Navigator.of(context).pop();

          setState(() {
            _selectedProject =
                project as Project; //todo do we need both _selectedProject and
            _showDetails = true;
            _projectId = project.projectId;
          });
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

    widget.onSave!();
  }

  // Future<void> _saveChanges() async {
  //   print('_saveChanges $_prjNameController.text');

  //   final authProvider = Provider.of<AuthProvider>(context, listen: false);
  //   final projectService = ProjectService(authProvider: authProvider);

  //   if (_formKey.currentState!.validate()) {
  //     try {
  //       // Update the project with the new values
  //       final updatedProject = _selectedProject!.copyWith(
  //         name: _prjNameController.text,
  //         description: _prjDesctController.text,
  //       );

  //       await projectService.updateProject(updatedProject);
  //       // if (hasDataSource) {
  //       //   // If there's a data source, you might want to refresh or update it here
  //       //   //await projectService.refreshRepo(_selectedProject!.projectId);
  //       // }
  //       setState(() {
  //         //_isEditing = false;
  //         _createdMessage = 'Project updated successfully!';

  //         Navigator.pop(context);
  //         if (mounted) {
  //           ScaffoldMessenger.of(context).showSnackBar(SnackBar(
  //               content: Text(_createdMessage),
  //               backgroundColor: Colors.greenAccent));
  //         }
  //       });
  //       _onSave!();
  //     } catch (e) {
  //       setState(() {
  //         _errorMessage = 'Failed to update project: $e';
  //       });
  //       print('_errorMessage $_errorMessage');
  //     }
  //   }
  // }

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
    if (_projectId != null && _projectId! <= 0) {
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

  Future<void> _refreshDataSource() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechattService = CodechatService(authProvider: authProvider);
    String? clientId;
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
      clientId = await subscribeToMessages();
      await codechattService.refreshRepo(_projectId!, clientId);
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

  Widget _buildProjectInfo() {
    return _showDetails
        ? Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Project Name: ${_selectedProject!.name}',
                style:
                    const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                'Description: ${_selectedProject!.description}',
                style: const TextStyle(fontSize: 16),
              ),
            ],
          )
        : Column(children: [
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
                  fillColor:
                      Theme.of(context).colorScheme.surfaceContainerLowest
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
                  fillColor:
                      Theme.of(context).colorScheme.surfaceContainerLowest
                  //border: OutlineInputBorder(),
                  ),
            )
          ]);
  }

  Widget _buildProjectDetail() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      _buildProjectInfo(),
      _showDetails || _isEditing
          ? _buildDataSourceChoice()
          : const SizedBox.shrink(),
      _showDetails || _isEditing
          ? _buildDataSourceForm()
          : const SizedBox.shrink(),
      _buildBottomButtons(),
      _buildDebugMessagesWidget()
    ]);
  }

  Widget _buildBottomButtons() {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 16.0, horizontal: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: _showDetails || _isEditing
            ? [
                ElevatedButton(
                  onPressed: _cancelEdit,
                  style: ElevatedButton.styleFrom(
                    textStyle: const TextStyle(fontSize: 16),
                  ),
                  child: const Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: addProject,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.primary,
                    textStyle: const TextStyle(fontSize: 16),
                    foregroundColor: Theme.of(context).colorScheme.onPrimary,
                  ),
                  child: (_isEditing)
                      ? const Text('Apply')
                      : const Text('Complete Data Source Setup'),
                ),
              ]
            : [
                if (!_isEditing)
                  ElevatedButton(
                    onPressed: _cancelEdit,
                    style: ElevatedButton.styleFrom(
                      textStyle: const TextStyle(fontSize: 16),
                    ),
                    child: const Text('Cancel'),
                  ),
                if (!_isEditing) const SizedBox(width: 8),
                if (!_isEditing)
                  ElevatedButton(
                    onPressed: _createdProject,
                    style: ElevatedButton.styleFrom(
                      textStyle: const TextStyle(fontSize: 16),
                    ),
                    child: const Text('Create'),
                  ),
              ],
      ),
    );
  }

  Widget _buildDebugMessagesWidget() {
    if (_debugMessages.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 32),
        Container(
          height: 150,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey),
            borderRadius: BorderRadius.circular(8.0),
            color: Theme.of(context).colorScheme.primary.withOpacity(0.1),
          ),
          child: ListView.builder(
            controller: _scrollController,
            itemCount: _debugMessages.length,
            itemBuilder: (context, index) {
              return Padding(
                padding: const EdgeInsets.all(4.0),
                child: Text(_debugMessages[index]),
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildDataSourceChoice() {
    return Column(children: [
      Container(
        margin: const EdgeInsets.only(bottom: 16.0, top: 24.0),
        child: Row(children: [
          Text('Choose Data Source:',
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
            Tooltip(
                height: 48,
                message:
                    'Add an entry point to which the system will crawl, index, and analyze.\nAll linked pages will be processed recursively to train the AI on your sites content.',
                child: ChoiceButton(
                  text: 'Web URL',
                  icon: Icons.web,
                  isSelected: _selectedDataSource == DataSourceType.web,
                  onPressed: () {
                    setState(() {
                      _selectedDataSource = DataSourceType.web;
                    });
                  },
                )),
            const SizedBox(width: 16),
            Tooltip(
                message:
                    'The system will clone the repo, parse relevant files,\n and index the content to train the AI with your codebase or documentation',
                child: ChoiceButton(
                  text: 'File(s)',
                  icon: Icons.archive,
                  isSelected: _selectedDataSource == DataSourceType.zip,
                  onPressed: () {
                    setState(() {
                      _selectedDataSource = DataSourceType.zip;
                    });
                  },
                )),
            const SizedBox(width: 16),
            Tooltip(
                message:
                    'The system will clone the repo, parse relevant files,\n and index the content to train the AI with your codebase or documentation',
                child: ChoiceButton(
                  text: 'GitHub',
                  icon: Icons.code,
                  isSelected: _selectedDataSource == DataSourceType.github,
                  onPressed: () {
                    setState(() {
                      _selectedDataSource = DataSourceType.github;
                    });
                  },
                )),
            const SizedBox(width: 16),
          ],
        ),
      )
    ]);
  }

  Widget _buildDataSourceForm() {
    switch (_selectedDataSource) {
      case DataSourceType.github:
        return GithubForm(
          prjRepoURLController: _webURLController,
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
                isLoading: _loadingFiles)
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
        return WebForm(
          webURLController: _webURLController,
          allowedDomains: _domains ?? [],
          userNameController: _userNameController,
          userPswController: _userPswController,
          maxPagesController: _maxPagesController,
          maxDepthController: _maxDepthController,
          requestsPerMinuteController: _requestsPerMinuteController,
          onDomainsChanged: _updateDomains,
        );
      //isDisabled: _isEditing);
      default:
        return const SizedBox.shrink();
    }
  }

  //todo: from settings
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
                  label: const Text('Delete'),
                  icon: Icon(Icons.delete),
                  onPressed: _handleDelete,
                ),
              ),
          ],
          title: Text(_isEditing == true
              ? 'Project Settings'
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

  Future<void> addProject() async {
    if (_selectedDataSource == DataSourceType.github) {
      await addProjectGithub();
    }
    // else if (_selectedDataSource == DataSourceType.zip) {
    //   await _addProjectZip();
    // }
    else if (_selectedDataSource == DataSourceType.web) {
      await addProjectWeb();
    }
    return;
  }

  Future<void> addProjectGithub() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);

    String? clientId;
    setState(() {
      _loadingFiles = true;
      _errorMessage = null; // Clear previous error
    });

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
                Text("Setting up project...\nIt will take a while..."),
              ],
            ),
          ),
        );
      },
    );
    try {
      clientId = await subscribeToMessages();
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
        clientId,
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
      _selectedProject = project as Project?;
      _onSave!();

      Navigator.of(context).pop();
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to upload directory: $e';
      });
      print('Failed to upload directory: $e');
      // TODO: Needed-it?
      //unSubscribeToMessages();
      // ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      //     content: Text(_errorMessage!), backgroundColor: Colors.red));
    }
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

  // bool _validateWebURLs() {
  //   // Ensure at least one non-empty URL exists
  //   return widget.domains.any((url) => url.trim().isNotEmpty);
  // }

  Future<void> addProjectWeb() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);
    String? clientId;

    if (_formKey.currentState!.validate()) {
      setState(() {
        _loadingFiles = true;
        _errorMessage = null; // Clear previous error
      });
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
                  Text(
                      "Setting up your project’s data source \nIt will take a while..."),
                ],
              ),
            ),
          );
        },
      );
      try {
        clientId = await subscribeToMessages();
        if (_projectId == null || _projectId! < 0) {
          throw Exception('Invalid project ID');
        }
        print('Project ID: $_projectId');
        await codechatService.addProjectWeb(
            projectId: _projectId!,
            seedUrl: _webURLController.text,
            maxPages: int.tryParse(_maxPagesController?.text ?? ''),
            maxDepth: int.tryParse(_maxDepthController?.text ?? ''),
            requestsPerMinute:
                int.tryParse(_requestsPerMinuteController?.text ?? ''),
            allowedDomains: _domains,
            clientId: clientId);

        // Dismiss loading dialog
        if (mounted) {
          Navigator.of(context).pop();

          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
                content: Text('File(s) uploaded successfully!'),
                backgroundColor: Colors.greenAccent),
          );
        }
        _buildSuccessDialog();
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
  }

  Future<void> _handleFileAPI(String content, String fileName) async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);
    String? clientId;

    setState(() {
      _loadingFiles = true;
      _errorMessage = null; // Clear previous error
    });
    try {
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
                  Text(
                      "Setting up your project’s data source \nIt will take a while..."),
                ],
              ),
            ),
          );
        },
      );
      clientId = await subscribeToMessages();
      await codechatService.addProjectZip(
          _projectId!, content, fileName, clientId);

      // Dismiss loading dialog
      if (mounted) {
        Navigator.of(context).pop();
      }

      _buildSuccessDialog();
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

  void _buildSuccessDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Row(
            children: [
              Icon(Icons.check_circle, color: Colors.green),
              SizedBox(width: 8),
              const Text('Success'),
            ],
          ),
          content: Text('DataSource configured successfully!'),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop(); // Close the dialog
                Navigator.of(context).pop(); // Navigate
              },
              child: Text('View Projects'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                _redirectToChat();
              },
              child: Text('Start Discussion'),
            ),
          ],
        );
      },
    );
  }

  void _redirectToChat() {
    Navigator.pushNamed(
      context,
      'chat',
      arguments: _selectedProject,
    );
  }

  Future<String?> subscribeToMessages() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);
    String? clientId;
    _debugMessages.clear();

    final completer = Completer<bool>();

    try {
      if (_loadingFiles) {
        clientId =
            await codechatService.subscribeToMessages(onEvent: (event, data) {
          setState(() {
            _debugMessages = [..._debugMessages, data];
          });
          _scrollToBottom();
        }, onError: (error) {
          if (mounted) {
            setState(() {
              _errorMessage = 'Error: $error';
            });
          }
          if (!completer.isCompleted) {
            completer.complete(false);
          }
        }, onDone: () {
          if (mounted) {
            setState(() {
              _loadingFiles = false;
            });
          }
        }, onConnected: () {
          print('SSE connection established successfully');
          if (!completer.isCompleted) {
            completer.complete(true);
          }
        });
        // Wait for connection to be established or fail with a timeout
        return await completer.future.timeout(Duration(seconds: 10),
            onTimeout: () {
          print('SSE connection timed out');
          return false;
        }).then((connected) => connected ? clientId : null);
      } else {
        await unSubscribeToMessages(clientId!);
        return null;
      }
    } catch (e) {
      print('Error connecting to SSE: $e');
      return null;
    }
  }

  Future<bool> unSubscribeToMessages(String clientId) async {
    if (!AppConfig.PublicDebuggingEnabled) return false;

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);

    try {
      await codechatService.unSubscribeToMessages(clientId);
      setState(() {
        _loadingFiles = false;
      });
      return true;
    } catch (e) {
      print('Error stopping SSE subscription: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error stopping SSE subscription: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
      return false;
    }
  }

  // Future<void> testSubscription() async {
  //   if (!AppConfig.PublicDebuggingEnabled) return;
  //   final authProvider = Provider.of<AuthProvider>(context, listen: false);
  //   final codechatService = CodechatService(authProvider: authProvider);

  //   if (!_loadingFiles) {
  //     setState(() {
  //       _loadingFiles = true;
  //     });

  //     await codechatService.subscribeToMessages('testdebug',
  //         onEvent: (event, data) {
  //       setState(() {
  //         _debugMessages = [..._debugMessages!, data];
  //         //_debugMessages.add(data);
  //       });
  //       _scrollToBottom();
  //     }, onError: (error) {
  //       if (mounted) {
  //         setState(() {
  //           _errorMessage = 'Error: $error';
  //         });
  //       }
  //     }, onDone: () {
  //       if (mounted) {
  //         setState(() {
  //           _loadingFiles = false;
  //         });
  //       }
  //     });
  //   } else {
  //     unSubscribeToMessages();
  //     setState(() {
  //       _loadingFiles = false;
  //     });
  //   }
  // }
}
