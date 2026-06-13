import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/api_client.dart';
import '../core/app_state.dart';
import '../widgets/common.dart';

/// 注册页：邮箱 + 验证码（dev 打后端控制台）+ 密码 + 昵称。
/// 成功后自动登录并 pop(true)。
class RegisterPage extends StatefulWidget {
  const RegisterPage({super.key});

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage> {
  final _email = TextEditingController();
  final _code = TextEditingController();
  final _password = TextEditingController();
  final _nickname = TextEditingController();
  bool _loading = false;
  bool _sending = false;
  int _countdown = 0;

  @override
  void dispose() {
    _email.dispose();
    _code.dispose();
    _password.dispose();
    _nickname.dispose();
    super.dispose();
  }

  Future<void> _sendCode() async {
    final app = context.read<AppState>();
    if (_email.text.trim().isEmpty) {
      showToast(context, app.t('email'));
      return;
    }
    setState(() => _sending = true);
    try {
      await AuthService.sendEmailCode(_email.text.trim());
      if (!mounted) return;
      showToast(context, app.t('code_hint'));
      setState(() => _countdown = 60);
      _tick();
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  void _tick() {
    Future.delayed(const Duration(seconds: 1), () {
      if (!mounted) return;
      if (_countdown > 0) {
        setState(() => _countdown--);
        _tick();
      }
    });
  }

  Future<void> _register() async {
    final app = context.read<AppState>();
    if (_email.text.trim().isEmpty ||
        _code.text.trim().isEmpty ||
        _password.text.isEmpty) {
      showToast(context, app.t('required_tip'));
      return;
    }
    setState(() => _loading = true);
    try {
      await AuthService.register(
        email: _email.text.trim(),
        code: _code.text.trim(),
        password: _password.text,
        nickname: _nickname.text.trim().isEmpty ? _email.text.split('@').first : _nickname.text.trim(),
      );
      // 注册后自动登录
      final data = await AuthService.login(_email.text.trim(), _password.text);
      final token = data['token']?.toString() ?? '';
      final user = data['user'] is Map ? (data['user'] as Map).cast<String, dynamic>() : null;
      await app.setSession(token, user);
      if (!mounted) return;
      showToast(context, app.t('success'));
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
      appBar: AppBar(title: Text(app.t('register'))),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
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
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _code,
                  keyboardType: TextInputType.number,
                  decoration: InputDecoration(
                    labelText: app.t('verify_code'),
                    prefixIcon: const Icon(Icons.pin_outlined),
                    border: const OutlineInputBorder(),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              SizedBox(
                height: 56,
                child: OutlinedButton(
                  onPressed: (_sending || _countdown > 0) ? null : _sendCode,
                  child: Text(_countdown > 0 ? '${_countdown}s' : app.t('send_code')),
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(app.t('code_hint'), style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
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
          const SizedBox(height: 16),
          TextField(
            controller: _nickname,
            decoration: InputDecoration(
              labelText: app.t('nickname'),
              prefixIcon: const Icon(Icons.person_outline),
              border: const OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 24),
          SizedBox(
            height: 48,
            child: FilledButton(
              onPressed: _loading ? null : _register,
              child: _loading
                  ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : Text(app.t('register_now')),
            ),
          ),
        ],
      ),
    );
  }
}
