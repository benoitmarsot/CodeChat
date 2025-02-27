import 'package:codechatui/src/services/secure_storage.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'src/login_page.dart';
import 'src/services/auth_provider.dart';
import 'src/main_page.dart';

void main() async {  // Make main async
  WidgetsFlutterBinding.ensureInitialized();  // Required when using async in main
  
  // Get the stored token
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

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => MainPageState()),
      ],
      child: MaterialApp(
        title: 'Namer App',
        theme: ThemeData(
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
        ),
        home: const LoginPage(),
        initialRoute: '', 
        routes: {
          '': (context) => const LoginPage(),
          'home': (context) => MainPage(),
        },
      ),
    );
  }
}