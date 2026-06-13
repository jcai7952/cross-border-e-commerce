import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/api_client.dart';
import '../core/app_state.dart';
import '../models/address.dart';
import '../models/checkout.dart';
import '../widgets/common.dart';
import 'address_page.dart';
import 'pay_page.dart';

/// 结算页：地址选择 + 实名(跨境需要) + 优惠券 + preview 明细 + 提交下单。
/// 支持购物车结算（默认）和立即购买（buyNowItems）。
class CheckoutPage extends StatefulWidget {
  /// 立即购买的行项 [{skuId,quantity}]；为 null 表示购物车结算。
  final List<Map<String, dynamic>>? buyNowItems;

  const CheckoutPage({super.key, this.buyNowItems});

  @override
  State<CheckoutPage> createState() => _CheckoutPageState();
}

class _CheckoutPageState extends State<CheckoutPage> {
  List<Identity> _identities = [];
  Address? _selectedAddress;
  Identity? _selectedIdentity;
  AvailableCoupon? _selectedCoupon;
  CheckoutPreview? _preview;
  final _remark = TextEditingController();

  bool _loading = true;
  bool _previewing = false;
  bool _submitting = false;
  bool _error = false;

  bool get _fromCart => widget.buyNowItems == null;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _init());
  }

  @override
  void dispose() {
    _remark.dispose();
    super.dispose();
  }

  Future<void> _init() async {
    setState(() {
      _loading = true;
      _error = false;
    });
    try {
      final addrs = await AddressService.list();
      final ids = await AddressService.identityList();
      if (!mounted) return;
      _identities = ids;
      _selectedAddress = addrs.where((a) => a.isDefault).isNotEmpty
          ? addrs.firstWhere((a) => a.isDefault)
          : (addrs.isNotEmpty ? addrs.first : null);
      _selectedIdentity = ids.where((i) => i.isDefault).isNotEmpty
          ? ids.firstWhere((i) => i.isDefault)
          : (ids.isNotEmpty ? ids.first : null);
      setState(() => _loading = false);
      if (_selectedAddress != null) await _refreshPreview();
    } catch (_) {
      if (mounted) {
        setState(() {
        _loading = false;
        _error = true;
        });
      }
    }
  }

  Future<void> _refreshPreview() async {
    final app = context.read<AppState>();
    if (_selectedAddress == null) return;
    setState(() => _previewing = true);
    try {
      final p = await CheckoutService.preview(
        addressId: _selectedAddress!.id,
        fromCart: _fromCart,
        items: widget.buyNowItems,
        userCouponId: _selectedCoupon?.userCouponId,
        currency: app.currency,
        locale: app.locale,
      );
      if (!mounted) return;
      setState(() {
        _preview = p;
        _previewing = false;
      });
    } on ApiException catch (e) {
      if (mounted) {
        setState(() => _previewing = false);
        showToast(context, e.message);
      }
    } catch (_) {
      if (mounted) setState(() => _previewing = false);
    }
  }

  Future<void> _submit() async {
    final app = context.read<AppState>();
    final preview = _preview;
    if (_selectedAddress == null || preview == null) {
      showToast(context, app.t('select_address'));
      return;
    }
    if (preview.identityRequired && _selectedIdentity == null) {
      showToast(context, app.t('identity_required'));
      return;
    }
    setState(() => _submitting = true);
    try {
      final res = await CheckoutService.createOrder(
        addressId: _selectedAddress!.id,
        fromCart: _fromCart,
        items: widget.buyNowItems,
        userCouponId: _selectedCoupon?.userCouponId,
        identityId: preview.identityRequired ? _selectedIdentity?.id : null,
        payCurrency: app.currency,
        locale: app.locale,
        remark: _remark.text,
      );
      final orderNo = res['orderNo']?.toString() ?? '';
      if (!mounted) return;
      // 刷新购物车角标
      if (_fromCart) {
        try {
          final c = await CartService.count();
          app.setCartCount(c);
        } catch (_) {}
      }
      if (!mounted) return;
      await Navigator.of(context).push(MaterialPageRoute(
        builder: (_) => PayPage(orderNo: orderNo),
      ));
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (_) {
      if (mounted) showToast(context, app.t('load_failed'));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(title: Text(app.t('checkout'))),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error
              ? ErrorView(message: app.t('load_failed'), onRetry: _init)
              : ListView(
                  padding: const EdgeInsets.all(12),
                  children: [
                    _addressCard(app),
                    const SizedBox(height: 10),
                    if (_preview?.identityRequired == true) ...[
                      _identityCard(app),
                      const SizedBox(height: 10),
                    ],
                    _itemsCard(app),
                    const SizedBox(height: 10),
                    _couponCard(app),
                    const SizedBox(height: 10),
                    _remarkCard(app),
                    const SizedBox(height: 10),
                    _amountCard(app),
                  ],
                ),
      bottomNavigationBar: _loading ? null : _bottomBar(app),
    );
  }

  Widget _card({required Widget child}) => Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8)),
        child: child,
      );

  Widget _addressCard(AppState app) {
    return InkWell(
      onTap: () async {
        final picked = await Navigator.of(context).push<Address>(
          MaterialPageRoute(builder: (_) => const AddressPage(selectable: true)),
        );
        if (picked != null) {
          setState(() => _selectedAddress = picked);
          await _refreshPreview();
        } else if (_selectedAddress == null) {
          // 之前没有地址，用户可能新增了，重新拉取并选默认
          final addrs = await AddressService.list();
          if (!mounted) return;
          if (addrs.isNotEmpty) {
            setState(() {
              _selectedAddress =
                  addrs.firstWhere((a) => a.isDefault, orElse: () => addrs.first);
            });
            await _refreshPreview();
          }
        }
      },
      child: _card(
        child: Row(
          children: [
            const Icon(Icons.location_on_outlined),
            const SizedBox(width: 8),
            Expanded(
              child: _selectedAddress == null
                  ? Text(app.t('no_address'))
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Text(_selectedAddress!.receiverName,
                                style: const TextStyle(fontWeight: FontWeight.bold)),
                            const SizedBox(width: 8),
                            Text(_selectedAddress!.phone, style: TextStyle(color: Colors.grey.shade600)),
                          ],
                        ),
                        const SizedBox(height: 2),
                        Text(_selectedAddress!.fullAddress, style: const TextStyle(fontSize: 13)),
                      ],
                    ),
            ),
            const Icon(Icons.chevron_right),
          ],
        ),
      ),
    );
  }

  Widget _identityCard(AppState app) {
    return _card(
      child: Row(
        children: [
          const Icon(Icons.badge_outlined),
          const SizedBox(width: 8),
          Expanded(
            child: _identities.isEmpty
                ? Text(app.t('identity_required'), style: TextStyle(color: Theme.of(context).colorScheme.error))
                : DropdownButton<Identity>(
                    isExpanded: true,
                    underline: const SizedBox(),
                    value: _selectedIdentity,
                    hint: Text(app.t('select_identity')),
                    items: _identities
                        .map((i) => DropdownMenuItem(
                              value: i,
                              child: Text('${i.realName}  ${i.idCardMask}'),
                            ))
                        .toList(),
                    onChanged: (v) => setState(() => _selectedIdentity = v),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _itemsCard(AppState app) {
    final items = _preview?.items ?? [];
    return _card(
      child: Column(
        children: items
            .map((it) => Padding(
                  padding: const EdgeInsets.symmetric(vertical: 6),
                  child: Row(
                    children: [
                      ClipRRect(
                        borderRadius: BorderRadius.circular(6),
                        child: NetImage(it.image, width: 56, height: 56),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(it.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 13)),
                            Text(it.skuText, style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
                          ],
                        ),
                      ),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          if (it.unitPrice != null) Text(it.unitPrice!.display, style: const TextStyle(fontSize: 13)),
                          Text('x${it.quantity}', style: TextStyle(color: Colors.grey.shade500, fontSize: 12)),
                        ],
                      ),
                    ],
                  ),
                ))
            .toList(),
      ),
    );
  }

  Widget _couponCard(AppState app) {
    final coupons = _preview?.availableCoupons ?? [];
    return _card(
      child: Row(
        children: [
          const Icon(Icons.local_offer_outlined),
          const SizedBox(width: 8),
          Text(app.t('coupon')),
          const Spacer(),
          DropdownButton<int>(
            value: _selectedCoupon?.userCouponId ?? -1,
            underline: const SizedBox(),
            items: [
              DropdownMenuItem(value: -1, child: Text(app.t('no_coupon'))),
              ...coupons.map((c) => DropdownMenuItem(value: c.userCouponId, child: Text(c.title))),
            ],
            onChanged: (v) async {
              setState(() {
                _selectedCoupon = (v == null || v == -1)
                    ? null
                    : coupons.firstWhere((c) => c.userCouponId == v);
              });
              await _refreshPreview();
            },
          ),
        ],
      ),
    );
  }

  Widget _remarkCard(AppState app) {
    return _card(
      child: Row(
        children: [
          const Icon(Icons.edit_note_outlined),
          const SizedBox(width: 8),
          Expanded(
            child: TextField(
              controller: _remark,
              decoration: InputDecoration(
                hintText: app.t('remark'),
                border: InputBorder.none,
                isDense: true,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _amountCard(AppState app) {
    final p = _preview;
    if (p == null) return const SizedBox();
    Widget row(String label, String value, {bool bold = false, Color? color}) => Padding(
          padding: const EdgeInsets.symmetric(vertical: 3),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(label, style: TextStyle(color: Colors.grey.shade700)),
              Text(value,
                  style: TextStyle(
                      fontWeight: bold ? FontWeight.bold : FontWeight.normal,
                      color: color,
                      fontSize: bold ? 16 : 14)),
            ],
          ),
        );
    return _card(
      child: Column(
        children: [
          row(app.t('goods_amount'), p.goods?.display ?? ''),
          if ((p.discount?.amountMinor ?? 0) > 0)
            row(app.t('discount'), '-${p.discount?.display ?? ''}', color: Theme.of(context).colorScheme.error),
          row(app.t('shipping'), p.shipping?.display ?? ''),
          row('${app.t('tax')}${p.taxNote.isNotEmpty ? ' (${p.taxNote})' : ''}', p.tax?.display ?? ''),
          if (p.estDaysMin > 0)
            row(app.t('est_delivery'), '${p.estDaysMin}-${p.estDaysMax} ${app.t('days')}'),
          const Divider(),
          row(app.t('order_total'), p.total?.display ?? '',
              bold: true, color: Theme.of(context).colorScheme.error),
        ],
      ),
    );
  }

  Widget _bottomBar(AppState app) {
    final p = _preview;
    return SafeArea(
      child: Container(
        color: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            if (_previewing)
              const Padding(
                padding: EdgeInsets.only(right: 12),
                child: SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2)),
              ),
            Text('${app.t('order_total')}: ',
                style: const TextStyle(fontSize: 13)),
            Text(p?.total?.display ?? '—',
                style: TextStyle(
                    color: Theme.of(context).colorScheme.error,
                    fontWeight: FontWeight.bold,
                    fontSize: 18)),
            const Spacer(),
            FilledButton(
              onPressed: (_submitting || p == null || _selectedAddress == null) ? null : _submit,
              style: FilledButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.error),
              child: _submitting
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : Text(app.t('submit_order')),
            ),
          ],
        ),
      ),
    );
  }
}
