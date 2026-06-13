import 'money.dart';

/// 商品列表项（瀑布流/网格/闪购共用）。
class ProductCard {
  final int id;
  final String name;
  final String mainImage;
  final Money? price;
  final Money? flashPrice;
  final int? discountPercent;
  final int salesCount;
  final double ratingAvg;
  final int ratingCount;

  ProductCard({
    required this.id,
    required this.name,
    required this.mainImage,
    this.price,
    this.flashPrice,
    this.discountPercent,
    this.salesCount = 0,
    this.ratingAvg = 0,
    this.ratingCount = 0,
  });

  factory ProductCard.fromJson(Map<String, dynamic> j) => ProductCard(
        id: _asInt(j['id']),
        name: (j['name'] ?? '').toString(),
        mainImage: (j['mainImage'] ?? '').toString(),
        price: Money.fromPrice(j['price']),
        flashPrice: Money.fromPrice(j['flashPrice']),
        discountPercent: j['discountPercent'] == null ? null : _asInt(j['discountPercent']),
        salesCount: _asInt(j['salesCount']),
        ratingAvg: _asDouble(j['ratingAvg']),
        ratingCount: _asInt(j['ratingCount']),
      );

  /// 实际生效价：有闪购价用闪购价。
  Money? get effectivePrice => flashPrice ?? price;
  bool get hasFlash => flashPrice != null;
}

/// SKU。
class Sku {
  final int id;
  final String color;
  final String colorZh;
  final String size;
  final Money? price;
  final Money? flashPrice;
  final int stock;
  final String image;

  Sku({
    required this.id,
    required this.color,
    required this.colorZh,
    required this.size,
    this.price,
    this.flashPrice,
    this.stock = 0,
    this.image = '',
  });

  factory Sku.fromJson(Map<String, dynamic> j) => Sku(
        id: _asInt(j['id']),
        color: (j['color'] ?? '').toString(),
        colorZh: (j['colorZh'] ?? '').toString(),
        size: (j['size'] ?? '').toString(),
        price: Money.fromPrice(j['price']),
        flashPrice: Money.fromPrice(j['flashPrice']),
        stock: _asInt(j['stock']),
        image: (j['image'] ?? '').toString(),
      );

  Money? get effectivePrice => flashPrice ?? price;

  String colorLabel(bool zh) => zh ? (colorZh.isNotEmpty ? colorZh : color) : color;
}

/// 商品详情。
class ProductDetail {
  final int id;
  final String name;
  final String subtitle;
  final String detail;
  final String tradeMode;
  final List<String> images;
  final List<Sku> skus;
  final double ratingAvg;
  final int ratingCount;
  final int salesCount;
  final bool favorite;
  final int? flashDiscountPercent;

  ProductDetail({
    required this.id,
    required this.name,
    required this.subtitle,
    required this.detail,
    required this.tradeMode,
    required this.images,
    required this.skus,
    this.ratingAvg = 0,
    this.ratingCount = 0,
    this.salesCount = 0,
    this.favorite = false,
    this.flashDiscountPercent,
  });

  factory ProductDetail.fromJson(Map<String, dynamic> j) {
    final flash = j['flash'];
    return ProductDetail(
      id: _asInt(j['id']),
      name: (j['name'] ?? '').toString(),
      subtitle: (j['subtitle'] ?? '').toString(),
      detail: (j['detail'] ?? '').toString(),
      tradeMode: (j['tradeMode'] ?? '').toString(),
      images: ((j['images'] as List?) ?? []).map((e) => e.toString()).toList(),
      skus: ((j['skus'] as List?) ?? [])
          .map((e) => Sku.fromJson(e as Map<String, dynamic>))
          .toList(),
      ratingAvg: _asDouble(j['ratingAvg']),
      ratingCount: _asInt(j['ratingCount']),
      salesCount: _asInt(j['salesCount']),
      favorite: j['favorite'] == true,
      flashDiscountPercent:
          (flash is Map && flash['discountPercent'] != null) ? _asInt(flash['discountPercent']) : null,
    );
  }

  bool get hasFlash => flashDiscountPercent != null;

  /// 去重后的颜色选项（按出现顺序）。
  List<String> colorKeys() {
    final seen = <String>{};
    final out = <String>[];
    for (final s in skus) {
      if (seen.add(s.color)) out.add(s.color);
    }
    return out;
  }

  /// 某颜色下可选尺码。
  List<Sku> skusOfColor(String color) => skus.where((s) => s.color == color).toList();
}

/// 类目节点。
class Category {
  final int id;
  final String name;
  final List<Category> children;

  Category({required this.id, required this.name, this.children = const []});

  factory Category.fromJson(Map<String, dynamic> j) => Category(
        id: _asInt(j['id']),
        name: (j['name'] ?? j['nameZh'] ?? '').toString(),
        children: ((j['children'] as List?) ?? [])
            .map((e) => Category.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}

int _asInt(dynamic v) {
  if (v is int) return v;
  if (v is num) return v.toInt();
  return int.tryParse(v?.toString() ?? '') ?? 0;
}

double _asDouble(dynamic v) {
  if (v is double) return v;
  if (v is num) return v.toDouble();
  return double.tryParse(v?.toString() ?? '') ?? 0;
}
