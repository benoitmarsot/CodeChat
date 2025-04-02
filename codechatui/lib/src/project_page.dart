import 'package:codechatui/src/login_page.dart';
import 'package:codechatui/src/models/exceptions.dart';
import 'package:codechatui/src/models/project.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:flutter/material.dart';
import 'package:codechatui/src/widgets/project/project_detail.dart';
import 'package:provider/provider.dart';
import 'services/project_service.dart';
import 'package:codechatui/src/utils/error_handler.dart';

class ProjectPage extends StatefulWidget {
  @override
  State<ProjectPage> createState() => _ProjectPageState();
}

class _ProjectPageState extends State<ProjectPage>
    with SingleTickerProviderStateMixin {
  String _createdMessage = '';
  List<Project> _projects = [];
  int? _hoveredIndex;
  String? _errorMessage;
  bool _isEditing = false; // Track editing state
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
      final projects = await projectService.getAllProjects();

      setState(() {
        _projects = projects;
      });
      if (projects.length == 1) {
        _selectProject(projects[0]);
      }
    } on ForbiddenException catch (e) {
      // Handle 403 error
      ErrorHandler.handleForbiddenError(context, e.message);
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to load projects: $e';
      });
    }
  }

  // void _handleForbiddenError(String message) {
  //   // Log the user out and redirect to the login page
  //   final authProvider = Provider.of<AuthProvider>(context, listen: false);
  //   authProvider.clearAll();

  //   // Show a snackbar with the error message
  //   ScaffoldMessenger.of(context).showSnackBar(
  //     SnackBar(content: Text(message), backgroundColor: Colors.red),
  //   );

  //   // Redirect to the login page
  //   Navigator.of(context).pushReplacement(
  //     MaterialPageRoute(builder: (context) => const LoginPage()),
  //   );
  // }

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
      _isEditing = false; // Exit editing mode when selecting a new project
      // Populate the text controllers with the selected project's data
    });
    //_redirectToChat(project);
  }

  void _handleProjectAction(String action, Project project) {
    switch (action) {
      case 'edit':
        _selectProject(project);

        Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => ProjectDetail(
                  projectId: project.projectId,
                  isEditing: true,
                  onSave: _fetchProjects),
            ));

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
          _deleteProject(project, index);
        }
        break;
    }
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
                        ProjectDetail(isEditing: false, onSave: _fetchProjects),
                  ));
            },
          ),
        ],
      ),
      body: Padding(
        padding: EdgeInsets.all(20.0),
        child: Column(
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
                            ? Theme.of(context).colorScheme.primaryContainer
                            : null,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: ListTile(
                        title: Text(project.name),
                        subtitle: Text(project.description),
                        onTap: () {
                          _selectProject(project);
                          _redirectToChat(project);
                        },
                        trailing: PopupMenuButton<String>(
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
                              value: 'edit',
                              child: ListTile(
                                leading: Icon(Icons.edit),
                                title: Text('Edit'),
                                dense: true,
                              ),
                            ),
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
                                leading: Icon(Icons.delete, color: Colors.red),
                                title: Text('Delete',
                                    style: TextStyle(color: Colors.red)),
                                dense: true,
                              ),
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
