import 'package:codechatui/src/about_page.dart';
import 'package:codechatui/src/pricing_page.dart';
import 'package:codechatui/src/services/auth_provider.dart';
import 'package:codechatui/src/services/secure_storage.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:codechatui/src/project_page.dart';


class MainPageState extends ChangeNotifier {
}

class MainPage extends StatefulWidget {
  @override
  State<MainPage> createState() => _MainPage();
}

class _MainPage extends State<MainPage> {
  var selectedIndex = 0;
  Future<void> _handleLogout() async {
    final secureStorage = SecureStorageService();
    await secureStorage.clearAll();
    if (mounted) {
      Provider.of<AuthProvider>(context, listen: false).clearAll();
      Navigator.of(context).pushReplacementNamed('/');
    }
  }
  @override
  Widget build(BuildContext context) {
    Widget page;
    switch (selectedIndex) {
      case 0:
        page = ProjectPage();
      case 1:
        page = AboutPage();
      case 2:
        page = PricingPage();
      default:
        throw UnimplementedError('no widget for $selectedIndex');
    }
    return LayoutBuilder(
      builder: (context, constraints) {
        return Scaffold(
          body: Row(
            children: [
              SafeArea(
                child: NavigationRail(
                  extended: constraints.maxWidth > 600,
                  destinations: [
                    NavigationRailDestination(
                      icon: Icon(Icons.folder),
                      label: Text('Projects'),
                    ),
                    NavigationRailDestination(
                      icon: Icon(Icons.info_outline),
                      label: Text('About Codechat'),
                    ),
                    NavigationRailDestination(
                      icon: Icon(Icons.workspace_premium),
                      label: Text('Upgrade'),
                    ),
                    NavigationRailDestination(
                      icon: Icon(Icons.logout),
                      label: Text('Logout'),
                    ),
                  ],
                  selectedIndex: selectedIndex,
                  onDestinationSelected: (value) {
                    if (value == 3) {
                      _handleLogout();
                    } else {
                      setState(() {
                        selectedIndex = value;
                      });
                    }                  
                  },
                ),
              ),
              Expanded(
                child: Container(
                  color: Theme.of(context).colorScheme.primaryContainer,
                  child: page,
                ),
              ),
            ],
          ),
        );
      }
    );
  }
}