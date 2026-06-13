import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/api_client.dart';
import '../core/app_state.dart';
import '../models/logistics.dart';
import '../models/order.dart';
import '../widgets/common.dart';

/// 订单详情：金额明细 + 收货信息 + 商品 + 物流时间线 + 操作(取消/确认收货)。
class OrderDetailPage extends StatefulWidget {
  final String orderNo;
  const OrderDetailPage({super.key, required this.orderNo});

  @override
  State<OrderDetailPage> createState() => _OrderDetailPageState();
}

class _OrderDetailPageState extends State<OrderDetailPage> {
  OrderDetail? _order;
  Logistics? _logistics;
  bool _loading = true;
  bool _error = false;
  bool _acting = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    final app = context.read<AppState>();
    setState(() {
      _loading = true;
      _error = false;
    });
    try {
      final d = await OrderService.detail(widget.orderNo, currency: app.currency, locale: app.locale);
      Logistics? log;
      // 已发货/已完成的订单才有物流
      if (d.status == 'SHIPPED' || d.status == 'FINISHED') {
        try {
          log = await LogisticsService.track(widget.orderNo);
        } catch (_) {}
      }
      if (!mounted) return;
      setState(() {
        _order = d;
        _logistics = log;
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

  Future<void> _cancel() async {
    final app = context.read<AppState>();
    setState(() => _acting = true);
    try {
      await OrderService.cancel(widget.orderNo);
      if (mounted) showToast(context, app.t('success'));
      await _load();
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } finally {
      if (mounted) setState(() => _acting = false);
    }
  }

  Future<void> _confirm() async {
    final app = context.read<AppState>();
    setState(() => _acting = true);
    try {
      await OrderService.confirm(widget.orderNo);
      if (mounted) showToast(context, app.t('success'));
      await _load();
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } finally {
      if (mounted) setState(() => _acting = false);
    }
  }

  String _statusLabel(AppState app, String status) {
    switch (status) {
      case 'WAIT_PAY':
        return app.t('order_status_wait_pay');
      case 'PAID':
        return app.t('order_status_paid');
      case 'SHIPPED':
        return app.t('order_status_shipped');
      case 'FINISHED':
        return app.t('order_status_finished');
      case 'CLOSED':
        return app.t('order_status_closed');
      default:
        return status;
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(title: Text(app.t('order_detail'))),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error || _order == null
              ? ErrorView(message: app.t('load_failed'), onRetry: _load)
              : ListView(
                  padding: const EdgeInsets.all(12),
                  children: [
                    _statusCard(app, _order!),
                    const SizedBox(height: 10),
                    if (_logistics != null) ...[
                      _logisticsCard(app, _logistics!),
                      const SizedBox(height: 10),
                    ],
                    _receiverCard(app, _order!),
                    const SizedBox(height: 10),
                    _itemsCard(app, _order!),
                    const SizedBox(height: 10),
                    _amountCard(app, _order!),
                  ],
                ),
      bottomNavigationBar: (_loading || _order == null) ? null : _bottomBar(app, _order!),
    );
  }

  Widget _card({required Widget child}) => Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8)),
        child: child,
      );

