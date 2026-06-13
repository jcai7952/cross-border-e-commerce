import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/app_state.dart';
import '../models/product.dart';
import '../widgets/common.dart';
import '../widgets/product_card.dart';

/// 搜索结果页：可由首页搜索栏、分类页进入。支持 keyword 或 categoryId + 排序。
class SearchPage extends StatefulWidget {
  final String? initialKeyword;
  final int? categoryId;
  final String? title;

  const SearchPage({super.key, this.initialKeyword, this.categoryId, this.title});

  @override
  State<SearchPage> createState() => _SearchPageState();
}

class _SearchPageState extends State<SearchPage> {
  final _controller = TextEditingController();
  final _scroll = ScrollController();
  final List<ProductCard> _items = [];
  String _sort = 'sales';
  int _pageNum = 1;
  final int _pageSize = 20;
  bool _loading = false;
  bool _hasMore = true;
  bool _firstLoad = true;
  bool _error = false;

  @override
  void initState() {
    super.initState();
    _controller.text = widget.initialKeyword ?? '';
    _scroll.addListener(() {
      if (_scroll.position.pixels >= _scroll.position.maxScrollExtent - 300) {
        _loadMore();
      }
    });
    WidgetsBinding.instance.addPostFrameCallback((_) => _search(reset: true));
  }

  @override
  void dispose() {
    _controller.dispose();
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _search({required bool reset}) async {
    final app = context.read<AppState>();
    if (reset) {
      setState(() {
        _pageNum = 1;
        _hasMore = true;
        _error = false;
      });
    }
    try {
      final res = await CatalogService.productPage(
        categoryId: widget.categoryId,
        keyword: _controller.text.trim().isEmpty ? null : _controller.text.trim(),
        sort: _sort,
        pageNum: 1,
        pageSize: _pageSize,
        locale: app.locale,
        currency: app.currency,
      );
      final list = res['list'] as List<ProductCard>;
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
      final res = await CatalogService.productPage(
        categoryId: widget.categoryId,
        keyword: _controller.text.trim().isEmpty ? null : _controller.text.trim(),
        sort: _sort,
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
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(
        title: widget.categoryId != null
            ? Text(widget.title ?? app.t('tab_category'))
            : SizedBox(
                height: 38,
                child: TextField(
                  controller: _controller,
                  autofocus: widget.initialKeyword == null,
                  textInputAction: TextInputAction.search,
                  onSubmitted: (_) => _search(reset: true),
                  decoration: InputDecoration(
                    hintText: app.t('search_hint'),
                    prefixIcon: const Icon(Icons.search, size: 20),
                    filled: true,
                    fillColor: const Color(0xFFF0F0F0),
                    contentPadding: EdgeInsets.zero,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: BorderSide.none,
                    ),
                  ),
                ),
              ),
        actions: [
          if (widget.categoryId == null)
            TextButton(onPressed: () => _search(reset: true), child: Text(app.t('search_hint'))),
        ],
      ),
      body: Column(
        children: [
          _sortBar(app),
          Expanded(child: _body(app)),
        ],
      ),
    );
  }

  Widget _sortBar(AppState app) {
    final sorts = {
      'new': app.t('sort_new'),
      'sales': app.t('sort_sales'),
      'price_asc': app.t('sort_price_asc'),
      'price_desc': app.t('sort_price_desc'),
    };
    return Container(
      color: Colors.white,
      height: 44,
      child: Row(
        children: sorts.entries.map((e) {
          final selected = _sort == e.key;
          return Expanded(
            child: InkWell(
              onTap: () {
                setState(() => _sort = e.key);
                _search(reset: true);
              },
              child: Center(
                child: Text(
                  e.value,
                  style: TextStyle(
                    color: selected ? Theme.of(context).colorScheme.error : Colors.grey.shade700,
                    fontWeight: selected ? FontWeight.bold : FontWeight.normal,
                    fontSize: 13,
                  ),
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _body(AppState app) {
    if (_firstLoad) return const Center(child: CircularProgressIndicator());
    if (_error && _items.isEmpty) {
      return ErrorView(message: app.t('load_failed'), onRetry: () => _search(reset: true));
    }
    if (_items.isEmpty) return EmptyView(message: app.t('empty'));
    return GridView.builder(
      controller: _scroll,
      padding: const EdgeInsets.all(12),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        mainAxisSpacing: 12,
        crossAxisSpacing: 12,
        childAspectRatio: 0.62,
      ),
      itemCount: _items.length,
      itemBuilder: (ctx, i) => ProductCardView(product: _items[i]),
    );
  }
}
