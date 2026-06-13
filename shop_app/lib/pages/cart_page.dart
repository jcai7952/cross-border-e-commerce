import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/app_state.dart';
import '../models/cart.dart';
import '../widgets/common.dart';
import 'checkout_page.dart';
import 'login_page.dart';

/// 购物车 tab：勾选/改量/删除/合计/去结算。未登录显示登录引导。
class CartPage extends StatefulWidget {
  const CartPage({super.key});

  @override
  State<CartPage> createState() => CartPageState();
}

class CartPageState extends State<CartPage> {
  Cart? _cart;
  bool _loading = false;
  bool _error = false;
  String _lastKey = '';

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final app = context.watch<AppState>();
    final k = '${app.isLoggedIn}|${app.locale}|${app.currency}';
    if (k != _lastKey) {
      _lastKey = k;
      if (app.isLoggedIn) {
        WidgetsBinding.instance.addPostFrameCallback((_) => _load());
      } else {
        setState(() => _cart = null);
      }
    }
  }

  Future<void> _load() async {
    final app = context.read<AppState>();
    if (!app.isLoggedIn) return;
    setState(() {
      _loading = true;
      _error = false;
    });
    try {
      final cart = await CartService.list(locale: app.locale, currency: app.currency);
      if (!mounted) return;
      setState(() {
        _cart = cart;
        _loading = false;
      });
      app.setCartCount(cart.items.fold(0, (s, e) => s + e.quantity));
    } catch (_) {
      if (mounted) {
        setState(() {
        _loading = false;
        _error = true;
        });
      }
    }
  }

  Future<void> _changeQty(CartItem item, int delta) async {
    final newQty = item.quantity + delta;
    if (newQty < 1) return;
    final app = context.read<AppState>();
    setState(() => item.quantity = newQty);
    try {
      await CartService.update(item.id, quantity: newQty);
      await _load();
    } catch (e) {
      if (mounted) {
        setState(() => item.quantity -= delta);
        showToast(context, app.t('load_failed'));
      }
    }
  }

  Future<void> _toggleCheck(CartItem item, bool v) async {
    setState(() => item.checked = v);
    try {
      await CartService.update(item.id, checked: v);
      await _load();
    } catch (_) {
      if (mounted) setState(() => item.checked = !v);
    }
  }

  Future<void> _checkAll(bool v) async {
    try {
      await CartService.checkAll(v);
      await _load();
    } catch (_) {}
  }

  Future<void> _remove(CartItem item) async {
    try {
      await CartService.remove(item.id);
      await _load();
    } catch (e) {
      if (mounted) showToast(context, context.read<AppState>().t('load_failed'));
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(title: Text(app.t('tab_cart'))),
      body: !app.isLoggedIn ? _loginGuide(app) : _cartBody(app),
    );
  }

  Widget _loginGuide(AppState app) {
    return EmptyView(
      message: app.t('please_login'),
      icon: Icons.shopping_cart_outlined,
      action: FilledButton(
        onPressed: () async {
          final ok = await Navigator.of(context).push<bool>(
            MaterialPageRoute(builder: (_) => const LoginPage()),
          );
          if (ok == true) _load();
        },
        child: Text(app.t('login')),
      ),
    );
  }

  Widget _cartBody(AppState app) {
    if (_loading && _cart == null) return const Center(child: CircularProgressIndicator());
    if (_error && _cart == null) return ErrorView(message: app.t('load_failed'), onRetry: _load);
    final cart = _cart;
    if (cart == null || cart.items.isEmpty) {
      return EmptyView(message: app.t('cart_empty'), icon: Icons.remove_shopping_cart_outlined);
    }
    final allChecked = cart.items.every((e) => e.checked);
    return Column(
      children: [
        Expanded(
          child: RefreshIndicator(
            onRefresh: _load,
            child: ListView.separated(
              padding: const EdgeInsets.all(12),
              itemCount: cart.items.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (ctx, i) => _itemTile(app, cart.items[i]),
            ),
          ),
        ),
        _bottomBar(app, cart, allChecked),
      ],
    );
  }

  Widget _itemTile(AppState app, CartItem item) {
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8)),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Checkbox(
            value: item.checked,
            onChanged: item.invalid ? null : (v) => _toggleCheck(item, v ?? false),
          ),
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: NetImage(item.image, width: 72, height: 72),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(item.name, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 13)),
                const SizedBox(height: 2),
                Text(item.skuText, style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
                const SizedBox(height: 6),
                Row(
                  children: [
                    if (item.price != null)
                      Text(item.price!.display,
                          style: TextStyle(
                              color: Theme.of(context).colorScheme.error,
                              fontWeight: FontWeight.bold)),
                    const Spacer(),
                    _qtyStepper(item),
                  ],
                ),
              ],
            ),
          ),
          IconButton(
            icon: const Icon(Icons.delete_outline, size: 20),
            onPressed: () => _remove(item),
          ),
        ],
      ),
    );
  }

  Widget _qtyStepper(CartItem item) {
    return Row(
      children: [
        _stepBtn(Icons.remove, () => _changeQty(item, -1)),
        Container(
          width: 32,
          alignment: Alignment.center,
          child: Text('${item.quantity}'),
        ),
        _stepBtn(Icons.add, () => _changeQty(item, 1)),
      ],
    );
  }

  Widget _stepBtn(IconData icon, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      child: Container(
        width: 26,
        height: 26,
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey.shade300),
          borderRadius: BorderRadius.circular(4),
        ),
        child: Icon(icon, size: 16),
      ),
    );
  }

  Widget _bottomBar(AppState app, Cart cart, bool allChecked) {
    return SafeArea(
      child: Container(
        color: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            Checkbox(value: allChecked, onChanged: (v) => _checkAll(v ?? false)),
            Text(app.t('select_all')),
            const Spacer(),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('${app.t('total')}: ${cart.subtotal?.display ?? ''}',
                    style: TextStyle(
                        color: Theme.of(context).colorScheme.error,
                        fontWeight: FontWeight.bold,
                        fontSize: 16)),
              ],
            ),
            const SizedBox(width: 12),
            FilledButton(
              onPressed: cart.checkedCount > 0
                  ? () async {
                      final changed = await Navigator.of(context).push<bool>(
                        MaterialPageRoute(builder: (_) => const CheckoutPage()),
                      );
                      if (changed == true) _load();
                    }
                  : null,
              style: FilledButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.error),
              child: Text('${app.t('checkout')}(${cart.checkedCount})'),
            ),
          ],
        ),
      ),
    );
  }
}
