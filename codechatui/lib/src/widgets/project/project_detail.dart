import 'dart:async';
import 'package:codechatui/src/models/project.dart';
import 'package:codechatui/src/models/project_resources.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/services/project_service.dart';
import 'package:codechatui/src/widgets/project/assistant.dart';
import 'package:codechatui/src/widgets/project/project_resources.dart';
import 'package:codechatui/src/widgets/project/project_form.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart'; // Import intl for date formatting
import 'package:provider/provider.dart';

class ProjectDetail extends StatefulWidget {
  final int projectId;
  final bool isEditing;
  final Future<void> Function()? onSave;
  final ResourceType resourceType;
  const ProjectDetail(
      {super.key,
      this.projectId = -1,
      this.onSave,
      this.resourceType = ResourceType.web,
      required this.isEditing});

  @override
  _ProjectDetailState createState() => _ProjectDetailState();
}

class _ProjectDetailState extends State<ProjectDetail>
    with SingleTickerProviderStateMixin {
  final TextEditingController _prjNameController = TextEditingController();
  final TextEditingController _prjDescController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  ResourceType _selectedResourceType = ResourceType.web;
  int? _projectId;
  bool _isLoading = false;
  Future<void> Function()? _onSave;
  String? _errorMessage;
  Project? _selectedProject;
  late List<ProjectResource> _projectResources = [];
  late TabController _tabController;
  final _formKey = GlobalKey<FormState>();

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _projectId = widget.projectId;
    if (widget.onSave != null) _onSave = widget.onSave;
    _selectedResourceType = widget.resourceType;
    if (widget.isEditing) {
      _fetchProjectDetails();
    } else {
      _selectedProject = Project(
          name: _prjNameController.text,
          description: _prjDescController.text,
          projectId: _projectId ?? -1,
          authorId: -1,
          assistantId: -1);

      // showDialog(
      //     context: context,
      //     builder: (BuildContext context) {
      //       return ProjectForm(
      //           isEditing: false,
      //           projectId: -1,
      //           onCancel: () async => Navigator.of(context).pop(),
      //           onSave: (Project project) async {
      //             Navigator.of(context).pop();
      //             Navigator.push(
      //               context,
      //               MaterialPageRoute(
      //                 builder: (context) => ProjectResources(
      //                   projectResources: _projectResources,
      //                   resourceType: resoureceType!,
      //                   isEditing: false,
      //                   projectId: _projectId!,
      //                   onSave: (int? id, ResourceType? resourceType) async {
      //                     Navigator.of(context).pop();
      //                   },
      //                 ),
      //               ),
      //             );
      //           });
      //     });
      //open create dialog
    }
  }

  @override
  void dispose() {
    _tabController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  ResourceType resourceTypeFromString(String type) {
    return ResourceType.values.firstWhere(
      (e) => e.name == type,
    );
  }

  void _initProjectData() {
    if (_projectId != null) {
      _prjNameController.text = _selectedProject!.name;
      _prjDescController.text = _selectedProject!.description;
      _projectResources = _selectedProject!.resources;
    } else {
      _prjNameController.clear();
      _prjDescController.clear();
    }
  }

  Future<void> _fetchProjectDetails() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);
    setState(() {
      _isLoading = true;
    });
    try {
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
      setState(() {
        _isLoading = false;
      });
      if (mounted && _errorMessage != null && _errorMessage!.isNotEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text(_errorMessage ?? ''),
          duration: const Duration(seconds: 5),
          backgroundColor: Colors.redAccent,
        ));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Theme.of(context).colorScheme.surface,
      child: Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () {
              Navigator.of(context).pop();
            },
          ),
          title: const Text('Project Details'),
        ),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : Column(
                children: [
                  _buildHeader(context),
                  Expanded(
                    child: TabBarView(
                      controller: _tabController,
                      children: [
                        SingleChildScrollView(
                          padding: const EdgeInsets.all(24.0),
                          child: ProjectResources(
                            isEditing: true,
                            projectResources: _projectResources,
                            projectId: _projectId!,
                            // onComplete: (ProjectResource resource) =>
                            //_fetchProjectDetails()
                          ),
                        ),
                        SingleChildScrollView(
                          padding: const EdgeInsets.all(24.0),
                          child: AssistantForm(projectId: _projectId!),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            Theme.of(context).colorScheme.primaryContainer,
            Theme.of(context).colorScheme.surfaceContainer,
          ],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
        ),
      ),
      padding: EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            widget.isEditing == true
                ? _selectedProject?.name ?? 'Project Details'
                : 'Create Project',
            style: TextStyle(
              fontSize: 32,
              fontWeight: FontWeight.bold,
              color: Theme.of(context).colorScheme.onSurface,
            ),
          ),
          SizedBox(height: 8),
          Text(
            widget.isEditing == true
                ? _selectedProject?.description ?? ''
                : 'Create a new project to start building your AI assistant.',
            style: TextStyle(
              fontSize: 16,
              color: Theme.of(context).colorScheme.onPrimaryContainer,
            ),
          ),
          SizedBox(height: 8),

          // if (_selectedProject != null)
          //   Row(
          //     crossAxisAlignment: CrossAxisAlignment.start,
          //     children: [
          //       Text(
          //         'Created by: ${_selectedProject!.author}, ', // Replace with actual author name if available
          //         style: TextStyle(
          //           fontSize: 14,
          //           color: Theme.of(context).colorScheme.onSurfaceVariant,
          //         ),
          //       ),
          //       Text(
          //         'Last Modified: ${DateFormat('yyyy-MM-dd – kk:mm').format(_selectedProject!.updatedAt ?? DateTime.now())}',
          //         style: TextStyle(
          //           fontSize: 14,
          //           color: Theme.of(context).colorScheme.onSurfaceVariant,
          //         ),
          //       ),
          //     ],
          //   ),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              if (widget.isEditing == true)
                Tooltip(
                  message: 'Edit Project Name and Description',
                  child: ElevatedButton.icon(
                    label: const Text('Edit'),
                    icon: Icon(Icons.edit),
                    onPressed: () {
                      showDialog(
                          context: context,
                          builder: (BuildContext context) {
                            return ProjectForm(
                                isEditing: true,
                                projectId: _projectId!,
                                onCancel: () async =>
                                    Navigator.of(context).pop(),
                                onSave: (Project project) async {
                                  Navigator.of(context).pop();
                                  _fetchProjectDetails();
                                  // Navigator.push(
                                  //   context,
                                  //   MaterialPageRoute(
                                  //     builder: (context) => ProjectResources(
                                  //       projectResources: _projectResources,
                                  //       resourceType: resoureceType!,
                                  //       isEditing: false,
                                  //       projectId: _projectId!,
                                  //       onSave: (int? id,
                                  //           ResourceType? resourceType) async {
                                  //         Navigator.of(context).pop();
                                  //       },
                                  //     ),
                                  //   ),
                                  // );
                                });
                          });
                    },
                  ),
                ),
              SizedBox(width: 8),
              if (widget.isEditing == true && _selectedProject != null)
                Tooltip(
                  message: 'Delete Project',
                  child: ElevatedButton.icon(
                    label: const Text('Delete'),
                    icon: Icon(Icons.delete),
                    onPressed: () async {
                      final confirmDelete = await showDialog<bool>(
                        context: context,
                        builder: (context) => AlertDialog(
                          title: Text('Confirm Deletion'),
                          content: Text(
                              'Are you sure you want to delete the project "${_selectedProject!.name}"?'),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.of(context).pop(false),
                              child: Text('Cancel'),
                            ),
                            TextButton(
                              onPressed: () async {
                                final authProvider = Provider.of<AuthProvider>(
                                    context,
                                    listen: false);
                                final projectService =
                                    ProjectService(authProvider: authProvider);
                                if (_projectId == null || _projectId! < 0)
                                  return;
                                try {
                                  await projectService
                                      .deleteProject(_projectId!);
                                  _onSave!();
                                  Navigator.of(context).pop(true);
                                } catch (e) {
                                  setState(() {
                                    _errorMessage =
                                        'Failed to delete project: $e';
                                  });
                                }
                              },
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.red,
                                foregroundColor: Colors.white,
                              ),
                              child: Text('Delete'),
                            ),
                          ],
                        ),
                      );

                      if (confirmDelete == true) {
                        final authProvider =
                            Provider.of<AuthProvider>(context, listen: false);
                        final projectService =
                            ProjectService(authProvider: authProvider);
                        try {
                          await projectService
                              .deleteProject(_selectedProject!.projectId);
                          Navigator.of(context).pop();
                        } catch (e) {
                          setState(() {
                            _errorMessage = 'Failed to delete project: $e';
                          });
                        }
                      }
                    },
                  ),
                ),
            ],
          ),
          TabBar(
            controller: _tabController,
            labelPadding: EdgeInsets.symmetric(horizontal: 20),
            tabs: const [
              Tab(
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: Text('Resources'),
                ),
              ),
              Tab(
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: Text('Assistant'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
