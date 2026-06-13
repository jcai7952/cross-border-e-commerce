import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/api_client.dart';
import '../core/app_state.dart';
import '../models/product.dart';
import '../widgets/common.dart';
import '../widgets/product_card.dart';
import 'main_shell.dart';
import 'product_detail_page.dart';
import 'search_page.dart';

/// 首页：搜索栏 + 语言币种入口 + 闪购横滑 + 商品瀑布流（分页加载）。
class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final _scroll = ScrollController();
  final List<ProductCard> _items = [];
  List<ProductCard> _flash = [];
  int _pageNum = 1;
  final int _pageSize = 20;
  bool _loading = false;
  bool _hasMore = true;
  bool _firstError = false;
  String _lastKey = '';

  @override
  void initState() {
    super.initState();
    _scroll.addListener(() {
      if (_scroll.position.pixels >= _scroll.position.maxScrollExtent - 300) {
        _loadMore();
      }
    });
    WidgetsBinding.instance.addPostFrameCallback((_) => _initLoad());
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  String _key(AppState app) => '${app.locale}|${app.currency}';

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final app = context.watch<AppState>();
    final k = _key(app);
    if (_lastKey.isNotEmpty && k != _lastKey) {
      _lastKey = k;
      _refresh();
    }
  }

  Future<void> _initLoad() async {
    _lastKey = _key(context.read<AppState>());
    await _refresh();
  }

  Future<void> _refresh() async {
    final app = context.read<AppState>();
    setState(() {
      _firstError = false;
      _pageNum = 1;
      _hasMore = true;
    });
    try {
      final flash = await CatalogService.flashSale(locale: app.locale, currency: app.currency);
      final res = await CatalogService.productPage(
        sort: 'sales',
        pageNum: 1,
        pageSize: _pageSize,
        locale: app.locale,
        currency: app.currency,
      );
      final list = res['list'] as List<ProductCard>;
      if (!mounted) return;
      setState(() {
        _flash = flash;
        _items
          ..clear()
          ..addAll(list);
        _hasMore = list.length >= _pageSize;
      });
    } on ApiException catch (e) {
      if (mounted) setState(() => _firstError = true);
      if (mounted && _items.isEmpty) showToast(context, e.message);
    } catch (_) {
      if (mounted) setState(() => _firstError = true);
    }
  }

  Future<void> _loadMore() async {
    if (_loading || !_hasMore) return;
    final app = context.read<AppState>();
    setState(() => _loading = true);
    try {
      final res = await CatalogService.productPage(
        sort: 'sales',
        pageNum: _pageNum + 1,
        pageSize: _pageSize,
        locale: app.locale,
        currency: app.currency,
      );
      final list = res['list'] as List<ProductCard>;
      if (!mounted) return;
      setState(() {
        _pageNum += 1;
        _items.addAll(list);
        _hasMore = list.length >= _pageSize;
      });
    } catch (_) {
      // 忽略加载更多错误
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            _searchBar(app),
            Expanded(
              child: _firstError && _items.isEmpty
                  ? ErrorView(message: app.t('load_failed'), onRetry: _refresh)
                  : RefreshIndicator(
                      onRefresh: _refresh,
                      child: CustomScrollView(
                        controller: _scroll,
                        slivers: [
                          if (_flash.isNotEmpty) _flashSection(app),
                          SliverToBoxAdapter(
                            child: Padding(
                              padding: const EdgeInsets.fromLTRB(12, 12, 12, 4),
                              child: Text(app.t('hot_recommend'),
                                  style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                            ),
                          ),
                          SliverPadding(
                            padding: const EdgeInsets.symmetric(horizontal: 12),
                            sliver: SliverGrid(
                              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                                crossAxisCount: 2,
                                mainAxisSpacing: 12,
                                crossAxisSpacing: 12,
                                childAspectRatio: 0.62,
                              ),
                              delegate: SliverChildBuilderDelegate(
                                (ctx, i) => ProductCardView(product: _items[i]),
                                childCount: _items.length,
                              ),
                            ),
                          ),
                          SliverToBoxAdapter(
                            child: Padding(
                              padding: const EdgeInsets.all(16),
                              child: Center(
                                child: _loading
                                    ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                                    : Text(_hasMore ? '' : app.t('no_more'),
                                        style: TextStyle(color: Colors.grey.shade400, fontSize: 12)),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _searchBar(AppState app) {
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(12, 8, 4, 8),
      child: Row(
        children: [
          Expanded(
            child: InkWell(
              onTap: () {
                Navigator.of(context).push(MaterialPageRoute(builder: (_) => const SearchPage()));
              },
              child: Container(
                height: 40,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                decoration: BoxDecoration(
                  color: const Color(0xFFF0F0F0),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Row(
                  children: [
                    Icon(Icons.search, size: 20, color: Colors.grey.shade500),
                    const SizedBox(width: 8),
                    Text(app.t('search_hint'), style: TextStyle(color: Colors.grey.shade500)),
                  ],
                ),
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.language),
            tooltip: '${app.t('language')} / ${app.t('currency')}',
            onPressed: () => showLocaleCurrencySheet(context),
          ),
        ],
      ),
    );
  }

  Widget _flashSection(AppState app) {
    return SliverToBoxAdapter(
      child: Container(
        margin: const EdgeInsets.fromLTRB(12, 12, 12, 0),
        padding: const EdgeInsets.symmetric(vertical: 10),
        decoration: BoxDecoration(
          gradient: const LinearGradient(colors: [Color(0xFFFFE9E9), Color(0xFFFFF6F6)]),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Row(
                children: [
                  Icon(Icons.flash_on, color: Theme.of(context).colorScheme.error, size: 20),
                  const SizedBox(width: 4),
                  Text(app.t('flash_sale'),
                      style: TextStyle(fontWeight: FontWeight.bold, color: Theme.of(context).colorScheme.error)),
                ],
              ),
            ),
            const SizedBox(height: 8),
            SizedBox(
              height: 150,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                itemCount: _flash.length,
                separatorBuilder: (_, _) => const SizedBox(width: 10),
                itemBuilder: (ctx, i) {
                  final p = _flash[i];
                  return SizedBox(
                    width: 100,
                    child: GestureDetector(
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute(
                            builder: (_) => ProductDetailPage(productId: p.id)),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: NetImage(p.mainImage, width: 100, height: 100),
                          ),
                          const SizedBox(height: 4),
                          if (p.effectivePrice != null)
                            Text(p.effectivePrice!.display,
                                style: TextStyle(
                                    color: Theme.of(context).colorScheme.error,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 13)),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
