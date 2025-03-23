import 'package:codechatui/src/config/app_config.dart';
import 'package:codechatui/src/models/project.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/widgets/choice-button.dart';
import 'package:codechatui/src/widgets/github_widget.dart';
import 'package:codechatui/src/widgets/web_widget.dart';
import 'package:codechatui/src/widgets/zip_widget.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'services/project_service.dart';
import 'services/codechat_service.dart';

enum DataSourceType { github, zip, web }

class ProjectPage extends StatefulWidget {
  @override
  State<ProjectPage> createState() => _ProjectPageState();
}

class _ProjectPageState extends State<ProjectPage>
    with SingleTickerProviderStateMixin {
  String? _errorMessage;
  DataSourceType _selectedDataSource = DataSourceType.github;

  String _createdMessage = '';
  List<Project> _projects = [];
  int? _hoveredIndex;
  final TextEditingController _prjNameController = TextEditingController();
  final TextEditingController _prjDesctController = TextEditingController();
  final TextEditingController _prjRepoURLController = TextEditingController();
  final TextEditingController _branchNameController =
      TextEditingController(text: 'main');
  final TextEditingController _userNameController = TextEditingController();
  final TextEditingController _userPswController = TextEditingController();
  final TextEditingController _patController = TextEditingController();

  late TabController _tabController;

  int _initialTabIndex = 0;
  bool _isEditing = false; // Track editing state
  Project? _selectedProject; // Store the selected project

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    _tabController.addListener(_handleTabChange); // Add listener
    _fetchProjects();
  }

  @override
  void dispose() {
    _tabController.removeListener(_handleTabChange); // Remove listener
    _tabController.dispose();
    super.dispose();
  }

  void _handleTabChange() {
    if (_tabController.indexIsChanging) {
      print('Tab changed to: ${_tabController.index}');
      if (_tabController.index != 1) _resetEditing();
      // Add any additional logic you want to handle on tab change
    }
  }

  Future<void> _fetchProjects() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);

    try {
      final projects = await projectService.getAllProjects();

      setState(() {
        _projects = projects;

        _tabController.animateTo(_projects.isEmpty
            ? 1
            : 0); // Default to create tab if no projects exist
      });
      if (projects.length == 1) {
        _selectProject(projects[0]);
      }
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

      // Dismiss loading dialog
      if (mounted) {
        Navigator.of(context).pop();
      }
      // Refresh projects and switch to projects tab
      await _fetchProjects();

      _selectProject(project);
      //_tabController.animateTo(0);  // Switch to first tab
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to upload directory: $e';
      });
      print('Failed to upload directory: $e');
    }
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
        _startEditing();

      case 'delete':
        int index = _projects.indexOf(project);
        if (index != -1) {
          _deleteProject(project, index);
        }
    }
  }

  void _startEditing() {
    _prjNameController.text = _selectedProject?.name ?? '';
    _prjDesctController.text = _selectedProject?.description ?? "";
    _tabController.animateTo(1);
    setState(() {
      _isEditing = true;
    });
  }

  void _resetEditing() {
    setState(() {
      _isEditing = false;
      _prjNameController.clear();
      _prjDesctController.clear();
    });
  }

  void _cancelEdit() {
    _resetEditing();
    _tabController.animateTo(0);
  }

  Future<void> _saveChanges() async {
    print('_saveChanges $_prjNameController.text');
    //if (_selectedProject == null) return;

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final projectService = ProjectService(authProvider: authProvider);

    try {
      // Update the project with the new values
      final updatedProject = _selectedProject!.copyWith(
        name: _prjNameController.text,
        description: _prjDesctController.text,
      );

      await projectService.updateProject(updatedProject);

      // Refresh the project list
      await _fetchProjects();

      setState(() {
        _isEditing = false;
        _createdMessage = 'Project updated successfully!';
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to update project: $e';
      });
      print('_errorMessage $_errorMessage');
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
          onFileSelected: (String? path) {
            print(path);
          },
        );
      case DataSourceType.web:
      default:
        return WebForm(
          prjRepoURLController: _prjRepoURLController,
          userNameController: _userNameController,
          userPswController: _userPswController,
        );
    }
  }

  @override
  Widget build(BuildContext context) {
    //final theme = Theme.of(context);

    return DefaultTabController(
      initialIndex: _initialTabIndex,
      length: 3,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Projects'),
          bottom: TabBar(
            controller: _tabController, // Add controller here
            tabs: const <Widget>[
              Tooltip(
                message: 'Project List',
                child: Tab(icon: Icon(Icons.folder_open)),
              ),
              Tooltip(
                  message: 'Create Project',
                  child: Tab(icon: Icon(Icons.drive_folder_upload_outlined))),
              Tooltip(message: 'Share', child: Tab(icon: Icon(Icons.group_add)))
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
                      child: Text('Open a project',
                          style: TextStyle(fontSize: 20)),
                    ),
                    Expanded(
                      child: ListView.builder(
                        itemCount: _projects.length,
                        itemBuilder: (context, index) {
                          final project = _projects[index];
                          return MouseRegion(
                            onEnter: (_) =>
                                setState(() => _hoveredIndex = index),
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
                                // IconButton(
                                //   icon: Icon(Icons.delete),
                                //   onPressed: () =>
                                //       _deleteProject(project, index),
                                // ),
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
                    Text(_isEditing ? 'Edit Project' : 'Create a project',
                        style: TextStyle(fontSize: 20)),
                    FractionallySizedBox(
                      widthFactor: 0.8,
                      child: Column(
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
                            margin:
                                const EdgeInsets.only(bottom: 16.0, top: 16.0),
                            child: Row(children: [
                              Text('Define Data Source',
                                  style: TextStyle(
                                      fontSize: 20,
                                      fontWeight: FontWeight.bold)),
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
                                      _selectedDataSource =
                                          DataSourceType.github;
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
                                // Expanded(
                                //   child: ImageRadioListTile(
                                //     title: const Text('GitHub Repo'),
                                //     image: Image.asset(
                                //         'assets/github_logo.png',
                                //         width: 40),
                                //     value: DataSourceType.github,
                                //     groupValue: _selectedDataSource,
                                //     onChanged: (DataSourceType? value) {
                                //       setState(() {
                                //         _selectedDataSource = value!;
                                //       });
                                //     },
                                //   ),
                                // ),
                                // Expanded(
                                //   child: ImageRadioListTile(
                                //     title: const Text('Zip File'),
                                //     image: Image.asset('assets/zip_logo.png',
                                //         width: 40),
                                //     value: DataSourceType.zip,
                                //     groupValue: _selectedDataSource,
                                //     onChanged: (DataSourceType? value) {
                                //       setState(() {
                                //         _selectedDataSource = value!;
                                //       });
                                //     },
                                //   ),
                                // ),
                                // Expanded(
                                //   child: ImageRadioListTile(
                                //     title: const Text('Web URL'),
                                //     image: Image.asset('assets/web_logo2.png',
                                //         width: 40),
                                //     value: DataSourceType.web,
                                //     groupValue: _selectedDataSource,
                                //     onChanged: (DataSourceType? value) {
                                //       setState(() {
                                //         _selectedDataSource = value!;
                                //       });
                                //     },
                                //   ),
                                // ),
                              ],
                            ),
                          ),
                          // Conditional Input Fields based on selected data source

                          _buildDataSourceForm(),

                          Align(
                            alignment: Alignment.bottomRight,
                            child: (_isEditing
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
                                      // ElevatedButton(
                                      //   onPressed: () {
                                      //     print('_saveChanges');
                                      //     _saveChanges();
                                      //   },
                                      //   style: ElevatedButton.styleFrom(
                                      //     textStyle: TextStyle(fontSize: 20),
                                      //   ),
                                      //   child: Text('Apply'),
                                      // ),
                                    ],
                                  )
                                : ElevatedButton(
                                    onPressed: _createdProject,
                                    style: ElevatedButton.styleFrom(
                                      textStyle: TextStyle(fontSize: 20),
                                    ),
                                    child: Text('Create'),
                                  )),
                          ),
                        ],
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
                      child: Text('Share your project',
                          style: TextStyle(fontSize: 20)),
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
                                      style: TextStyle(
                                          fontSize: 16,
                                          fontWeight: FontWeight.bold)),
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
                                      style: TextStyle(
                                          fontSize: 16,
                                          fontWeight: FontWeight.bold)),
                                  SizedBox(height: 16),
                                  Expanded(
                                    child: ListView.builder(
                                      itemCount:
                                          5, // Replace with actual users count
                                      itemBuilder: (context, index) {
                                        return ListTile(
                                          title: Text(
                                              'User ${index + 1}'), // Replace with actual user name
                                          subtitle: Text(
                                              'user${index + 1}@example.com'), // Replace with actual email
                                          trailing: IconButton(
                                            icon: Icon(
                                                Icons.remove_circle_outline),
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

// class ImageRadioListTile<T> extends StatelessWidget {
//   final Widget title;
//   final Widget image;
//   final T value;
//   final T? groupValue;
//   final ValueChanged<T?>? onChanged;

//   const ImageRadioListTile({
//     Key? key,
//     required this.title,
//     required this.image,
//     required this.value,
//     required this.groupValue,
//     required this.onChanged,
//   }) : super(key: key);

//   @override
//   Widget build(BuildContext context) {
//     return InkWell(
//       onTap: () {
//         if (onChanged != null) {
//           onChanged!(value);
//         }
//       },
//       child: Row(
//         children: [
//           Radio<T>(
//             value: value,
//             groupValue: groupValue,
//             onChanged: onChanged,
//           ),
//           image,
//           const SizedBox(width: 8),
//           title,
//         ],
//       ),
//     );
//   }
// }
