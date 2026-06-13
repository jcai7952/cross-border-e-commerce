import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/app_state.dart';
import '../models/order.dart';
import '../widgets/common.dart';
import 'order_detail_page.dart';
import 'pay_page.dart';

/// 订单列表：按状态分 tab。
/// 状态映射：全部(空)/WAIT_PAY/PAID/SHIPPED/FINISHED/CLOSED。
class OrderListPage extends StatefulWidget {
  final int initialTab;
  const OrderListPage({super.key, this.initialTab = 0});

  @override
  State<OrderListPage> createState() => _OrderListPageState();
}

class _OrderListPageState extends State<OrderListPage> with SingleTickerProviderStateMixin {
  late TabController _tab;

  static const _statuses = ['', 'WAIT_PAY', 'PAID', 'SHIPPED', 'FINISHED'];

  @override
  void initState() {
    super.initState();
    _tab = TabController(length: _statuses.length, vsync: this, initialIndex: widget.initialTab);
  }

  @override
  void dispose() {
    _tab.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    final labels = [
      app.t('order_status_all'),
      app.t('order_status_wait_pay'),
      app.t('order_status_paid'),
      app.t('order_status_shipped'),
      app.t('order_status_finished'),
    ];
    return Scaffold(
      appBar: AppBar(
        title: Text(app.t('my_orders')),
        bottom: TabBar(
          controller: _tab,
          isScrollable: true,
          tabAlignment: TabAlignment.start,
          tabs: labels.map((l) => Tab(text: l)).toList(),
        ),
      ),
      body: TabBarView(
        controller: _tab,
        children: _statuses.map((s) => _OrderTab(status: s, key: ValueKey('tab-$s'))).toList(),
      ),
    );
  }
}

class _OrderTab extends StatefulWidget {
  final String status;
  const _OrderTab({super.key, required this.status});

  @override
  State<_OrderTab> createState() => _OrderTabState();
}

class _OrderTabState extends State<_OrderTab> with AutomaticKeepAliveClientMixin {
  final List<OrderSummary> _items = [];
  final _scroll = ScrollController();
  int _pageNum = 1;
  final int _pageSize = 10;
  bool _loading = false;
  bool _hasMore = true;
  bool _firstLoad = true;
  bool _error = false;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _scroll.addListener(() {
      if (_scroll.position.pixels >= _scroll.position.maxScrollExtent - 200) _loadMore();
    });
    WidgetsBinding.instance.addPostFrameCallback((_) => _refresh());
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _refresh() async {
    final app = context.read<AppState>();
    setState(() {
      _pageNum = 1;
      _hasMore = true;
      _error = false;
    });
    try {
      final res = await OrderService.page(
        status: widget.status.isEmpty ? null : widget.status,
        pageNum: 1,
        pageSize: _pageSize,
        currency: app.currency,
        locale: app.locale,
      );
      final list = res['list'] as List<OrderSummary>;
      if (!mounted) return;
      setState(() {
        _firstLoad = false;
        _items
          ..clear()
          ..addAll(list);
        _hasMore = list.length >= _pageSize;
      });
    } catch (_) {
      if (mounted) {
        setState(() {
        _firstLoad = false;
        _error = true;
        });
      }
    }
  }

  Future<void> _loadMore() async {
    if (_loading || !_hasMore) return;
    final app = context.read<AppState>();
    setState(() => _loading = true);
    try {
      final res = await OrderService.page(
        status: widget.status.isEmpty ? null : widget.status,
        pageNum: _pageNum + 1,
        pageSize: _pageSize,
        currency: app.currency,
        locale: app.locale,
      );
      final list = res['list'] as List<OrderSummary>;
      if (!mounted) return;
      setState(() {
        _pageNum += 1;
        _items.addAll(list);
        _hasMore = list.length >= _pageSize;
      });
    } catch (_) {
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final app = context.watch<AppState>();
    if (_firstLoad) return const Center(child: CircularProgressIndicator());
    if (_error && _items.isEmpty) return ErrorView(message: app.t('load_failed'), onRetry: _refresh);
    if (_items.isEmpty) return EmptyView(message: app.t('no_orders'), icon: Icons.receipt_long_outlined);
    return RefreshIndicator(
      onRefresh: _refresh,
      child: ListView.separated(
        controller: _scroll,
        padding: const EdgeInsets.all(12),
        itemCount: _items.length,
        separatorBuilder: (_, _) => const SizedBox(height: 10),
        itemBuilder: (ctx, i) => _orderCard(app, _items[i]),
      ),
    );
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

  Widget _orderCard(AppState app, OrderSummary o) {
    return InkWell(
      onTap: () async {
        await Navigator.of(context).push(MaterialPageRoute(
          builder: (_) => OrderDetailPage(orderNo: o.orderNo),
        ));
        _refresh();
      },
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8)),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text('${app.t('order_no')}: ${o.orderNo}',
                    style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
                const Spacer(),
                Text(_statusLabel(app, o.status),
                    style: TextStyle(
                        color: Theme.of(context).colorScheme.error, fontWeight: FontWeight.bold, fontSize: 13)),
              ],
            ),
            const Divider(),
            ...o.items.take(2).map((it) => Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
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
                            Text(it.productName, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 13)),
                            Text(it.skuText, style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
                          ],
                        ),
                      ),
                      Text('x${it.quantity}', style: TextStyle(color: Colors.grey.shade500)),
                    ],
                  ),
                )),
            const SizedBox(height: 6),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                Text('${app.t('total')}: ', style: const TextStyle(fontSize: 13)),
                Text(o.totalDisplay?.display ?? '',
                    style: const TextStyle(fontWeight: FontWeight.bold)),
              ],
            ),
            if (o.status == 'WAIT_PAY') ...[
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerRight,
                child: FilledButton(
                  onPressed: () async {
                    await Navigator.of(context).push(MaterialPageRoute(
                      builder: (_) => PayPage(orderNo: o.orderNo),
                    ));
                    _refresh();
                  },
                  style: FilledButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.error),
                  child: Text(app.t('go_pay')),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
