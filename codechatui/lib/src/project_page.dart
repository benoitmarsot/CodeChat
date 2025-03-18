import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/models/project.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'services/project_service.dart';
import 'services/codechat_service.dart';

class ProjectPage extends StatefulWidget {
  @override
  State<ProjectPage> createState() => _ProjectPageState();
}

class _ProjectPageState extends State<ProjectPage> with SingleTickerProviderStateMixin {
  String? _errorMessage;
  String _createdMessage = '';
  List<Project> _projects = [];
  int? _hoveredIndex;
  final TextEditingController _prjNameController = TextEditingController();
  final TextEditingController _prjDesctController = TextEditingController();
  final TextEditingController _prjRepoURLController = TextEditingController();
  final TextEditingController _branchNameController = TextEditingController(text: 'main');
  final TextEditingController _userNameController = TextEditingController();
  final TextEditingController _userPswController = TextEditingController();
  final TextEditingController _patController = TextEditingController();
  
  late TabController _tabController;

  int get _initialTabIndex => _projects.isNotEmpty ? 0 : 1;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    _fetchProjects();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _fetchProjects() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);

    try {
      final projects = await projectService.getAllProjects();
      setState(() {
        _projects = projects;
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to load projects: $e';
      });
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
      
      // Refresh projects and switch to projects tab
      await _fetchProjects();
      // Dismiss loading dialog
      if (mounted) {
        Navigator.of(context).pop();
      }

      _selectProject(project);
      //_tabController.animateTo(0);  // Switch to first tab
      
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to upload directory: $e';
      });
      print('Failed to upload directory: $e');
    } 
  }

  void _handleDrop(Object data) {
    // Implement a safe approach for web without relying on deprecated APIs.
    setState(() {
      _prjRepoURLController.text = "Folder path from data";
    });
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
    _redirectToChat(project);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final style = theme.textTheme.displayLarge!.copyWith(
      color: theme.colorScheme.primary,
    );
    return DefaultTabController(
      initialIndex: _initialTabIndex,
      length: 3,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Projects'),
          bottom: TabBar(
            controller: _tabController,  // Add controller here
            tabs: const <Widget>[
              Tab(icon: Icon(Icons.folder_open)),
              Tab(icon: Icon(Icons.drive_folder_upload_outlined)),
              Tab(icon: Icon(Icons.group_add)),
            ],
          ),
        ),
        body: TabBarView(
          controller: _tabController,  
          children: <Widget>[
            Card(
              margin: EdgeInsets.all(20),
              child: Padding(
                padding: EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Center(
                      child: Text('Open a project', style: TextStyle(fontSize: 20)),
                    ),
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
                                onTap: () => _selectProject(project),
                                trailing: IconButton(
                                  icon: Icon(Icons.delete),
                                  onPressed: () => _deleteProject(project, index),
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
            ),
            Card(
              margin: EdgeInsets.all(20),
              child: Padding(
                padding: EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Center(
                      child: Text('Create a project', style: TextStyle(fontSize: 20)),
                    ),
                    Spacer(),
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
                    const SizedBox(height: 16.0),
                    DragTarget<Object>(
                      onWillAcceptWithDetails: (data) => true,
                      onAcceptWithDetails: (details) {
                        _handleDrop(details.data);
                      },
                      builder: (context, candidateData, rejectedData) {
                        return Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              flex: 3,
                              child: TextField(
                                controller: _prjRepoURLController,
                                decoration: const InputDecoration(
                                  labelText: 'Git Repo URL (use the HTTPS link of your repo):',
                                ),
                              ),
                            ),
                            SizedBox(width: 12.0),
                            Expanded(
                              flex: 1,
                              child: TextField(
                                controller: _branchNameController,
                                decoration: const InputDecoration(
                                  labelText: 'Branch name:',
                                ),
                              ),
                            ),
                          ],
                        );
                      },
                    ),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          flex: 3,
                          child: TextField(
                            controller: _userNameController,
                            decoration: const InputDecoration(
                              labelText: 'Git user name (for private repo):',
                            ),
                          ),
                        ),
                        SizedBox(width: 12.0),
                        Expanded(
                          flex: 1,
                          child: TextField(
                            controller: _userPswController, 
                            decoration: const InputDecoration(
                              labelText: 'Password:',
                            ),
                          ),
                        ),
                      ],
                    ),
                    TextField(
                      controller: _patController,
                      decoration: const InputDecoration(
                        labelText: 'Or PAT (for private repo):',
                      ),
                    ),
                    if (_errorMessage != null)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 16.0),
                        child: Text(
                          _errorMessage!,
                          style: TextStyle(
                            color: theme.colorScheme.error,
                            fontSize: 14,
                          ),
                        ),
                      )
                    else if (_createdMessage.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 16.0),
                        child: Text(
                          _createdMessage,
                          style: TextStyle(
                            color: theme.colorScheme.onPrimary,
                            fontSize: 14,
                          ),
                        ),
                      ),
                    const SizedBox(height: 16.0),
                    ElevatedButton(
                      onPressed: _createdProject,
                      child: const Text('Create', style: TextStyle(fontSize: 20)),
                    ),
                    Spacer(),
                    Text(""),
                  ],
                ),
              ),
            ),
            Card(
              margin: EdgeInsets.all(20),
              child: Padding(
                padding: EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Center(
                      child: Text('Share your project', style: TextStyle(fontSize: 20)),
                    ),
                    SizedBox(height: 20),
                    Expanded(
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Left column - Invitation form
                          Expanded(
                            child: Padding(
                              padding: EdgeInsets.all(10),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text('Invite a user',
                                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                                  SizedBox(height: 16),
                                  TextField(
                                    decoration: InputDecoration(
                                      labelText: 'Email address',
                                      hintText: 'Enter user email',
                                    ),
                                  ),
                                  SizedBox(height: 16),
                                  ElevatedButton(
                                    onPressed: () {
                                      // TODO: Implement invite functionality
                                    },
                                    child: Text('Send Invite'),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          
                          // Center divider with debossed look
                          Container(
                            width: 1,
                            margin: EdgeInsets.symmetric(horizontal: 10),
                            decoration: BoxDecoration(
                              color: Colors.grey[300],
                              boxShadow: [
                                BoxShadow(
                                  color: Colors.white,
                                  offset: Offset(1, 0),
                                  blurRadius: 0,
                                ),
                              ],
                            ),
                          ),
                          
                          // Right column - User list
                          Expanded(
                            child: Padding(
                              padding: EdgeInsets.all(10),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text('Users with access',
                                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                                  SizedBox(height: 16),
                                  Expanded(
                                    child: ListView.builder(
                                      itemCount: 5, // Replace with actual users count
                                      itemBuilder: (context, index) {
                                        return ListTile(
                                          title: Text('User ${index + 1}'), // Replace with actual user name
                                          subtitle: Text('user${index + 1}@example.com'), // Replace with actual email
                                          trailing: IconButton(
                                            icon: Icon(Icons.remove_circle_outline),
                                            onPressed: () {
                                              // TODO: Implement remove user functionality
                                            },
                                          ),
                                        );
                                      },
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
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