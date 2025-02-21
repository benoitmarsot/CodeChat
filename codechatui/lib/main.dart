import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'src/login.dart';
import 'src/main_page.dart';

void main() {
  runApp(const MyApp());
}
class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => MyAppState()),
      ],
      child: MaterialApp(
        title: 'Namer App',
        theme: ThemeData(
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
        ),
        home: const Login(),
        initialRoute: '', 
        routes: {
          '': (context) => const Login(),
          'home': (context) => MainPage(),
        },
      ),
    );
  }
}

