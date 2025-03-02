import 'package:codechatui/src/services/auth_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';  // Add this import for keyboard keys
import 'package:codechatui/src/models/project.dart';
import 'package:codechatui/src/models/message.dart';
import 'package:codechatui/src/models/discussion.dart';
import 'package:codechatui/src/services/discussion_service.dart';
import 'package:codechatui/src/widgets/ai_response_widget.dart';
import 'package:codechatui/src/widgets/user_message_bubble.dart';
import 'package:provider/provider.dart';

class ChatPage extends StatefulWidget {
  final Project project;

  const ChatPage({super.key, required this.project});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> with SingleTickerProviderStateMixin {
  late AuthProvider authProvider;
  late TabController _tabController;
  late DiscussionService _discussionService;
  final TextEditingController _messageController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final FocusNode _messageFocusNode = FocusNode();  // Add this for keyboard handling
  final List<Message> _messages = [];
  
  // Panel control variables
  double _leftPanelWidth = 250.0; // Default width
  bool _isLeftPanelVisible = true;
  
  // Discussion tracking
  int _selectedDiscussionId = 0; // 0 means no discussion selected
  List<Discussion> _discussions = [];
   
  bool _isLoading = false;
  
  @override
  void initState() {
    super.initState();
     authProvider = Provider.of<AuthProvider>(context, listen: false);
    _tabController = TabController(length: 2, vsync: this);
    _discussionService = DiscussionService(authProvider: authProvider);
    
    // Method 1: Using addPostFrameCallback (recommended)
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadDiscussions();
    });
    
