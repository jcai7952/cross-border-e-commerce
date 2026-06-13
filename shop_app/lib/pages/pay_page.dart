import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/api_client.dart';
import '../core/app_state.dart';
import '../models/order.dart';
import '../widgets/common.dart';
import 'order_detail_page.dart';

/// 支付页：展示应付金额 + 渠道(模拟支付)。
/// 流程：pay/create → notify/simulator(SUCCESS) → 轮询 pay/get → 成功跳订单。
class PayPage extends StatefulWidget {
  final String orderNo;
  const PayPage({super.key, required this.orderNo});

  @override
  State<PayPage> createState() => _PayPageState();
}

class _PayPageState extends State<PayPage> {
  OrderDetail? _order;
  bool _loading = true;
  bool _paying = false;
  bool _paid = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    final app = context.read<AppState>();
    try {
      final d = await OrderService.detail(widget.orderNo, currency: app.currency, locale: app.locale);
      if (!mounted) return;
      setState(() {
        _order = d;
        _loading = false;
        _paid = d.status != 'WAIT_PAY';
      });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _pay() async {
    final app = context.read<AppState>();
    setState(() => _paying = true);
    try {
      final create = await PayService.create(widget.orderNo);
      final payNo = create['payNo']?.toString() ?? '';
      // 演示：直接触发模拟支付成功回调（等价于 simulator 页点击"支付成功"）。
      await PayService.notifySimulator(payNo, result: 'SUCCESS');
      // 兜底同步 + 轮询支付状态
      String status = 'PENDING';
      for (int i = 0; i < 8; i++) {
        try {
          await PayService.sync(payNo);
        } catch (_) {}
        final q = await PayService.query(payNo);
        status = q['status']?.toString() ?? 'PENDING';
        if (status == 'SUCCESS' || status == 'FAILED') break;
        await Future.delayed(const Duration(milliseconds: 600));
      }
      if (!mounted) return;
      if (status == 'SUCCESS') {
        setState(() => _paid = true);
        showToast(context, app.t('pay_success'));
      } else {
        showToast(context, app.t('pay_failed'));
      }
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (_) {
      if (mounted) showToast(context, app.t('pay_failed'));
    } finally {
      if (mounted) setState(() => _paying = false);
    }
  }

  void _goOrder() {
    Navigator.of(context).pushReplacement(MaterialPageRoute(
      builder: (_) => OrderDetailPage(orderNo: widget.orderNo),
    ));
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return PopScope(
      canPop: true,
      child: Scaffold(
        appBar: AppBar(title: Text(app.t('pay'))),
        body: _loading
            ? const Center(child: CircularProgressIndicator())
            : _paid
                ? _successView(app)
                : _payView(app),
      ),
    );
  }

  Widget _payView(AppState app) {
    final amount = _order?.total?.display ?? '';
    return Column(
      children: [
        const SizedBox(height: 24),
        Text(app.t('pay_amount'), style: TextStyle(color: Colors.grey.shade600)),
        const SizedBox(height: 8),
        Text(amount,
            style: TextStyle(
                fontSize: 36, fontWeight: FontWeight.bold, color: Theme.of(context).colorScheme.error)),
        const SizedBox(height: 8),
        Text('${app.t('order_no')}: ${widget.orderNo}',
            style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
        const SizedBox(height: 24),
        Container(
          margin: const EdgeInsets.symmetric(horizontal: 16),
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8)),
          child: Row(
            children: [
              const Icon(Icons.credit_card, color: Colors.blue),
              const SizedBox(width: 12),
              Text(app.t('simulator_pay'), style: const TextStyle(fontWeight: FontWeight.bold)),
              const Spacer(),
              const Icon(Icons.check_circle, color: Colors.blue, size: 20),
            ],
          ),
        ),
        const Spacer(),
        SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: SizedBox(
              width: double.infinity,
              height: 48,
              child: FilledButton(
                onPressed: _paying ? null : _pay,
                style: FilledButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.error),
                child: _paying
                    ? Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)),
                          const SizedBox(width: 8),
                          Text(app.t('paying')),
                        ],
                      )
                    : Text(app.t('pay_now')),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _successView(AppState app) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.check_circle, color: Colors.green, size: 80),
          const SizedBox(height: 16),
          Text(app.t('pay_success'), style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          if (_order?.total != null)
            Text(_order!.total!.display, style: TextStyle(color: Colors.grey.shade600)),
          const SizedBox(height: 32),
          FilledButton(onPressed: _goOrder, child: Text(app.t('view_order'))),
        ],
      ),
    );
  }
}
