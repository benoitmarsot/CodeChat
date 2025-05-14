import 'dart:async';
import 'package:codechatui/src/models/project_resources.dart';
import 'package:codechatui/src/services/project_service.dart';
import 'package:flutter/scheduler.dart'; // Import SchedulerBinding
import 'package:codechatui/src/config/app_config.dart';

import 'package:flutter/material.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/services/codechat_service.dart';
import 'package:provider/provider.dart';
import 'package:codechatui/src/widgets/project/github_widget.dart';
import 'package:codechatui/src/widgets/project/zip_widget.dart';
import 'package:codechatui/src/widgets/project/web_widget.dart';

enum Temperature { extraSmall, small, medium, large, extraLarge }

class ProjectResources extends StatefulWidget {
  final List<ProjectResource> projectResources;
  final ValueChanged<List<ProjectResource>>? onResourcesChanged;
  final int? projectId;
  final bool? isEditing;

  //final ResourceType resourceType;
  final Future<void> Function(ProjectResource resource)? onComplete;
  //final Function(ProjectResource, int) onSave;
  //final Function onCancel;

  const ProjectResources({
    Key? key,
    required this.projectResources,
    this.onResourcesChanged,
    required this.projectId,
    this.isEditing = false,
    //this.resourceType = ResourceType.web,
    this.onComplete,
    //required this.onCancel,
  }) : super(key: key);

  @override
  _ProjectResourcesState createState() => _ProjectResourcesState();
}

class _ProjectResourcesState extends State<ProjectResources> {
  // final TextEditingController _prjNameController = TextEditingController();
  // final TextEditingController _prjDescController = TextEditingController();

  final TextEditingController _branchNameController =
      TextEditingController(text: 'main');
  final TextEditingController _userNameController = TextEditingController();
  final TextEditingController _userPswController = TextEditingController();
  final TextEditingController _patController = TextEditingController();
  //final TextEditingController _zipFilePathController = TextEditingController();
  final TextEditingController _webURLController = TextEditingController();
  final TextEditingController _maxPagesController =
      TextEditingController(text: '100');
  final TextEditingController _maxDepthController =
      TextEditingController(text: '2');
  final TextEditingController _requestsPerMinuteController =
      TextEditingController(text: '200');
  List<ProjectResource> _projectResources = []; // Local state
  ResourceType _selectedResourceType = ResourceType.web;

  final ScrollController _scrollController = ScrollController();
  int? _projectId;
  Future<void> Function(ProjectResource)? _onComplete;

  List<String> _debugMessages = [];
  String? _errorMessage;
  bool _isEditing = false;
  // bool _loadingDetails = false;
  bool _loadingFiles = false;
  bool _showDebugMessages = false;
  // bool _showDetails = false;
  // Project? _selectedProject;
  late List<String> _domains = [];
  final _formKey = GlobalKey<FormState>();
  Set<Temperature> selection = <Temperature>{Temperature.medium};

