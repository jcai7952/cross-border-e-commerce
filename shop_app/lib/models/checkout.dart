import 'money.dart';

/// 结算预览行项。
class CheckoutLine {
  final int skuId;
  final int productId;
  final String name;
  final String skuText;
  final String image;
  final int quantity;
  final Money? unitPrice;
  final Money? lineTotal;

  CheckoutLine({
    required this.skuId,
    required this.productId,
    required this.name,
    required this.skuText,
    required this.image,
    required this.quantity,
    this.unitPrice,
    this.lineTotal,
  });

  factory CheckoutLine.fromJson(Map<String, dynamic> j) => CheckoutLine(
        skuId: _asInt(j['skuId']),
        productId: _asInt(j['productId']),
        name: (j['name'] ?? '').toString(),
        skuText: (j['skuText'] ?? '').toString(),
        image: (j['image'] ?? '').toString(),
        quantity: _asInt(j['quantity']),
        unitPrice: Money.fromPrice(j['unitPrice']),
        lineTotal: Money.fromPrice(j['lineTotal']),
      );
}

/// 可用优惠券。
class AvailableCoupon {
  final int userCouponId;
  final String title;
  final String type;
  final num value;
  final int discountCents;

  AvailableCoupon({
    required this.userCouponId,
    required this.title,
    required this.type,
    required this.value,
    required this.discountCents,
  });

  factory AvailableCoupon.fromJson(Map<String, dynamic> j) => AvailableCoupon(
        userCouponId: _asInt(j['userCouponId']),
        title: (j['title'] ?? '').toString(),
        type: (j['type'] ?? '').toString(),
        value: (j['value'] is num) ? j['value'] as num : 0,
        discountCents: _asInt(j['discountCents']),
      );
}

/// 结算预览结果。
class CheckoutPreview {
  final List<CheckoutLine> items;
  final Money? goods;
  final Money? discount;
  final Money? shipping;
  final Money? tax;
  final Money? total;
  final bool identityRequired;
  final String taxNote;
  final int estDaysMin;
  final int estDaysMax;
  final List<AvailableCoupon> availableCoupons;

  CheckoutPreview({
    required this.items,
    this.goods,
    this.discount,
    this.shipping,
    this.tax,
    this.total,
    this.identityRequired = false,
    this.taxNote = '',
    this.estDaysMin = 0,
    this.estDaysMax = 0,
    this.availableCoupons = const [],
  });

  factory CheckoutPreview.fromJson(Map<String, dynamic> j) => CheckoutPreview(
        items: ((j['items'] as List?) ?? [])
            .map((e) => CheckoutLine.fromJson(e as Map<String, dynamic>))
            .toList(),
        goods: Money.fromMoneyVO(j['goods']),
        discount: Money.fromMoneyVO(j['discount']),
        shipping: Money.fromMoneyVO(j['shipping']),
        tax: Money.fromMoneyVO(j['tax']),
        total: Money.fromMoneyVO(j['total']),
        identityRequired: j['identityRequired'] == true,
        taxNote: (j['taxNote'] ?? '').toString(),
        estDaysMin: _asInt(j['estDaysMin']),
        estDaysMax: _asInt(j['estDaysMax']),
        availableCoupons: ((j['availableCoupons'] as List?) ?? [])
            .map((e) => AvailableCoupon.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}

int _asInt(dynamic v) {
  if (v is int) return v;
  if (v is num) return v.toInt();
  return int.tryParse(v?.toString() ?? '') ?? 0;
}
