import 'address.dart';
import 'money.dart';

/// 订单行项。
class OrderItem {
  final int productId;
  final int skuId;
  final String productName;
  final String skuText;
  final String image;
  final int priceCents;
  final int quantity;
  final int totalCents;

  OrderItem({
    required this.productId,
    required this.skuId,
    required this.productName,
    required this.skuText,
    required this.image,
    required this.priceCents,
    required this.quantity,
    required this.totalCents,
  });

  factory OrderItem.fromJson(Map<String, dynamic> j) => OrderItem(
        productId: _asInt(j['productId']),
        skuId: _asInt(j['skuId']),
        productName: (j['productName'] ?? '').toString(),
        skuText: (j['skuText'] ?? '').toString(),
        image: (j['image'] ?? '').toString(),
        priceCents: _asInt(j['priceCents']),
        quantity: _asInt(j['quantity']),
        totalCents: _asInt(j['totalCents']),
      );
}

/// 订单列表项。
class OrderSummary {
  final String orderNo;
  final String status;
  final String tradeMode;
  final Money? totalDisplay;
  final String payCurrency;
  final int? countdownSeconds;
  final String createTime;
  final List<OrderItem> items;

  OrderSummary({
    required this.orderNo,
    required this.status,
    required this.tradeMode,
    this.totalDisplay,
    this.payCurrency = '',
    this.countdownSeconds,
    this.createTime = '',
    this.items = const [],
  });

  factory OrderSummary.fromJson(Map<String, dynamic> j) => OrderSummary(
        orderNo: (j['orderNo'] ?? '').toString(),
        status: (j['status'] ?? '').toString(),
        tradeMode: (j['tradeMode'] ?? '').toString(),
        totalDisplay: Money.fromPrice(j['totalDisplay']),
        payCurrency: (j['payCurrency'] ?? '').toString(),
        countdownSeconds: j['countdownSeconds'] == null ? null : _asInt(j['countdownSeconds']),
        createTime: (j['createTime'] ?? '').toString(),
        items: ((j['items'] as List?) ?? [])
            .map((e) => OrderItem.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}

/// 订单详情。
class OrderDetail {
  final String orderNo;
  final String status;
  final String tradeMode;
  final Money? goods;
  final Money? discount;
  final Money? shipping;
  final Money? tax;
  final Money? total;
  final String payCurrency;
  final Address? receiver;
  final String? identityName;
  final String? identityMask;
  final String? remark;
  final String createTime;
  final List<OrderItem> items;

  OrderDetail({
    required this.orderNo,
    required this.status,
    required this.tradeMode,
    this.goods,
    this.discount,
    this.shipping,
    this.tax,
    this.total,
    this.payCurrency = '',
    this.receiver,
    this.identityName,
    this.identityMask,
    this.remark,
    this.createTime = '',
    this.items = const [],
  });

  factory OrderDetail.fromJson(Map<String, dynamic> j) {
    final identity = j['identity'];
    return OrderDetail(
      orderNo: (j['orderNo'] ?? '').toString(),
      status: (j['status'] ?? '').toString(),
      tradeMode: (j['tradeMode'] ?? '').toString(),
      goods: Money.fromMoneyVO(j['goods']),
      discount: Money.fromMoneyVO(j['discount']),
      shipping: Money.fromMoneyVO(j['shipping']),
      tax: Money.fromMoneyVO(j['tax']),
      total: Money.fromMoneyVO(j['total']),
      payCurrency: (j['payCurrency'] ?? '').toString(),
      receiver: j['receiver'] is Map
          ? Address.fromJson(j['receiver'] as Map<String, dynamic>)
          : null,
      identityName: identity is Map ? identity['realName']?.toString() : null,
      identityMask: identity is Map ? identity['idCardMask']?.toString() : null,
      remark: j['remark']?.toString(),
      createTime: (j['createTime'] ?? '').toString(),
      items: ((j['items'] as List?) ?? [])
          .map((e) => OrderItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

int _asInt(dynamic v) {
  if (v is int) return v;
  if (v is num) return v.toInt();
  return int.tryParse(v?.toString() ?? '') ?? 0;
}
