import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/app_state.dart';
import 'cart_page.dart';
import 'category_page.dart';
import 'home_page.dart';
import 'mine_page.dart';

/// 主框架：底部 4 个 tab。
class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _index = 0;

  static const _pages = [HomePage(), CategoryPage(), CartPage(), MinePage()];

  @override
  void initState() {
    super.initState();
    _refreshCartCount();
  }

  Future<void> _refreshCartCount() async {
    final app = context.read<AppState>();
    if (!app.isLoggedIn) return;
    try {
      final c = await CartService.count();
      if (mounted) app.setCartCount(c);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Scaffold(
      body: IndexedStack(index: _index, children: _pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) {
          setState(() => _index = i);
          if (i == 2) _refreshCartCount();
        },
        destinations: [
          NavigationDestination(icon: const Icon(Icons.home_outlined), selectedIcon: const Icon(Icons.home), label: app.t('tab_home')),
          NavigationDestination(icon: const Icon(Icons.grid_view_outlined), selectedIcon: const Icon(Icons.grid_view), label: app.t('tab_category')),
          NavigationDestination(
            icon: Badge(
              isLabelVisible: app.cartCount > 0,
              label: Text('${app.cartCount}'),
              child: const Icon(Icons.shopping_cart_outlined),
            ),
            selectedIcon: Badge(
              isLabelVisible: app.cartCount > 0,
              label: Text('${app.cartCount}'),
              child: const Icon(Icons.shopping_cart),
            ),
            label: app.t('tab_cart'),
          ),
          NavigationDestination(icon: const Icon(Icons.person_outline), selectedIcon: const Icon(Icons.person), label: app.t('tab_mine')),
        ],
      ),
    );
  }
}

/// 语言 + 币种切换底部弹窗（首页/我的 共用）。
Future<void> showLocaleCurrencySheet(BuildContext context) async {
  final app = context.read<AppState>();
  await showModalBottomSheet(
    context: context,
    builder: (ctx) {
      return StatefulBuilder(
        builder: (ctx, setSheet) {
          return SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(app.t('language'), style: const TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    children: [
                      ChoiceChip(
                        label: const Text('中文'),
                        selected: app.locale == 'zh-CN',
                        onSelected: (_) {
                          app.setLocale('zh-CN');
                          setSheet(() {});
                        },
                      ),
                      ChoiceChip(
                        label: const Text('English'),
                        selected: app.locale == 'en-US',
                        onSelected: (_) {
                          app.setLocale('en-US');
                          setSheet(() {});
                        },
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Text(app.t('currency'), style: const TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    children: ['USD', 'CNY', 'EUR', 'GBP', 'JPY'].map((c) {
                      return ChoiceChip(
                        label: Text(c),
                        selected: app.currency == c,
                        onSelected: (_) {
                          app.setCurrency(c);
                          setSheet(() {});
                        },
                      );
                    }).toList(),
                  ),
                  const SizedBox(height: 8),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton(
                      onPressed: () => Navigator.of(ctx).pop(),
                      child: Text(app.t('confirm')),
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      );
    },
  );
}
