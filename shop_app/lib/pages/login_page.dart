import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/api_client.dart';
import '../core/app_state.dart';
import '../widgets/common.dart';
import 'register_page.dart';

/// 登录页。成功后 pop(true)，调用方据此继续流程。
class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _email = TextEditingController(text: 'buyer2@kinn.dev');
  final _password = TextEditingController(text: 'Passw0rd!');
  bool _loading = false;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    final app = context.read<AppState>();
    if (_email.text.trim().isEmpty || _password.text.isEmpty) {
      showToast(context, app.t('required_tip'));
      return;
    }
    setState(() => _loading = true);
    try {
      final data = await AuthService.login(_email.text.trim(), _password.text);
      final token = data['token']?.toString() ?? '';
      final user = data['user'] is Map ? (data['user'] as Map).cast<String, dynamic>() : null;
      await app.setSession(token, user);
      // 同步购物车角标
      try {
        final c = await CartService.count();
        app.setCartCount(c);
      } catch (_) {}
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (_) {
      if (mounted) showToast(context, app.t('load_failed'));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(title: Text(app.t('login'))),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          const SizedBox(height: 24),
          Center(
            child: Text(app.t('app_title'),
                style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold)),
          ),
          const SizedBox(height: 40),
          TextField(
            controller: _email,
            keyboardType: TextInputType.emailAddress,
            decoration: InputDecoration(
              labelText: app.t('email'),
              prefixIcon: const Icon(Icons.email_outlined),
              border: const OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _password,
            obscureText: true,
            decoration: InputDecoration(
              labelText: app.t('password'),
              prefixIcon: const Icon(Icons.lock_outline),
              border: const OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 24),
          SizedBox(
            height: 48,
            child: FilledButton(
              onPressed: _loading ? null : _login,
              child: _loading
                  ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : Text(app.t('login_now')),
            ),
          ),
          const SizedBox(height: 12),
          TextButton(
            onPressed: () async {
              final nav = Navigator.of(context);
              final ok = await nav.push<bool>(
                MaterialPageRoute(builder: (_) => const RegisterPage()),
              );
              if (ok == true) nav.pop(true);
            },
            child: Text(app.t('no_account')),
          ),
        ],
      ),
    );
  }
}
