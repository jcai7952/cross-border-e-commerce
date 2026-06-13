import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/api_client.dart';
import '../core/app_state.dart';
import '../models/address.dart';
import '../widgets/common.dart';

/// 实名认证管理页。
class IdentityPage extends StatefulWidget {
  const IdentityPage({super.key});

  @override
  State<IdentityPage> createState() => _IdentityPageState();
}

class _IdentityPageState extends State<IdentityPage> {
  List<Identity> _list = [];
  bool _loading = true;
  bool _error = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = false;
    });
    try {
      final list = await AddressService.identityList();
      if (!mounted) return;
      setState(() {
        _list = list;
        _loading = false;
      });
    } catch (_) {
      if (mounted) {
        setState(() {
        _loading = false;
        _error = true;
        });
      }
    }
  }

  Future<void> _openForm() async {
    final added = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) => const _IdentityForm(),
    );
    if (added == true) _load();
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(title: Text(app.t('identity_manage'))),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error
              ? ErrorView(message: app.t('load_failed'), onRetry: _load)
              : _list.isEmpty
                  ? EmptyView(
                      message: app.t('empty'),
                      icon: Icons.badge_outlined,
                      action: FilledButton(onPressed: _openForm, child: Text(app.t('add_address'))),
                    )
                  : ListView.separated(
                      padding: const EdgeInsets.all(12),
                      itemCount: _list.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 10),
                      itemBuilder: (ctx, i) {
                        final id = _list[i];
                        return Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8)),
                          child: Row(
                            children: [
                              const Icon(Icons.badge_outlined),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(id.realName, style: const TextStyle(fontWeight: FontWeight.bold)),
                                    Text(id.idCardMask, style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
                                  ],
                                ),
                              ),
                              if (id.verified)
                                const Icon(Icons.verified, color: Colors.green, size: 20),
                            ],
                          ),
                        );
                      },
                    ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openForm,
        icon: const Icon(Icons.add),
        label: Text(app.t('identity_manage')),
      ),
    );
  }
}

class _IdentityForm extends StatefulWidget {
  const _IdentityForm();

  @override
  State<_IdentityForm> createState() => _IdentityFormState();
}

class _IdentityFormState extends State<_IdentityForm> {
  final _name = TextEditingController();
  final _idCard = TextEditingController();
  bool _saving = false;

  @override
  void dispose() {
    _name.dispose();
    _idCard.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final app = context.read<AppState>();
    if (_name.text.trim().isEmpty || _idCard.text.trim().isEmpty) {
      showToast(context, app.t('required_tip'));
      return;
    }
    setState(() => _saving = true);
    try {
      await AddressService.addIdentity(_name.text.trim(), _idCard.text.trim());
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (_) {
      if (mounted) showToast(context, app.t('load_failed'));
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(app.t('identity_manage'), style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            TextField(
              controller: _name,
              decoration: InputDecoration(labelText: app.t('real_name'), border: const OutlineInputBorder(), isDense: true),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _idCard,
              decoration: InputDecoration(labelText: app.t('id_card'), border: const OutlineInputBorder(), isDense: true),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: _saving ? null : _save,
                child: _saving
                    ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                    : Text(app.t('save')),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
