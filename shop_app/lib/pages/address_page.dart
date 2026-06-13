import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/api_client.dart';
import '../core/app_state.dart';
import '../models/address.dart';
import '../widgets/common.dart';

/// 地址管理页。selectable=true 时点击地址返回所选地址(pop Address)。
class AddressPage extends StatefulWidget {
  final bool selectable;
  const AddressPage({super.key, this.selectable = false});

  @override
  State<AddressPage> createState() => _AddressPageState();
}

class _AddressPageState extends State<AddressPage> {
  List<Address> _list = [];
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
      final list = await AddressService.list();
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

  Future<void> _openAddForm() async {
    final added = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) => const _AddressForm(),
    );
    if (added == true) _load();
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(title: Text(app.t('address_manage'))),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error
              ? ErrorView(message: app.t('load_failed'), onRetry: _load)
              : _list.isEmpty
                  ? EmptyView(
                      message: app.t('no_address'),
                      icon: Icons.location_off_outlined,
                      action: FilledButton(onPressed: _openAddForm, child: Text(app.t('add_address'))),
                    )
                  : ListView.separated(
                      padding: const EdgeInsets.all(12),
                      itemCount: _list.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 10),
                      itemBuilder: (ctx, i) => _addressTile(app, _list[i]),
                    ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openAddForm,
        icon: const Icon(Icons.add),
        label: Text(app.t('add_address')),
      ),
    );
  }

  Widget _addressTile(AppState app, Address a) {
    return InkWell(
      onTap: widget.selectable ? () => Navigator.of(context).pop(a) : null,
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8)),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(a.receiverName, style: const TextStyle(fontWeight: FontWeight.bold)),
                      const SizedBox(width: 8),
                      Text(a.phone, style: TextStyle(color: Colors.grey.shade600)),
                      if (a.isDefault) ...[
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                          decoration: BoxDecoration(
                              color: Theme.of(context).colorScheme.error,
                              borderRadius: BorderRadius.circular(3)),
                          child: Text(app.t('default_tag'),
                              style: const TextStyle(color: Colors.white, fontSize: 10)),
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(a.fullAddress, style: const TextStyle(fontSize: 13)),
                ],
              ),
            ),
            if (widget.selectable) const Icon(Icons.chevron_right),
          ],
        ),
      ),
    );
  }
}

/// 新增地址表单（底部弹窗）。
class _AddressForm extends StatefulWidget {
  const _AddressForm();

  @override
  State<_AddressForm> createState() => _AddressFormState();
}

class _AddressFormState extends State<_AddressForm> {
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _state = TextEditingController();
  final _city = TextEditingController();
  final _line1 = TextEditingController();
  final _postcode = TextEditingController();
  String _country = 'CN';
  bool _isDefault = false;
  bool _saving = false;

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    _state.dispose();
    _city.dispose();
    _line1.dispose();
    _postcode.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final app = context.read<AppState>();
    if (_name.text.trim().isEmpty ||
        _phone.text.trim().isEmpty ||
        _line1.text.trim().isEmpty ||
        _city.text.trim().isEmpty) {
      showToast(context, app.t('required_tip'));
      return;
    }
    setState(() => _saving = true);
    try {
      await AddressService.add({
        'receiverName': _name.text.trim(),
        'phone': _phone.text.trim(),
        'countryCode': _country,
        'state': _state.text.trim(),
        'city': _city.text.trim(),
        'addressLine1': _line1.text.trim(),
        'postcode': _postcode.text.trim(),
        'isDefault': _isDefault ? 1 : 0,
      });
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
      child: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(app.t('add_address'), style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              _field(_name, app.t('receiver_name')),
              _field(_phone, app.t('phone'), keyboard: TextInputType.phone),
              Row(
                children: [
                  Text('${app.t('country')}: '),
                  const SizedBox(width: 8),
                  DropdownButton<String>(
                    value: _country,
                    items: const [
                      DropdownMenuItem(value: 'CN', child: Text('CN')),
                      DropdownMenuItem(value: 'US', child: Text('US')),
                      DropdownMenuItem(value: 'GB', child: Text('GB')),
                      DropdownMenuItem(value: 'JP', child: Text('JP')),
                    ],
                    onChanged: (v) => setState(() => _country = v ?? 'CN'),
                  ),
                ],
              ),
              _field(_state, app.t('province')),
              _field(_city, app.t('city')),
              _field(_line1, app.t('address_detail')),
              _field(_postcode, app.t('postcode'), keyboard: TextInputType.number),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(app.t('set_default')),
                value: _isDefault,
                onChanged: (v) => setState(() => _isDefault = v),
              ),
              const SizedBox(height: 8),
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
      ),
    );
  }

  Widget _field(TextEditingController c, String label, {TextInputType? keyboard}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: TextField(
        controller: c,
        keyboardType: keyboard,
        decoration: InputDecoration(labelText: label, border: const OutlineInputBorder(), isDense: true),
      ),
    );
  }
}
