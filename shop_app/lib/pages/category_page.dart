import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/app_state.dart';
import '../models/product.dart';
import '../widgets/common.dart';
import '../widgets/product_card.dart';
import 'search_page.dart';

/// 分类 tab：左侧一级类目栏 + 右侧二级类目网格 + 该一级类目商品预览。
class CategoryPage extends StatefulWidget {
  const CategoryPage({super.key});

  @override
  State<CategoryPage> createState() => _CategoryPageState();
}

class _CategoryPageState extends State<CategoryPage> {
  List<Category> _tree = [];
  int _selectedIndex = 0;
  bool _loading = true;
  bool _error = false;
  String _lastLocale = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final app = context.watch<AppState>();
    if (_lastLocale.isNotEmpty && app.locale != _lastLocale) {
      _lastLocale = app.locale;
      _load();
    }
  }

  Future<void> _load() async {
    final app = context.read<AppState>();
    _lastLocale = app.locale;
    setState(() {
      _loading = true;
      _error = false;
    });
    try {
      final tree = await CatalogService.categoryTree(locale: app.locale);
      if (!mounted) return;
      setState(() {
        _tree = tree;
        _loading = false;
        if (_selectedIndex >= _tree.length) _selectedIndex = 0;
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

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(title: Text(app.t('tab_category'))),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error
              ? ErrorView(message: app.t('load_failed'), onRetry: _load)
              : _tree.isEmpty
                  ? EmptyView(message: app.t('empty'))
                  : Row(
                      children: [
                        _leftBar(app),
                        Expanded(child: _rightContent(app)),
                      ],
                    ),
    );
  }

  Widget _leftBar(AppState app) {
    return Container(
      width: 96,
      color: const Color(0xFFF2F2F2),
      child: ListView.builder(
        itemCount: _tree.length,
        itemBuilder: (ctx, i) {
          final selected = i == _selectedIndex;
          return InkWell(
            onTap: () => setState(() => _selectedIndex = i),
            child: Container(
              color: selected ? Colors.white : Colors.transparent,
              padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 8),
              child: Row(
                children: [
                  if (selected)
                    Container(width: 3, height: 16, color: Theme.of(context).colorScheme.error),
                  if (selected) const SizedBox(width: 5),
                  Expanded(
                    child: Text(
                      _tree[i].name,
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: selected ? FontWeight.bold : FontWeight.normal,
                        color: selected ? Colors.black : Colors.grey.shade700,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _rightContent(AppState app) {
    final cat = _tree[_selectedIndex];
    return _CategoryProducts(category: cat, key: ValueKey('${app.locale}-${cat.id}'));
  }
}

/// 右侧：二级类目入口 + 一级类目热销预览。
class _CategoryProducts extends StatefulWidget {
  final Category category;
  const _CategoryProducts({super.key, required this.category});

  @override
  State<_CategoryProducts> createState() => _CategoryProductsState();
}

class _CategoryProductsState extends State<_CategoryProducts> {
  List<ProductCard> _items = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    final app = context.read<AppState>();
    setState(() => _loading = true);
    try {
      final res = await CatalogService.productPage(
        categoryId: widget.category.id,
        sort: 'sales',
        pageNum: 1,
        pageSize: 10,
        locale: app.locale,
        currency: app.currency,
      );
      if (!mounted) return;
      setState(() {
        _items = res['list'] as List<ProductCard>;
        _loading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    final children = widget.category.children;
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        if (children.isNotEmpty) ...[
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: children.map((c) {
              return ActionChip(
                label: Text(c.name),
                onPressed: () {
                  Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => SearchPage(categoryId: c.id, title: c.name),
                  ));
                },
              );
            }).toList(),
          ),
          const SizedBox(height: 12),
        ],
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(app.t('hot_recommend'), style: const TextStyle(fontWeight: FontWeight.bold)),
            TextButton(
              onPressed: () {
                Navigator.of(context).push(MaterialPageRoute(
                  builder: (_) => SearchPage(categoryId: widget.category.id, title: widget.category.name),
                ));
              },
              child: Text(app.t('order_status_all')),
            ),
          ],
        ),
        if (_loading)
          const Padding(padding: EdgeInsets.all(24), child: Center(child: CircularProgressIndicator()))
        else if (_items.isEmpty)
          Padding(padding: const EdgeInsets.all(24), child: Center(child: Text(app.t('empty'))))
        else
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              mainAxisSpacing: 12,
              crossAxisSpacing: 12,
              childAspectRatio: 0.62,
            ),
            itemCount: _items.length,
            itemBuilder: (ctx, i) => ProductCardView(product: _items[i]),
          ),
      ],
    );
  }
}
