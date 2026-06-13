import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'core/app_state.dart';
import 'pages/main_shell.dart';

void main() {
  runApp(const ShopApp());
}

class ShopApp extends StatelessWidget {
  const ShopApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => AppState()..load(),
      child: Consumer<AppState>(
        builder: (context, app, _) {
          return MaterialApp(
            title: app.t('app_title'),
            debugShowCheckedModeBanner: false,
            theme: ThemeData(
              useMaterial3: true,
              colorScheme: ColorScheme.fromSeed(
                seedColor: const Color(0xFF1A1A1A),
                primary: const Color(0xFF1A1A1A),
                error: const Color(0xFFE60012),
              ),
              scaffoldBackgroundColor: const Color(0xFFF7F7F7),
              appBarTheme: const AppBarTheme(
                backgroundColor: Colors.white,
                foregroundColor: Color(0xFF1A1A1A),
                elevation: 0.5,
                centerTitle: true,
              ),
            ),
            home: app.ready
                ? const MainShell()
                : const Scaffold(body: Center(child: CircularProgressIndicator())),
          );
        },
      ),
    );
  }
}