  int? _hoveredIndex;
  @override
  void initState() {
    super.initState();
    _isEditing = widget.isEditing ?? false;
    _projectId = widget.projectId;

    _projectResources = widget.projectResources;
    _onComplete = widget.onComplete;
    //if (!_isEditing && _projectId != null) {
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _loadingFiles || _showDebugMessages
            ? Padding(
                padding: const EdgeInsets.all(8.0),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Theme(
                        data: Theme.of(context)
                            .copyWith(dividerColor: Colors.transparent),
                        child: ExpansionTile(
                          backgroundColor:
                              Theme.of(context).colorScheme.surfaceContainerLow,
                          initiallyExpanded: true,
                          title: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Row(
                                children: [
                                  SizedBox(
                                    width: 20,
                                    height: 20,
                                    child: _showDebugMessages
                                        ? Center(
                                            child: Icon(Icons.check_circle,
                                                color: Colors.green))
                                        : CircularProgressIndicator(
                                            strokeWidth: 2.0),
                                  ),
                                  SizedBox(width: 8),
                                  Text(_showDebugMessages
                                      ? "Resource added"
                                      : "Adding resource..."),
                                  SizedBox(width: 8),
                                  Builder(
                                    builder: (BuildContext context) {
                                      final ExpansionTileController controller =
                                          ExpansionTileController.of(context);
                                      return Text(
                                        controller.isExpanded
                                            ? 'Hide Details'
                                            : 'View Details',
                                        style: TextStyle(
                                          color: Colors.blue,
                                          decoration: TextDecoration.underline,
                                        ),
                                      );
                                    },
                                  ),
                                ],
                              ),
                              IconButton(
                                icon: Icon(Icons.close),
                                onPressed: () {
                                  setState(() {
                                    _loadingFiles = false;
                                    _showDebugMessages = false;
                                  });
                                },
                              ),
                            ],
                          ),
                          children: [
                            _buildDebugMessagesWidget(),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              )
            : SizedBox.shrink(),
        _buildProjectResources(),
      ],
    );
  }

  Future<void> _deleteResource(ProjectResource resource) async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);

    try {
      // Adjust the method below to match your project service code
      await projectService.deleteResource(resource.resourceId!);
      setState(() {
        _projectResources.remove(resource);
        widget.onResourcesChanged?.call(_projectResources);
      });

      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text('Resource removed successfully!'),
          backgroundColor: Colors.greenAccent));
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text('Failed delete resource.'),
          backgroundColor: Colors.redAccent));
    } finally {
      Navigator.of(context).pop();
    }
  }

  Future<void> _handleDelete(ProjectResource resource, int index) async {
    await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Confirm Deletion'),
        content: Text(
            'Are you sure you want to delete the project \n "${resource.uri}"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text('Cancel'),
          ),
          TextButton(
            onPressed: () => _deleteResource(resource),
            child: Text('Delete', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Card(
        elevation: 0,
        margin: EdgeInsets.all(16),
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.folder_open, size: 60, color: Colors.grey),
              SizedBox(height: 20),
              Text(
                'No resources found',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w500,
                  color: Colors.grey[700],
                ),
              ),
              SizedBox(height: 12),
              Text(
                'Add resources to your project to start building your AI assistant.',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 16,
                  color: Colors.grey[600],
                ),
              ),
              ElevatedButton.icon(
                onPressed: _handleProjectResourceChoice,
                icon: Icon(Icons.add),
                label: Text('Add Resource'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildProjectResources() {
    return Card.filled(
        color: Theme.of(context).colorScheme.surfaceContainerLow,
        elevation: 0,
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: _projectResources.isNotEmpty
              ? Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  ListView.builder(
                    shrinkWrap: true,
                    physics:
                        NeverScrollableScrollPhysics(), // Prevent scrolling inside the form
                    itemCount: _projectResources.length,
                    itemBuilder: (context, index) {
                      return _buildProjectResourceItem(context, index);
                    },
                  ),
                  const SizedBox(height: 8),
                  ElevatedButton.icon(
                    onPressed: _handleProjectResourceChoice,
                    icon: Icon(Icons.add),
                    label: Text('Add Resource'),
                  ),
                ])
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [_buildEmptyState()]),
        ));
  }

  Widget _buildResource(Widget form) {
    return Card(
        elevation: 0,
        child: ConstrainedBox(
            constraints: BoxConstraints(maxWidth: 1200),
            child: Padding(
                padding: const EdgeInsets.all(24.0),
                child: Form(
                    key: _formKey,
                    child: Column(children: [
                      form,
                      const SizedBox(height: 30),
                      _buildBottomButtons(),
                    ])))));
  }

  Future<void> _navigateToResource(ProjectResource resource) async {
    Widget form;
    switch (resource.restype) {
      case ResourceType.git:
        form = GithubForm(
          prjRepoURLController: _webURLController,
          branchNameController: _branchNameController,
          userNameController: _userNameController,
          userPswController: _userPswController,
          patController: _patController,
        );

      case ResourceType.zip:
        form = ZipForm(
          onFileSelected: addFilesToProject,
          projectId: _projectId ?? 0,
        );

      case ResourceType.web:
        form = WebForm(
          webURLController: _webURLController,
          allowedDomains: _domains ?? [],
          userNameController: _userNameController,
          userPswController: _userPswController,
          maxPagesController: _maxPagesController,
          maxDepthController: _maxDepthController,
          requestsPerMinuteController: _requestsPerMinuteController,
          //onDomainsChanged: _updateDomains,
        );
      // default:
      //   // Handle unknown resource types
      //   form = Center(child: Text('Unsupported resource type'));
      //   // Navigate to the form
      //   await Navigator.push(
      //     context,
      //     MaterialPageRoute(
      //       builder: (context) => Scaffold(
      //         appBar: AppBar(title: Text('Resource Setup')),
      //         body: form,
      //       ),
      //     ),
      //   );
    }

    // Navigate to the form
    await Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => Scaffold(
            appBar: AppBar(title: Text('Resource Settings')),
            body: SingleChildScrollView(
              padding: const EdgeInsets.all(24.0),
              child: Center(child: _buildResource(form)),
            ),
          ),
        ));
  }

  Widget _buildProjectResourceItem(BuildContext context, int index) {
    bool isHovered = false;

    ProjectResource resource = _projectResources[index];

    return StatefulBuilder(
      builder: (context, setState) {
        return MouseRegion(
          onEnter: (_) => setState(() => _hoveredIndex = index),
          onExit: (_) => setState(() => _hoveredIndex = null),
          child: Container(
            decoration: BoxDecoration(
              color: (_hoveredIndex == index &&
                      resource.restype != ResourceType.zip)
                  ? Theme.of(context).colorScheme.primaryContainer
                  : null,
              borderRadius: BorderRadius.circular(8),
            ),
            child: ListTile(
              title: Row(
                children: [
                  Icon(
                    resource.restype == ResourceType.zip
                        ? Icons.archive
                        : resource.restype == ResourceType.git
                            ? Icons.code
                            : Icons.web,
                  ),
                  const SizedBox(width: 8),
                  Expanded(child: Text(resource.uri ?? '')),
                ],
              ),
              //onTap: () {
              // if (resource.restype != ResourceType.zip) {
              //   _navigateToResource(resource);
              // } else {
              //   // Handle zip resource tap
              //   print('Zip resource tapped');
              // }
              //},
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  (resource.restype == ResourceType.git)
                      ? IconButton(
                          icon: Icon(Icons.refresh),
                          onPressed: () {
                            showDialog(
                              context: context,
                              builder: (context) => AlertDialog(
                                title: Text('Confirm Refresh'),
                                content: Text(
                                    'Are you sure you want to refresh the repository?'),
                                actions: [
                                  TextButton(
                                    onPressed: () =>
                                        Navigator.of(context).pop(), // Cancel
                                    child: Text('Cancel'),
                                  ),
                                  TextButton(
                                    onPressed: () {
                                      Navigator.of(context)
                                          .pop(); // Close dialog
                                      refreshRepo(_projectId!);
                                    },
                                    child: Text(
                                      'Refresh',
                                      style: TextStyle(
                                          color: Theme.of(context)
                                              .colorScheme
                                              .primary),
                                    ),
                                  ),
                                ],
                              ),
                            );

                            //@todo: Add API support we should call explict resource
                          })
                      : const SizedBox.shrink(),
                  IconButton(
                    icon: Icon(Icons.delete,
                        color: isHovered ? Colors.red : Colors.grey),
                    onPressed: () => _handleDelete(resource, index),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
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

  void _cancelEdit() {
    Navigator.pop(context);

    // widget.onSave!();
  }

  Widget _buildBottomButtons() {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 16.0, horizontal: 8.0),
      child: Row(mainAxisAlignment: MainAxisAlignment.end, children: [
        ElevatedButton(
          onPressed: _cancelEdit,
          style: ElevatedButton.styleFrom(
            textStyle: const TextStyle(fontSize: 16),
          ),
          child: const Text('Cancel'),
        ),
        _selectedResourceType != ResourceType.zip
            ? ElevatedButton(
                onPressed: addProject,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Theme.of(context).colorScheme.primary,
                  textStyle: const TextStyle(fontSize: 16),
                  foregroundColor: Theme.of(context).colorScheme.onPrimary,
                ),
                child: const Text('Add Resource'),
              )
            : SizedBox.shrink(),
      ]),
    );
  }

  Future<void> addProject() async {
    if (_selectedResourceType == ResourceType.git) {
      await addProjectRepo();
    } else if (_selectedResourceType == ResourceType.web) {
      await addProjectWeb();
    }
    return;
  }

  Future<void> addProjectWeb() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);
    String? clientId;

    if (_formKey.currentState!.validate()) {
      setState(() {
        _loadingFiles = true;
        _errorMessage = null; // Clear previous error
      });

      //_buildLoadingDialog();
      try {
        clientId = await subscribeToMessages();

        SchedulerBinding.instance.addPostFrameCallback((_) {
          // Delay Navigator.pop
          if (mounted) {
            Navigator.of(context).pop(); // go back to overview
          }
        });

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
        // if (mounted) {
        //   _buildSuccessDialog();
        // }
      } catch (e) {
        setState(() {
          String errorMsg = e.toString();
          if (errorMsg.length > 250) {
            errorMsg = errorMsg.substring(0, 250);
          }
          _errorMessage = 'Failed to create project: $errorMsg';
        });
      } finally {
        _handleCompletion(_webURLController.text);
      }
    }
  }

  Future<void> addProjectRepo() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);
    String? clientId;

    if (_formKey.currentState!.validate()) {
      setState(() {
        _loadingFiles = true;
        _errorMessage = null;
      });

      // Show loading dialog
      //_buildLoadingDialog();

      try {
        clientId = await subscribeToMessages();

        if (clientId == null) {
          throw Exception('Failed to subscribe to messages.');
        }
        if (mounted) {
          Navigator.of(context).pop(); // go back to overview
        }

        await codechatService.addProjectRepo(
            projectId: _projectId!,
            repoUrl: _webURLController.text,
            branchName: _branchNameController.text,
            userName: _userNameController.text,
            password: _userPswController.text,
            clientId: clientId);

        // if (mounted) {
        //   _buildSuccessDialog();
        // }
      } catch (e) {
        setState(() {
          String errorMsg = e.toString();
          if (errorMsg.length > 250) {
            errorMsg = errorMsg.substring(0, 250);
          }
          _errorMessage = 'Failed to create project: $errorMsg';
        });
      } finally {
        if (mounted) {
          _handleCompletion(_webURLController.text);
        }
      }
    }
  }

  Future<void> addFilesToProject(String content, String fileName) async {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return AlertDialog(
          content: Padding(
            padding: const EdgeInsets.all(24.0),
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
                addProjectZip(content, fileName);
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

  Future<void> addProjectZip(String content, String fileName) async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);
    String? clientId;

    setState(() {
      _loadingFiles = true;
      _errorMessage = null; // Clear previous error
    });
    try {
      SchedulerBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          Navigator.of(context).pop(); // go back to overview
        }
      });
      clientId = await subscribeToMessages();
      await codechatService.addProjectZip(
          _projectId!, content, fileName, clientId);

      // Dismiss loading dialog
      // if (mounted) {
      //   _buildSuccessDialog();
      // }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to fetch project details: $e';
        });
      }
    } finally {
      _handleCompletion(fileName);
    }
  }

  Future<void> refreshRepo(int projectId) async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final codechatService = CodechatService(authProvider: authProvider);
    String? clientId;

    try {
      setState(() {
        _loadingFiles = true;
      });
      clientId = await subscribeToMessages();
      await codechatService.refreshRepo(projectId, clientId);

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Repository refresh initiated successfully!'),
          backgroundColor: Colors.greenAccent,
        ),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Failed to initiate repository refresh: $e'),
          backgroundColor: Colors.redAccent,
        ),
      );
    } finally {
      if (mounted) {
        setState(() {
          _loadingFiles = false;
        });
      }
    }
  }

  void _handleCompletion(String uri) {
    if (mounted) {
      final newResource = ProjectResource(
        restype: _selectedResourceType,
        uri: uri,
      );
      if (_onComplete != null) {
        _onComplete!(newResource);
      }

      if (!_projectResources.any((res) =>
          res.uri == newResource.uri && res.restype == newResource.restype)) {
        _projectResources.add(newResource);
        widget.onResourcesChanged?.call(_projectResources);
      }
      setState(() {
        _loadingFiles = false;
        _showDebugMessages = true;
      });
      if (_errorMessage != null && _errorMessage!.isNotEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(_errorMessage!),
            duration: const Duration(seconds: 5),
            backgroundColor: Colors.redAccent,
          ),
        );
      }
    }
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
          if (mounted) {
            setState(() {
              _debugMessages = [..._debugMessages, data];
            });
          }
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
          // Complete the completer *after* clientId is assigned
          if (!completer.isCompleted) {
            completer.complete(true);
          }
        });
        // Wait for connection to be established or fail with a timeout
        return await completer.future.timeout(Duration(seconds: 10),
            onTimeout: () {
          print('SSE connection timed out');
          return false;
        }).then((connected) => connected && clientId != null
            ? clientId
            : null); // Check for null clientId
      } else {
        if (clientId != null) {
          await unSubscribeToMessages(clientId!);
        }
        return null;
      }
    } catch (e) {
      print('Error connecting to SSE: $e');
      return null;
    }
  }

  Widget _buildDebugMessagesWidget() {
    //if (_debugMessages.isEmpty) return const SizedBox.shrink();

    return Container(
      height: 200,
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
    );
  }

  // void _buildSuccessDialog() {
  //   showDialog(
  //     context: context,
  //     builder: (BuildContext context) {
  //       return AlertDialog(
  //         title: Row(
  //           children: [
  //             Icon(Icons.check_circle, color: Colors.green),
  //             SizedBox(width: 8),
  //             const Text('Success'),
  //           ],
  //         ),
  //         content: Text('DataSource configured successfully!'),
  //         actions: [
  //           TextButton(
  //             onPressed: () {
  //               if (mounted) {
  //                 Navigator.of(context).pop(); //close
  //                 Navigator.of(context).pushReplacementNamed('/');
  //                 if (_onSave != null) {
  //                   _onSave!();
  //                 }
  //               }
  //             },
  //             child: Text('View Projects'),
  //           ),
  //           TextButton(
  //             onPressed: () {
  //               Navigator.of(context).pop();
  //               //_redirectToChat();
  //             },
  //             child: Text('Start Discussion'),
  //           ),
  //         ],
  //       );
  //     },
  //   );
  // }

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
              backgroundColor: Colors.redAccent,
              duration: const Duration(seconds: 5)),
        );
      }
      return false;
    }
  }

  Future<void> _handleProjectResourceChoice() async {
    final newResource = await showDialog<ProjectResource>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setState) {
            return AlertDialog(
              title: Text('Select Resource Type'),
              content: SizedBox(
                width: 500,
                height: 200,
                child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Column(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      SizedBox(height: 16),
                      Tooltip(
                          preferBelow: false,
                          waitDuration: Duration(milliseconds: 1000),
                          height: 48,
                          message:
                              'Add an entry point to which the system will crawl, index, and analyze.\nAll linked pages will be processed recursively to train the model on your sites content.',
                          child: ChoiceChip(
                            avatar: SizedBox(
                              width: 40,
                              height: 40,
                              child: Icon(Icons.web, size: 32),
                            ),
                            label: Text(
                                'Website: Provide a URL to analyse.             '),
                            labelPadding: EdgeInsets.symmetric(
                                horizontal: 16.0, vertical: 8.0),
                            selected: _selectedResourceType == ResourceType.web,
                            onSelected: (selected) {
                              if (selected) {
                                setState(() {
                                  _selectedResourceType = ResourceType.web;
                                });
                              }
                            },
                          )),
                      SizedBox(height: 16),
                      Tooltip(
                          preferBelow: false,
                          waitDuration: Duration(milliseconds: 1000),
                          message:
                              ' A compressed archive containing files or documents for analysis. \n The system will extract the content and index it for training.',
                          child: ChoiceChip(
                            avatar: SizedBox(
                              width: 40,
                              height: 40,
                              child: Icon(Icons.archive, size: 32),
                            ),
                            label: Text(
                                'Zip File: Upload a archive for analysis.       '),
                            labelPadding: EdgeInsets.symmetric(
                                horizontal: 16.0, vertical: 8.0),
                            selected: _selectedResourceType == ResourceType.zip,
                            onSelected: (selected) {
                              if (selected) {
                                setState(() {
                                  _selectedResourceType = ResourceType.zip;
                                });
                              }
                            },
                          )),
                      SizedBox(height: 16),
                      Tooltip(
                          preferBelow: false,
                          waitDuration: Duration(milliseconds: 1000),
                          message:
                              'The system will clone the repo, parse relevant files,\n and index the content to train the AI with your codebase or documentation',
                          child: ChoiceChip(
                            avatar: SizedBox(
                              width: 40,
                              height: 40,
                              child: Icon(Icons.code, size: 32),
                            ),
                            label: Text(
                                'Git:  Connect a repository to retrieve code.'),
                            labelPadding: EdgeInsets.symmetric(
                                horizontal: 16.0, vertical: 8.0),
                            selected: _selectedResourceType == ResourceType.git,
                            onSelected: (selected) {
                              if (selected) {
                                setState(() {
                                  _selectedResourceType = ResourceType.git;
                                });
                              }
                            },
                          )),
                    ],
                  )
                ]),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(null),
                  child: Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: () {
                    Navigator.of(context).pop();
                    _navigateToResource(ProjectResource(
                        restype: _selectedResourceType, uri: ''));
                  },
                  child: Text('Continue',
                      style: TextStyle(
                          color: Theme.of(context).colorScheme.primary)),
                ),
              ],
            );
          },
        );
      },
    );
    if (newResource != null) {
      setState(() {
        _projectResources.add(newResource);
        widget.onResourcesChanged?.call(_projectResources);
      });
    }
  }
}