  Widget _statusCard(AppState app, OrderDetail o) {
    return _card(
      child: Row(
        children: [
          Icon(Icons.receipt_long, color: Theme.of(context).colorScheme.error),
          const SizedBox(width: 8),
          Text(_statusLabel(app, o.status),
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const Spacer(),
          Chip(
            label: Text(o.tradeMode, style: const TextStyle(fontSize: 11)),
            visualDensity: VisualDensity.compact,
            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
          ),
        ],
      ),
    );
  }

  Widget _logisticsCard(AppState app, Logistics log) {
    final zh = app.locale == 'zh-CN';
    return _card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.local_shipping_outlined, size: 20),
              const SizedBox(width: 8),
              Text(app.t('logistics'), style: const TextStyle(fontWeight: FontWeight.bold)),
              const Spacer(),
              Text('${log.carrier}  ${log.shipmentNo}',
                  style: TextStyle(fontSize: 11, color: Colors.grey.shade500)),
            ],
          ),
          const Divider(),
          if (log.tracks.isEmpty)
            Padding(
              padding: const EdgeInsets.all(8),
              child: Text(app.t('no_logistics'), style: TextStyle(color: Colors.grey.shade500)),
            )
          else
            ...List.generate(log.tracks.length, (i) => _trackNode(app, log.tracks[i], i == 0, i == log.tracks.length - 1, zh)),
        ],
      ),
    );
  }

  Widget _trackNode(AppState app, TrackNode node, bool isFirst, bool isLast, bool zh) {
    final color = isFirst ? Theme.of(context).colorScheme.error : Colors.grey.shade400;
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Column(
            children: [
              Container(
                width: 12,
                height: 12,
                margin: const EdgeInsets.only(top: 4),
                decoration: BoxDecoration(color: color, shape: BoxShape.circle),
              ),
              if (!isLast)
                Expanded(child: Container(width: 2, color: Colors.grey.shade300)),
            ],
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(node.label(zh),
                      style: TextStyle(
                          fontWeight: isFirst ? FontWeight.bold : FontWeight.normal,
                          color: isFirst ? Colors.black : Colors.grey.shade700,
                          fontSize: 13)),
                  if (node.location.isNotEmpty)
                    Text(node.location, style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
                  Text(_fmtTime(node.trackTime), style: TextStyle(fontSize: 11, color: Colors.grey.shade400)),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _receiverCard(AppState app, OrderDetail o) {
    final r = o.receiver;
    return _card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.location_on_outlined, size: 20),
              const SizedBox(width: 8),
              Text(app.t('receiver'), style: const TextStyle(fontWeight: FontWeight.bold)),
            ],
          ),
          const SizedBox(height: 8),
          if (r != null) ...[
            Row(
              children: [
                Text(r.receiverName, style: const TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(width: 8),
                Text(r.phone, style: TextStyle(color: Colors.grey.shade600)),
              ],
            ),
            const SizedBox(height: 2),
            Text(r.fullAddress, style: const TextStyle(fontSize: 13)),
          ],
          if (o.identityName != null) ...[
            const SizedBox(height: 6),
            Text('${app.t('identity_manage')}: ${o.identityName}  ${o.identityMask ?? ''}',
                style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
          ],
          if (o.remark != null && o.remark!.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text('${app.t('remark')}: ${o.remark}', style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
          ],
        ],
      ),
    );
  }

  Widget _itemsCard(AppState app, OrderDetail o) {
    return _card(
      child: Column(
        children: o.items
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
                            Text(it.productName, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 13)),
                            Text(it.skuText, style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
                          ],
                        ),
                      ),
                      Text('x${it.quantity}', style: TextStyle(color: Colors.grey.shade500)),
                    ],
                  ),
                ))
            .toList(),
      ),
    );
  }

  Widget _amountCard(AppState app, OrderDetail o) {
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
          row(app.t('goods_amount'), o.goods?.display ?? ''),
          if ((o.discount?.amountMinor ?? 0) > 0)
            row(app.t('discount'), '-${o.discount?.display ?? ''}', color: Theme.of(context).colorScheme.error),
          row(app.t('shipping'), o.shipping?.display ?? ''),
          row(app.t('tax'), o.tax?.display ?? ''),
          const Divider(),
          row(app.t('order_total'), o.total?.display ?? '',
              bold: true, color: Theme.of(context).colorScheme.error),
          const SizedBox(height: 6),
          Align(
            alignment: Alignment.centerLeft,
            child: Text('${app.t('order_no')}: ${o.orderNo}',
                style: TextStyle(fontSize: 12, color: Colors.grey.shade400)),
          ),
        ],
      ),
    );
  }

  Widget? _bottomBar(AppState app, OrderDetail o) {
    final buttons = <Widget>[];
    if (o.status == 'WAIT_PAY') {
      buttons.add(OutlinedButton(
        onPressed: _acting ? null : _cancel,
        child: Text(app.t('cancel_order')),
      ));
    }
    if (o.status == 'SHIPPED') {
      buttons.add(FilledButton(
        onPressed: _acting ? null : _confirm,
        style: FilledButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.error),
        child: Text(app.t('confirm_receive')),
      ));
    }
    if (buttons.isEmpty) return null;
    return SafeArea(
      child: Container(
        color: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            for (final b in buttons) ...[b, const SizedBox(width: 10)],
          ],
        ),
      ),
    );
  }

  String _fmtTime(String iso) {
    if (iso.isEmpty) return '';
    return iso.replaceFirst('T', ' ').split('.').first;
  }
}
