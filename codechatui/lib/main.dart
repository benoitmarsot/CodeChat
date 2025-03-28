import 'package:codechatui/src/models/project.dart';
import 'package:codechatui/src/services/secure_storage.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'src/login_page.dart';
import 'src/services/auth_provider.dart';
import 'src/main_page.dart';
import 'package:codechatui/src/chat_page.dart';

void main() async {
  WidgetsFlutterBinding
      .ensureInitialized(); // Required when using async in main

  final secureStorage = SecureStorageService();
  final token = await secureStorage.getToken();
  final userId = await secureStorage.getUserId();

  runApp(
    ChangeNotifierProvider(
      create: (_) {
        final authProvider = AuthProvider();
        // Initialize AuthProvider with stored values
        if (token != null) authProvider.setToken(token);
        if (userId != null) authProvider.setUserId(userId);
        return authProvider;
      },
      child: const App(),
    ),
  );
}

class App extends StatefulWidget {
  const App({super.key});

  @override
  State<App> createState() => _AppState();
}

class _AppState extends State<App> {
  bool isDarkMode = true;

  @override
  Widget build(BuildContext context) {
    void onThemeToggle() {
      setState(() => isDarkMode = !isDarkMode);
    }

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => MainPageState()),
      ],
      child: MaterialApp(
        title: 'Codechat',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          useMaterial3: true,
          brightness: Brightness.light,
          colorScheme: ColorScheme.fromSeed(
            seedColor: Color(0xFF1E4396), // Set primary color here
            brightness: Brightness.light,
          ),
          scaffoldBackgroundColor: Colors.white,
        ),
        darkTheme: ThemeData(
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(
            seedColor: Color(0xFF1E4396), // Set primary color here
            brightness: Brightness.dark,
          ),
          scaffoldBackgroundColor: Colors.black,
        ),
        themeMode:
            isDarkMode ? ThemeMode.dark : ThemeMode.light, // Conditional theme
        home: authProvider.isAuthenticated
            ? MainPage(isDarkMode: false, onThemeToggle: onThemeToggle)
            : LoginPage(),
        onGenerateRoute: (settings) {
          return MaterialPageRoute(
            settings: settings,
            builder: (context) {
              if (!authProvider.isAuthenticated) {
                return LoginPage();
              }

              switch (settings.name) {
                // cxase '/register':
                //   return RegisterPage();

                case 'chat':
                  final args = ModalRoute.of(context)?.settings.arguments;

                  // Safe handling of potentially null arguments
                  if (args is Project) {
                    return ChatPage(
                        project: args, onThemeToggle: onThemeToggle);
                  }
                  // Otherwise go back to home
                  return MainPage(
                      isDarkMode: isDarkMode, onThemeToggle: onThemeToggle);
                case '':
                default:
                  return MainPage(
                      isDarkMode: false, onThemeToggle: onThemeToggle);
              }
            },
          );
        },
      ),
    );
  }
}
