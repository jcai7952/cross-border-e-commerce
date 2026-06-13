import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/app_state.dart';
import 'address_page.dart';
import 'identity_page.dart';
import 'login_page.dart';
import 'main_shell.dart';
import 'order_list_page.dart';

/// 我的 tab：未登录→登录入口；已登录→用户信息 + 订单 + 地址 + 实名 + 语言币种 + 退出。
class MinePage extends StatelessWidget {
  const MinePage({super.key});

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(title: Text(app.t('tab_mine'))),
      body: ListView(
        children: [
          _header(context, app),
          const SizedBox(height: 8),
          if (app.isLoggedIn) _orderEntry(context, app),
          const SizedBox(height: 8),
          _tile(context, Icons.location_on_outlined, app.t('address_manage'), () {
            _requireLogin(context, app, () => const AddressPage());
          }),
          _tile(context, Icons.badge_outlined, app.t('identity_manage'), () {
            _requireLogin(context, app, () => const IdentityPage());
          }),
          _tile(context, Icons.language, '${app.t('language')} / ${app.t('currency')}', () {
            showLocaleCurrencySheet(context);
          }, trailing: Text('${app.locale == 'zh-CN' ? '中文' : 'EN'} · ${app.currency}',
              style: TextStyle(color: Colors.grey.shade500, fontSize: 13))),
          if (app.isLoggedIn) ...[
            const SizedBox(height: 16),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: OutlinedButton(
                onPressed: () => app.logout(),
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size.fromHeight(46),
                  foregroundColor: Theme.of(context).colorScheme.error,
                ),
                child: Text(app.t('logout')),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _header(BuildContext context, AppState app) {
    final email = app.user?['email']?.toString() ?? '';
    final nickname = app.user?['nickname']?.toString();
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(16, 24, 16, 24),
      child: Row(
        children: [
          CircleAvatar(
            radius: 30,
            backgroundColor: Theme.of(context).colorScheme.primary,
            child: const Icon(Icons.person, color: Colors.white, size: 32),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: app.isLoggedIn
                ? Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        (nickname != null && nickname.isNotEmpty) ? nickname : email.split('@').first,
                        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 4),
                      Text(email, style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
                    ],
                  )
                : Align(
                    alignment: Alignment.centerLeft,
                    child: FilledButton(
                      onPressed: () => Navigator.of(context).push(
                        MaterialPageRoute(builder: (_) => const LoginPage()),
                      ),
                      child: Text('${app.t('login')} / ${app.t('register')}'),
                    ),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _orderEntry(BuildContext context, AppState app) {
    final entries = [
      (Icons.payment_outlined, app.t('order_status_wait_pay'), 1),
      (Icons.inventory_2_outlined, app.t('order_status_paid'), 2),
      (Icons.local_shipping_outlined, app.t('order_status_shipped'), 3),
      (Icons.task_alt_outlined, app.t('order_status_finished'), 4),
    ];
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Column(
        children: [
          InkWell(
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const OrderListPage()),
            ),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: Row(
                children: [
                  const Icon(Icons.receipt_long_outlined),
                  const SizedBox(width: 8),
                  Text(app.t('my_orders'), style: const TextStyle(fontWeight: FontWeight.bold)),
                  const Spacer(),
                  Text(app.t('order_status_all'), style: TextStyle(color: Colors.grey.shade500, fontSize: 13)),
                  const Icon(Icons.chevron_right, size: 18),
                ],
              ),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: entries
                .map((e) => Expanded(
                      child: InkWell(
                        onTap: () => Navigator.of(context).push(
                          MaterialPageRoute(builder: (_) => OrderListPage(initialTab: e.$3)),
                        ),
                        child: Column(
                          children: [
                            Icon(e.$1, size: 26),
                            const SizedBox(height: 4),
                            Text(e.$2, style: const TextStyle(fontSize: 12)),
                          ],
                        ),
                      ),
                    ))
                .toList(),
          ),
        ],
      ),
    );
  }

  Widget _tile(BuildContext context, IconData icon, String title, VoidCallback onTap, {Widget? trailing}) {
    return Container(
      color: Colors.white,
      child: ListTile(
        leading: Icon(icon),
        title: Text(title),
        trailing: trailing ?? const Icon(Icons.chevron_right, size: 18),
        onTap: onTap,
      ),
    );
  }

  Future<void> _requireLogin(BuildContext context, AppState app, Widget Function() builder) async {
    if (!app.isLoggedIn) {
      final ok = await Navigator.of(context).push<bool>(
        MaterialPageRoute(builder: (_) => const LoginPage()),
      );
      if (ok != true) return;
    }
    if (context.mounted) {
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => builder()));
    }
  }
}