    // Alternative method 2: Using Future.microtask
    // Future.microtask(() {
    //   _loadDiscussions();
    // });
  }

  @override
  void dispose() {
    _tabController.dispose();
    _messageController.dispose();
    _scrollController.dispose();
    _messageFocusNode.dispose();  // Don't forget to dispose the focus node
    super.dispose();
  }

  // Load discussions for the current project
  Future<void> _loadDiscussions() async {
    setState(() {
      _isLoading = true;
    });
    
    try {
      final discussions = await _discussionService.getDiscussionsByProject(widget.project.projectId);
      setState(() {
        _discussions = discussions;
      });
    } catch (e) {
      // Show error to user
      if(mounted) {
        print("Error loading discussions: $e");
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to load discussions: $e'))
        );
      }
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }
  
  // Load messages for a specific discussion
  Future<void> _selectDiscussion(int discussionId) async {
    if (_selectedDiscussionId == discussionId) return;
    
    setState(() {
      _isLoading = true;
      _messages.clear();
    });
    
    try {
      final messages = await _discussionService.getDiscussionMessages(discussionId);
      setState(() {
        _messages.addAll(messages);
        _selectedDiscussionId = discussionId;
      });
      
      // Scroll to bottom after loading messages
      _scrollToBottom();
    } catch (e) {
      if(mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to load messages: $e'))
        );
      }
      setState(() {
        _selectedDiscussionId = 0;
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _sendMessage() async {
    if (_messageController.text.trim().isEmpty) return;
    if(_selectedDiscussionId == 0) {
      Discussion newDiscussion = await _discussionService.createDiscussion(
          widget.project.projectId, 'Discussion ${_discussions.length + 1}'
      );
      _selectedDiscussionId = newDiscussion.did;
      await _loadDiscussions();
    }
    
    setState(() {
      _messages.add(Message(
        discussionId:_selectedDiscussionId,
        role: "user",
        text: _messageController.text,


      ));
      
      // Simulate AI response (in a real app, you would call your API here)
      _messages.add(Message(
        discussionId: _selectedDiscussionId,
        role: "assistant",
        text: '{"question": "${_messageController.text}", "answers": [{"explanation": "This is a sample response", "language": "java", "code": "print(\\"Hello World\\");", "references": ["Flutter docs"]}]}',
      ));
    });
    
    _messageController.clear();
    _scrollToBottom();
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

  void _toggleLeftPanel() {
    setState(() {
      _isLeftPanelVisible = !_isLeftPanelVisible;
    });
  }
  String minPanelEmptyMessage() => 'Start a new conversation${_discussions.isEmpty ? ' or select one from history' : ''}';

  // Handle key events for message input
  KeyEventResult _handleKeyEvent(FocusNode node, KeyEvent event) {
    if (event is KeyDownEvent && event.logicalKey == LogicalKeyboardKey.enter) {
      if (HardwareKeyboard.instance.isShiftPressed) {
        // Insert a new line at cursor position when shift+Enter is pressed
        final text = _messageController.text;
        final selection = _messageController.selection;
        final newText = '${text.substring(0, selection.start)}\n${text.substring(selection.end)}';
        
        _messageController.value = TextEditingValue(
          text: newText,
          selection: TextSelection.collapsed(
            offset: selection.start + 1,
          ),
        );
        return KeyEventResult.handled;
      } else if (!HardwareKeyboard.instance.isShiftPressed) {
        // Submit message on plain Enter (not handling shift+enter for now)
        // Call async method to send message and do not wait for it
        _sendMessage();
        return KeyEventResult.handled;  
      }
    }
    return KeyEventResult.ignored;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.project.name),
        leading: IconButton(
          icon: Icon(_isLeftPanelVisible ? Icons.menu_open : Icons.menu),
          onPressed: _toggleLeftPanel,
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.home),
            onPressed: () {
              Navigator.of(context).pop(); // Navigate back to home page
            },
            tooltip: 'Back to Home',
          ),
        ],
      ),
      body: Row(
        children: [
          // Left side - Tab controller for historical discussions
          if (_isLeftPanelVisible)
            SizedBox(
              width: _leftPanelWidth,
              child: Column(
                children: [
                  TabBar(
                    controller: _tabController,
                    tabs: const [
                      Tab(text: 'History'),
                      Tab(
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text('Favorites'),
                            SizedBox(width: 4),
                            Tooltip(
                              message: 'Favorite discussions remain saved beyond 30 days.',
                              child: Icon(Icons.info_outline, size: 14),
                            ),
                          ],
                        ),
                      ),
                    ],
                    labelColor: Theme.of(context).primaryColor,
                  ),
                  Expanded(
                    child: TabBarView(
                      controller: _tabController,
                      children: [
                        // History tab with actual discussions
                        _isLoading 
                          ? const Center(child: CircularProgressIndicator())
                          : Column(
                              children: [
                                // New Discussion button
                                if (_selectedDiscussionId != 0)
                                  ListTile(
                                    leading: const Icon(Icons.add_comment),
                                    title: const Text('New Discussion'),
                                    tileColor: _selectedDiscussionId == 0 
                                        ? Theme.of(context).colorScheme.primaryContainer
                                        : null,
                                    onTap: () {
                                      setState(() {
                                        _selectedDiscussionId = 0;
                                        _messages.clear();
                                      });
                                    },
                                  ),
                                if (_selectedDiscussionId != 0)
                                  const Divider(),
                                // Existing discussions list
                                Expanded(
                                  child: _discussions.isEmpty
                                    ? const Center(child: Text('No discussions found'))
                                    : getDiscussionsList(),
                                ),
                              ],
                            ),

                        // Favorites tab - similarly
                        _isLoading
                          ? const Center(child: CircularProgressIndicator())
                          : Column(
                              children: [
                                // New Discussion button
                                if (_selectedDiscussionId != 0)
                                  ListTile(
                                    leading: const Icon(Icons.add_comment),
                                    title: const Text('New Discussion'),
                                    tileColor: _selectedDiscussionId == 0 
                                        ? Theme.of(context).colorScheme.primaryContainer
                                        : null,
                                    onTap: () {
                                      setState(() {
                                        _selectedDiscussionId = 0;
                                        _messages.clear();
                                      });
                                    },
                                  ),
                                if (_selectedDiscussionId != 0)
                                  const Divider(),
                                // Existing favorites list
                                Expanded(
                                  child: ListView.builder(
                                    itemCount: _discussions.where((d) => d.isFavorite).length,
                                    itemBuilder: (context, index) {
                                      final favorites = _discussions.where((d) => d.isFavorite).toList();
                                      return Tooltip(
                                        message: 'Created: ${_formatDate(favorites[index].created)}\n'
                                                '${favorites[index].name}',
                                        preferBelow: false,
                                        verticalOffset: 20,
                                        child: ListTile(
                                          title: Text(favorites[index].name),
                                          subtitle: Text(_formatDate(favorites[index].created)),
                                          selected: _selectedDiscussionId == favorites[index].did,
                                          onTap: () => _selectDiscussion(favorites[index].did),
                                        ),
                                      );
                                    },
                                  ),
                                ),
                              ],
                            ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          
          // Resizable divider
          if (_isLeftPanelVisible)
            GestureDetector(
              behavior: HitTestBehavior.translucent,
              onHorizontalDragUpdate: (details) {
                setState(() {
                  _leftPanelWidth += details.delta.dx;
                  // Enforce minimum and maximum width constraints
                  _leftPanelWidth = _leftPanelWidth.clamp(150.0, MediaQuery.of(context).size.width * 0.5);
                });
              },
              child: MouseRegion(
                cursor: SystemMouseCursors.resizeLeftRight,
                child: Container(
                  width: 8,
                  height: double.infinity,
                  color: Colors.grey[300],
                  child: const Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.drag_indicator, size: 16),
                        SizedBox(height: 8),
                        Icon(Icons.drag_indicator, size: 16),
                        SizedBox(height: 8),
                        Icon(Icons.drag_indicator, size: 16),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          
          // Right side - Chat area
          Expanded(
            child: Column(
              children: [
                // Chat messages area with loading indicator
                Expanded(
                  child: _isLoading
                    ? const Center(child: CircularProgressIndicator())
                    : _messages.isEmpty
                        ? Center(child: Text(
                            'Start a new conversation${_discussions.isNotEmpty ? ' or select one from history' : ''}'
                          ))
                        : getDiscussionsList(),
                ),
                // Input area
                Container(
                  padding: const EdgeInsets.all(8.0),
                  decoration: BoxDecoration(
                    color: Colors.grey[200],
                    border: Border(
                      top: BorderSide(color: Colors.grey[300]!),
                    ),
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _messageController,
                          focusNode: _messageFocusNode..onKeyEvent = _handleKeyEvent,
                          keyboardType: TextInputType.multiline,
                          decoration: const InputDecoration(
                            hintText: 'Type your message...',
                            border: OutlineInputBorder(),
                            // helperText: 'Press Enter to send, Shift+Enter for new line',
                          ),
                          minLines: 1,
                          maxLines: 20,
                          // Using focusNode.onKeyEvent to handle keyboard events
                        ),
                      ),
                      const SizedBox(width: 8),
                      IconButton(
                        icon: const Icon(Icons.send),
                        onPressed: _sendMessage,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // Add this helper method to format dates nicely
  String _formatDate(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')} '
        '${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';
  }
  
  ListView getDiscussionsList() {
    return ListView.builder(
      itemCount: _discussions.length,
      itemBuilder: (context, index) {
        final discussion = _discussions[index];
        return Tooltip(
          message: 'Created: ${_formatDate(discussion.created)}\n'
                  '${discussion.description.isNotEmpty ? discussion.description : discussion.name}',
          preferBelow: false,
          verticalOffset: 20,
          child: ListTile(
            title: Text(discussion.name),
            subtitle: discussion.description.isNotEmpty 
                ? Text(discussion.description, 
                    maxLines: 1, overflow: TextOverflow.ellipsis)
                : null,
            selected: _selectedDiscussionId == discussion.did,
            onTap: () => _selectDiscussion(discussion.did),
            trailing: PopupMenuButton<String>(
              tooltip: 'Discussion options',
              onSelected: (value) => _handleDiscussionAction(value, discussion),
              itemBuilder: (BuildContext context) => <PopupMenuEntry<String>>[
                const PopupMenuItem<String>(
                  value: 'suggest_name',
                  child: ListTile(
                    leading: Icon(Icons.auto_awesome),
                    title: Text('Suggest name'),
                    dense: true,
                  ),
                ),
                const PopupMenuItem<String>(
                  value: 'rename',
                  child: ListTile(
                    leading: Icon(Icons.edit),
                    title: Text('Rename'),
                    dense: true,
                  ),
                ),
                const PopupMenuItem<String>(
                  value: 'description',
                  child: ListTile(
                    leading: Icon(Icons.description),
                    title: Text('Add/edit description'),
                    dense: true,
                  ),
                ),
                const PopupMenuDivider(),
                const PopupMenuItem<String>(
                  value: 'delete',
                  child: ListTile(
                    leading: Icon(Icons.delete, color: Colors.red),
                    title: Text('Delete', style: TextStyle(color: Colors.red)),
                    dense: true,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
  
  // Handler for discussion context menu actions
  void _handleDiscussionAction(String action, Discussion discussion) async {
    switch (action) {
      case 'suggest_name':
        _suggestDiscussionName(discussion);
      case 'rename':
        _renameDiscussion(discussion);
      case 'description':
        _editDiscussionDescription(discussion);
      case 'delete':
        _deleteDiscussion(discussion);
    }
  }

  // Method to suggest a name using AI
  Future<void> _suggestDiscussionName(Discussion discussion) async {
    // Show loading dialog
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return const AlertDialog(
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              CircularProgressIndicator(),
              SizedBox(height: 16),
              Text("Asking AI to suggest a name..."),
            ],
          ),
        );
      },
    );
    
    try {
      // In a real app, you would call your backend AI service here
      await Future.delayed(const Duration(seconds: 2)); // Simulating API call
      
      // Close loading dialog
      if (mounted) Navigator.of(context).pop();
      
      // Show suggestion dialog with mock response
      if (mounted) {
        final suggestedName = "Chat about ${widget.project.name} architecture";
        
        showDialog(
          context: context,
          builder: (BuildContext context) {
            return AlertDialog(
              title: const Text("Suggested Name"),
              content: Text("AI suggests: \"$suggestedName\""),
              actions: [
                TextButton(
                  onPressed: () {
                    Navigator.pop(context);
                  },
                  child: const Text("Cancel"),
                ),
                TextButton(
                  onPressed: () {
                    Navigator.pop(context);
                    // Apply the suggestion
                    // In a real app, you would update the discussion name via API
                    setState(() {
                      // Here you would make an API call to update the name
                    });
                  },
                  child: const Text("Apply"),
                ),
              ],
            );
          },
        );
      }
    } catch (e) {
      // Close loading dialog if error occurs
      if (mounted) {
        // Close loading dialog if error occurs
        Navigator.of(context).pop();
        // Show error message
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text("Failed to get name suggestion: $e")),
        );
      }
    }
  }

  // Method to rename a discussion
  Future<void> _renameDiscussion(Discussion discussion) async {
    final TextEditingController nameController = TextEditingController(text: discussion.name);
    
    await showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text("Rename Discussion"),
          content: TextField(
            controller: nameController,
            decoration: const InputDecoration(labelText: "New name"),
            autofocus: true,
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.pop(context);
              },
              child: const Text("Cancel"),
            ),
            TextButton(
              onPressed: () {
                Navigator.pop(context);
                // In a real app, you would update via API
                setState(() {
                  // Here you would make an API call to update the name
                });
              },
              child: const Text("Save"),
            ),
          ],
        );
      },
    );
    
    nameController.dispose();
  }

  // Method to edit discussion description
  Future<void> _editDiscussionDescription(Discussion discussion) async {
    final TextEditingController descController = TextEditingController(text: discussion.description);
    
    await showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text("Edit Description"),
          content: TextField(
            controller: descController,
            decoration: const InputDecoration(labelText: "Description"),
            maxLines: 3,
            autofocus: true,
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.pop(context);
              },
              child: const Text("Cancel"),
            ),
            TextButton(
              onPressed: () {
                Navigator.pop(context);
                // In a real app, you would update via API
                setState(() {
                  // Here you would make an API call to update the description
                });
              },
              child: const Text("Save"),
            ),
          ],
        );
      },
    );
    
    descController.dispose();
  }

  // Method to delete a discussion
  Future<void> _deleteDiscussion(Discussion discussion) async {
    final bool confirm = await showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text("Delete Discussion"),
          content: const Text("Are you sure you want to delete this discussion? This cannot be undone."),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.pop(context, false);
              },
              child: const Text("Cancel"),
            ),
            TextButton(
              onPressed: () {
                Navigator.pop(context, true);
              },
              style: TextButton.styleFrom(foregroundColor: Colors.red),
              child: const Text("Delete"),
            ),
          ],
        );
      },
    ) ?? false;
    
    if (confirm) {
      // In a real app, you would delete via API
      setState(() {
        if (discussion.did == _selectedDiscussionId) {
          _selectedDiscussionId = 0;
          _messages.clear();
        }
        // Here you would make an API call to delete the discussion
      });
    }
  }
}
