import 'money.dart';

/// 购物车条目。
class CartItem {
  final int id;
  final int skuId;
  final int productId;
  final String name;
  final String skuText;
  final String image;
  final Money? price;
  final Money? originalPrice;
  final int? discountPercent;
  int quantity;
  bool checked;
  final int stock;
  final bool invalid;

  CartItem({
    required this.id,
    required this.skuId,
    required this.productId,
    required this.name,
    required this.skuText,
    required this.image,
    this.price,
    this.originalPrice,
    this.discountPercent,
    this.quantity = 1,
    this.checked = true,
    this.stock = 0,
    this.invalid = false,
  });

  factory CartItem.fromJson(Map<String, dynamic> j) => CartItem(
        id: _asInt(j['id']),
        skuId: _asInt(j['skuId']),
        productId: _asInt(j['productId']),
        name: (j['name'] ?? '').toString(),
        skuText: (j['skuText'] ?? '').toString(),
        image: (j['image'] ?? '').toString(),
        price: Money.fromPrice(j['price']),
        originalPrice: Money.fromPrice(j['originalPrice']),
        discountPercent: j['discountPercent'] == null ? null : _asInt(j['discountPercent']),
        quantity: _asInt(j['quantity']),
        checked: j['checked'] == true,
        stock: _asInt(j['stock']),
        invalid: j['invalid'] == true,
      );
}

/// 购物车整体。
class Cart {
  final List<CartItem> items;
  final int checkedCount;
  final Money? subtotal;

  Cart({required this.items, this.checkedCount = 0, this.subtotal});

  factory Cart.fromJson(Map<String, dynamic> j) => Cart(
        items: ((j['items'] as List?) ?? [])
            .map((e) => CartItem.fromJson(e as Map<String, dynamic>))
            .toList(),
        checkedCount: _asInt(j['checkedCount']),
        subtotal: Money.fromPrice(j['subtotal']),
      );
}

int _asInt(dynamic v) {
  if (v is int) return v;
  if (v is num) return v.toInt();
  return int.tryParse(v?.toString() ?? '') ?? 0;
}
