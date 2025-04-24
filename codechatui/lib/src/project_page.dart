import 'package:codechatui/src/models/exceptions.dart';
import 'package:codechatui/src/models/openai/assistant.dart';
import 'package:codechatui/src/models/project.dart';
import 'package:codechatui/src/services/openai/assistant_service.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/widgets/status_tag.dart';
import 'package:flutter/material.dart';
import 'package:codechatui/src/widgets/project/project_detail.dart';
import 'package:provider/provider.dart';
import 'services/project_service.dart';
import 'package:codechatui/src/utils/error_handler.dart';

const Map<String, Color> languageModelColorMap = {
  'gpt_4o': Colors.blueGrey,
  'gpt_4o_mini': Colors.teal,
  'gpt_3.5_turbo': Colors.deepPurple,
  'gpt_4': Colors.grey,
  'gpt_4_turbo': Colors.deepOrange,
  'gpt_4o_realtime_preview': Colors.indigo,
  'o3_mini': Colors.brown,
};

class ProjectPage extends StatefulWidget {
  @override
  State<ProjectPage> createState() => _ProjectPageState();
}

class _ProjectPageState extends State<ProjectPage>
    with SingleTickerProviderStateMixin {
  List<Project> _projects = [];
  int? _hoveredIndex;
  String? _errorMessage;

  Project? _selectedProject; // Store the selected project

  @override
  void initState() {
    super.initState();
    _fetchProjects();
  }

  Future<void> _fetchProjects() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);

    try {
      var projects = await projectService.getAllProjects();

      for (var project in projects) {
        try {
          final assistant = await _fetchProjectAssistant(project.projectId);
          project.assistant = assistant.name;
          project.model = assistant.model;
        } catch (e) {
          project.assistant = '';
          project.model = '';
        }
      }

      setState(() {
        _projects = projects;
      });
      if (projects.length == 1) {
        _selectProject(projects[0]);
      }
      for (var project in projects) {
        try {
          final assistant = await _fetchProjectAssistant(project.projectId);
          project.assistant = assistant.name;
          project.model = assistant.model;
        } catch (e) {
          project.assistant = '';
          project.model = '';
        }
      }
      setState(() {
        _projects = projects;
      });
    } on ForbiddenException catch (e) {
      // Handle 403 error
      ErrorHandler.handleForbiddenError(context, e.message);
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to load projects: $e';
      });
    }
  }

  Future<Assistant> _fetchProjectAssistant(int projectId) async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final assistantService = AssistantService(authProvider: authProvider);

    try {
      return await assistantService.getAssistant(projectId);
    } on ForbiddenException catch (e) {
      // Handle 403 error
      ErrorHandler.handleForbiddenError(context, e.message);
      rethrow;
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to load project assistant: $e';
      });
      rethrow;
    }
  }

  Future<void> _handleDelete(Project project, int index) async {
    await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Confirm Deletion'),
        content: Text(
            'Are you sure you want to delete the project "${project.name}"?'),
        actions: [
          TextButton(
            onPressed: () => _deleteProject(project, index),
            child: Text('Delete', style: TextStyle(color: Colors.red)),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text('Cancel'),
          ),
        ],
      ),
    );
  }

  Future<void> _deleteProject(Project project, int index) async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);

    try {
      // Adjust the method below to match your project service code
      await projectService.deleteProject(project.projectId);
      setState(() {
        _projects.removeAt(index);
      });
      await _fetchProjects();
      Navigator.of(context).pop();
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to delete project: $e';
      });
    }
  }

  void _redirectToChat(Project project) {
    Navigator.pushNamed(
      context,
      'chat',
      arguments: project,
    );
  }

  void _selectProject(Project project) {
    setState(() {
      _selectedProject = project;
    });
    //_redirectToChat(project);
  }

  void gotoSettings(Project project) {
    _selectProject(project);

    Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => ProjectDetail(
              projectId: project.projectId,
              isEditing: true,
              onSave: _fetchProjects),
        ));
  }

  void _handleProjectAction(String action, Project project) {
    switch (action) {
      case 'edit':
        gotoSettings(project);
        break;

      case 'refresh':
        showDialog(
          context: context,
          builder: (BuildContext context) {
            return AlertDialog(
              title: Text('Not Implemented'),
              content: Text('The refresh feature is not implemented yet.'),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: Text('OK'),
                ),
              ],
            );
          },
        );
      case 'delete':
        int index = _projects.indexOf(project);
        if (index != -1) {
          _handleDelete(project, index);
        }
        break;
    }
  }

  Future<void> _onSave() async {
    _fetchProjects();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Projects'),
        actions: [
          TextButton.icon(
            icon: Icon(Icons.add),
            label: Text('New Project'),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) =>
                      ProjectDetail(isEditing: false, onSave: _onSave),
                ),
              );
            },
          ),
        ],
      ),
      body: Padding(
        padding: EdgeInsets.all(20.0),
        child: _projects.isEmpty
            ? Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.info_outline, size: 48, color: Colors.grey),
                    SizedBox(height: 16),
                    Text(
                      'No projects found.',
                      style: TextStyle(
                        fontSize: 18,
                        color: Colors.grey,
                      ),
                    ),
                    SizedBox(height: 8),
                    Text(
                      'Click the "New Project" button above to add your first project.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 16,
                        color: Colors.grey[600],
                      ),
                    ),
                  ],
                ),
              )
            : Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: ListView.builder(
                      itemCount: _projects.length,
                      itemBuilder: (context, index) {
                        final project = _projects[index];
                        return MouseRegion(
                          onEnter: (_) => setState(() => _hoveredIndex = index),
                          onExit: (_) => setState(() => _hoveredIndex = null),
                          child: Container(
                            decoration: BoxDecoration(
                              color: _hoveredIndex == index
                                  ? Theme.of(context)
                                      .colorScheme
                                      .primaryContainer
                                  : null,
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: ListTile(
                              title: Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                mainAxisAlignment: MainAxisAlignment.start,
                                children: [
                                  Text(project.name),
                                  SizedBox(width: 10),
                                  StatusTag(
                                    label: project.model ?? 'No Model',
                                    color:
                                        languageModelColorMap[project.model] ??
                                            Colors.grey,
                                  ),
                                ],
                              ),
                              subtitle: Text(project.description),
                              onTap: () {
                                _selectProject(project);
                                _redirectToChat(project);
                              },
                              trailing: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  IconButton(
                                    icon: Icon(Icons.settings),
                                    onPressed: () => gotoSettings(project),
                                  ),
                                  PopupMenuButton<String>(
                                    padding: EdgeInsets.zero,
                                    icon: Icon(
                                      Icons.more_vert,
                                      size: 16,
                                    ),
                                    iconSize: 16,
                                    tooltip: 'Project options',
                                    onSelected: (value) =>
                                        _handleProjectAction(value, project),
                                    itemBuilder: (BuildContext context) =>
                                        <PopupMenuEntry<String>>[
                                      const PopupMenuItem<String>(
                                        value: 'refresh',
                                        child: ListTile(
                                          leading: Icon(Icons.refresh),
                                          title: Text('Refresh'),
                                          dense: true,
                                        ),
                                      ),
                                      const PopupMenuDivider(),
                                      const PopupMenuItem<String>(
                                        value: 'delete',
                                        child: ListTile(
                                          leading: Icon(Icons.delete,
                                              color: Colors.red),
                                          title: Text('Delete',
                                              style:
                                                  TextStyle(color: Colors.red)),
                                          dense: true,
                                        ),
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}
