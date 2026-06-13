import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/services.dart';
import '../core/api_client.dart';
import '../core/app_state.dart';
import '../models/product.dart';
import '../widgets/common.dart';
import 'checkout_page.dart';
import 'login_page.dart';

/// 商品详情：图集 + 价格(闪购) + 颜色尺码选择 + 数量 + 加入购物车/立即购买。
class ProductDetailPage extends StatefulWidget {
  final int productId;
  const ProductDetailPage({super.key, required this.productId});

  @override
  State<ProductDetailPage> createState() => _ProductDetailPageState();
}

class _ProductDetailPageState extends State<ProductDetailPage> {
  ProductDetail? _detail;
  bool _loading = true;
  bool _error = false;
  String _lastKey = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final app = context.watch<AppState>();
    final k = '${app.locale}|${app.currency}';
    if (_lastKey.isNotEmpty && k != _lastKey) {
      _lastKey = k;
      _load();
    }
  }

  Future<void> _load() async {
    final app = context.read<AppState>();
    _lastKey = '${app.locale}|${app.currency}';
    setState(() {
      _loading = true;
      _error = false;
    });
    try {
      final d = await CatalogService.productDetail(widget.productId,
          locale: app.locale, currency: app.currency);
      if (!mounted) return;
      setState(() {
        _detail = d;
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

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(title: Text(app.t('product'))),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error || _detail == null
              ? ErrorView(message: app.t('load_failed'), onRetry: _load)
              : _content(app, _detail!),
      bottomNavigationBar: (_loading || _detail == null) ? null : _bottomBar(app, _detail!),
    );
  }

  Widget _content(AppState app, ProductDetail d) {
    final zh = app.locale == 'zh-CN';
    final firstSku = d.skus.isNotEmpty ? d.skus.first : null;
    final mainImage = (d.images.isNotEmpty ? d.images.first : (firstSku?.image ?? ''));
    return ListView(
      children: [
        AspectRatio(
          aspectRatio: 1,
          child: PageView(
            children: (d.images.isEmpty ? [mainImage] : d.images)
                .map((u) => NetImage(u, fit: BoxFit.cover))
                .toList(),
          ),
        ),
        Container(
          color: Colors.white,
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  if (firstSku?.effectivePrice != null)
                    Text(firstSku!.effectivePrice!.display,
                        style: TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.bold,
                            color: d.hasFlash ? Theme.of(context).colorScheme.error : Colors.black)),
                  if (d.hasFlash && firstSku?.price != null) ...[
                    const SizedBox(width: 8),
                    Text(firstSku!.price!.display,
                        style: const TextStyle(
                            color: Colors.grey, decoration: TextDecoration.lineThrough)),
                  ],
                  if (d.hasFlash) ...[
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.error,
                          borderRadius: BorderRadius.circular(4)),
                      child: Text('${app.t('flash_sale')} -${d.flashDiscountPercent}%',
                          style: const TextStyle(color: Colors.white, fontSize: 11)),
                    ),
                  ],
                ],
              ),
              const SizedBox(height: 8),
              Text(d.name, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              if (d.subtitle.isNotEmpty) ...[
                const SizedBox(height: 4),
                Text(d.subtitle, style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
              ],
              const SizedBox(height: 8),
              Row(
                children: [
                  const Icon(Icons.star, size: 14, color: Colors.amber),
                  const SizedBox(width: 2),
                  Text('${d.ratingAvg.toStringAsFixed(1)} (${d.ratingCount})',
                      style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
                  const SizedBox(width: 12),
                  Text('${d.salesCount} ${app.t('sold')}',
                      style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
                  const Spacer(),
                  Chip(
                    label: Text(d.tradeMode, style: const TextStyle(fontSize: 11)),
                    visualDensity: VisualDensity.compact,
                    materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 8),
        Container(
          color: Colors.white,
          width: double.infinity,
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(app.t('product_detail_title'),
                  style: const TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              Text(_stripHtml(d.detail), style: const TextStyle(height: 1.5, fontSize: 14)),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                children: d.colorKeys().map((c) {
                  final sku = d.skusOfColor(c).first;
                  return Chip(label: Text(sku.colorLabel(zh)), visualDensity: VisualDensity.compact);
                }).toList(),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _bottomBar(AppState app, ProductDetail d) {
    return SafeArea(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        color: Colors.white,
        child: Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: () => _openSkuSheet(app, d, buyNow: false),
                child: Text(app.t('add_to_cart')),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: FilledButton(
                onPressed: () => _openSkuSheet(app, d, buyNow: true),
                style: FilledButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.error),
                child: Text(app.t('buy_now')),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _openSkuSheet(AppState app, ProductDetail d, {required bool buyNow}) async {
    final zh = app.locale == 'zh-CN';
    String? selectedColor = d.colorKeys().isNotEmpty ? d.colorKeys().first : null;
    Sku? selectedSku;
    int quantity = 1;

    Sku? skuFor(String? color, String? size) {
      if (color == null) return null;
      final list = d.skusOfColor(color);
      if (size == null) return list.isNotEmpty ? list.first : null;
      try {
        return list.firstWhere((s) => s.size == size);
      } catch (_) {
        return list.isNotEmpty ? list.first : null;
      }
    }

    selectedSku = skuFor(selectedColor, null);
    String? selectedSize = selectedSku?.size;

    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (ctx, setSheet) {
            final sizes = selectedColor == null
                ? <Sku>[]
                : d.skusOfColor(selectedColor!);
            final priceMoney = selectedSku?.effectivePrice;
            final inStock = (selectedSku?.stock ?? 0) > 0;
            return Padding(
              padding: EdgeInsets.only(bottom: MediaQuery.of(ctx).viewInsets.bottom),
              child: SingleChildScrollView(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: NetImage(selectedSku?.image ?? (d.images.isNotEmpty ? d.images.first : ''),
                                width: 80, height: 80),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                if (priceMoney != null)
                                  Text(priceMoney.display,
                                      style: TextStyle(
                                          fontSize: 20,
                                          fontWeight: FontWeight.bold,
                                          color: Theme.of(ctx).colorScheme.error)),
                                const SizedBox(height: 4),
                                Text('${app.t('stock')}: ${selectedSku?.stock ?? 0}',
                                    style: TextStyle(color: Colors.grey.shade600, fontSize: 12)),
                              ],
                            ),
                          ),
                        ],
                      ),
                      const Divider(height: 24),
                      Text(app.t('color'), style: const TextStyle(fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: d.colorKeys().map((c) {
                          final sku = d.skusOfColor(c).first;
                          final sel = c == selectedColor;
                          return ChoiceChip(
                            label: Text(sku.colorLabel(zh)),
                            selected: sel,
                            onSelected: (_) {
                              setSheet(() {
                                selectedColor = c;
                                selectedSku = skuFor(c, null);
                                selectedSize = selectedSku?.size;
                              });
                            },
                          );
                        }).toList(),
                      ),
                      const SizedBox(height: 16),
                      Text(app.t('size'), style: const TextStyle(fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: sizes.map((s) {
                          final sel = s.size == selectedSize;
                          return ChoiceChip(
                            label: Text(s.size),
                            selected: sel,
                            onSelected: (_) {
                              setSheet(() {
                                selectedSize = s.size;
                                selectedSku = s;
                              });
                            },
                          );
                        }).toList(),
                      ),
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          Text(app.t('quantity'), style: const TextStyle(fontWeight: FontWeight.bold)),
                          const Spacer(),
                          IconButton(
                            onPressed: quantity > 1 ? () => setSheet(() => quantity--) : null,
                            icon: const Icon(Icons.remove_circle_outline),
                          ),
                          Text('$quantity', style: const TextStyle(fontSize: 16)),
                          IconButton(
                            onPressed: () => setSheet(() => quantity++),
                            icon: const Icon(Icons.add_circle_outline),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      SizedBox(
                        width: double.infinity,
                        child: FilledButton(
                          onPressed: (selectedSku == null || !inStock)
                              ? null
                              : () => _confirm(ctx, app, selectedSku!, quantity, buyNow),
                          style: FilledButton.styleFrom(
                              backgroundColor: Theme.of(ctx).colorScheme.error),
                          child: Text(inStock
                              ? (buyNow ? app.t('buy_now') : app.t('add_to_cart'))
                              : app.t('out_of_stock')),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            );
          },
        );
      },
    );
  }

  Future<void> _confirm(
      BuildContext sheetCtx, AppState app, Sku sku, int quantity, bool buyNow) async {
    // 先关掉规格弹窗，后续都用页面 context。
    Navigator.of(sheetCtx).pop();
    if (!app.isLoggedIn) {
      if (!mounted) return;
      final ok = await Navigator.of(context).push<bool>(
        MaterialPageRoute(builder: (_) => const LoginPage()),
      );
      if (ok != true) return;
    }
    if (!mounted) return;
    try {
      if (buyNow) {
        Navigator.of(context).push(MaterialPageRoute(
          builder: (_) => CheckoutPage(
            buyNowItems: [
              {'skuId': sku.id, 'quantity': quantity}
            ],
          ),
        ));
      } else {
        final count = await CartService.add(sku.id, quantity);
        if (!mounted) return;
        app.setCartCount(count);
        showToast(context, app.t('success'));
      }
    } catch (e) {
      if (mounted) {
        showToast(context, e is ApiException ? e.message : app.t('load_failed'));
      }
    }
  }

  String _stripHtml(String html) {
    return html.replaceAll(RegExp(r'<[^>]*>'), '').trim();
  }
}
