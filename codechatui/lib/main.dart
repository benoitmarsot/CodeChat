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
      child: const MyApp(),
    ),
  );
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool isDarkMode = true; // Track the theme mode

  @override
  Widget build(BuildContext context) {
    void _onThemeToggle() {
      setState(() => isDarkMode = !isDarkMode); // Toggle logic
    }

    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => MainPageState()),
      ],
      child: MaterialApp(
        title: 'Codechat',
        theme: ThemeData(
          useMaterial3: true,
          brightness: Brightness.light,
          colorScheme: ColorScheme.fromSeed(
            seedColor: Colors.blue, // Set primary color here
            brightness: Brightness.light,
          ),
          scaffoldBackgroundColor: Colors.white,
        ),
        darkTheme: ThemeData(
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(
            seedColor: Colors.blue, // Set primary color here
            brightness: Brightness.dark,
          ),
          scaffoldBackgroundColor: Colors.black,
        ),
        themeMode:
            isDarkMode ? ThemeMode.dark : ThemeMode.light, // Conditional theme
        home: const LoginPage(),

        initialRoute: '',
        onGenerateRoute: (settings) {
          return MaterialPageRoute(
            settings: settings,
            builder: (context) {
              final authProvider =
                  Provider.of<AuthProvider>(context, listen: false);
              final isAuthenticated = authProvider.isAuthenticated;

              switch (settings.name) {
                // case '/register':
                //   return RegisterPage();
                case 'main':
                  return isAuthenticated
                      ? MainPage(
                          isDarkMode: false, onThemeToggle: _onThemeToggle)
                      : LoginPage();
                case 'chat':
                  final args = ModalRoute.of(context)?.settings.arguments;

                  // Safe handling of potentially null arguments
                  if (args is Project) {
                    return ChatPage(
                        project: args, onThemeToggle: _onThemeToggle);
                  }
                  // Otherwise go back to home
                  return MainPage(
                      isDarkMode: isDarkMode, onThemeToggle: _onThemeToggle);
                case '':
                default:
                  return isAuthenticated
                      ? MainPage(
                          isDarkMode: false, onThemeToggle: _onThemeToggle)
                      : LoginPage();
              }
            },
          );
        },
      ),
    );
  }
}
