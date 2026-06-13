import '../core/api_client.dart';
import '../models/address.dart';
import '../models/cart.dart';
import '../models/checkout.dart';
import '../models/logistics.dart';
import '../models/order.dart';
import '../models/product.dart';

final _api = ApiClient.instance;

/// 认证。
class AuthService {
  /// 返回完整 data：{token,user}。
  static Future<Map<String, dynamic>> login(String email, String password) async {
    final data = await _api.post('/api/auth/login', body: {
      'email': email,
      'password': password,
    });
    return (data as Map).cast<String, dynamic>();
  }

  static Future<void> sendEmailCode(String email, {String scene = 'register'}) async {
    await _api.post('/api/auth/email-code', body: {'email': email, 'scene': scene});
  }

  static Future<Map<String, dynamic>> register({
    required String email,
    required String code,
    required String password,
    required String nickname,
  }) async {
    final data = await _api.post('/api/auth/register', body: {
      'email': email,
      'code': code,
      'password': password,
      'nickname': nickname,
    });
    return (data is Map) ? data.cast<String, dynamic>() : {};
  }
}

/// 商品 / 类目 / 闪购。
class CatalogService {
  static Future<Map<String, dynamic>> productPage({
    int? categoryId,
    String? keyword,
    String sort = 'sales',
    int pageNum = 1,
    int pageSize = 20,
    required String locale,
    required String currency,
  }) async {
    final data = await _api.get('/api/product/page', query: {
      'categoryId': ?categoryId,
      if (keyword != null && keyword.isNotEmpty) 'keyword': keyword,
      'sort': sort,
      'pageNum': pageNum,
      'pageSize': pageSize,
      'locale': locale,
      'currency': currency,
    });
    final m = (data as Map).cast<String, dynamic>();
    return {
      'total': m['total'] ?? 0,
      'list': ((m['list'] as List?) ?? [])
          .map((e) => ProductCard.fromJson(e as Map<String, dynamic>))
          .toList(),
    };
  }

  static Future<ProductDetail> productDetail(int id,
      {required String locale, required String currency}) async {
    final data = await _api.get('/api/product/$id', query: {
      'locale': locale,
      'currency': currency,
    });
    return ProductDetail.fromJson((data as Map).cast<String, dynamic>());
  }

  static Future<List<Category>> categoryTree({required String locale}) async {
    final data = await _api.get('/api/category/tree', query: {'locale': locale});
    return ((data as List?) ?? [])
        .map((e) => Category.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  static Future<List<ProductCard>> flashSale(
      {required String locale, required String currency}) async {
    final data = await _api.get('/api/flash-sale/current',
        query: {'locale': locale, 'currency': currency});
    if (data is Map && data['items'] is List) {
      return (data['items'] as List)
          .map((e) => ProductCard.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    return [];
  }
}

/// 购物车。
class CartService {
  static Future<Cart> list({required String locale, required String currency}) async {
    final data = await _api.get('/api/cart/list', query: {
      'locale': locale,
      'currency': currency,
    });
    return Cart.fromJson((data as Map).cast<String, dynamic>());
  }

  static Future<int> add(int skuId, int quantity) async {
    final data = await _api.post('/api/cart', body: {'skuId': skuId, 'quantity': quantity});
    return _count(data);
  }

  static Future<void> update(int id, {int? quantity, bool? checked}) async {
    await _api.put('/api/cart/$id', body: {
      'quantity': ?quantity,
      'checked': ?checked,
    });
  }

  static Future<void> checkAll(bool checked) async {
    await _api.put('/api/cart/check-all', body: {'checked': checked});
  }

  static Future<void> remove(int id) async {
    await _api.delete('/api/cart/$id');
  }

  static Future<int> count() async {
    final data = await _api.get('/api/cart/count');
    return _count(data);
  }

  static int _count(dynamic data) {
    if (data is Map && data['count'] != null) {
      final c = data['count'];
      return c is int ? c : int.tryParse(c.toString()) ?? 0;
    }
    return 0;
  }
}

/// 地址 / 实名。
class AddressService {
  static Future<List<Address>> list() async {
    final data = await _api.get('/api/address/list');
    return ((data as List?) ?? [])
        .map((e) => Address.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  static Future<void> add(Map<String, dynamic> body) async {
    await _api.post('/api/address', body: body);
  }

  static Future<List<Identity>> identityList() async {
    final data = await _api.get('/api/identity/list');
    return ((data as List?) ?? [])
        .map((e) => Identity.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  static Future<void> addIdentity(String realName, String idCardNo) async {
    await _api.post('/api/identity', body: {'realName': realName, 'idCardNo': idCardNo});
  }
}

/// 结算 / 下单。
class CheckoutService {
  static Future<CheckoutPreview> preview({
    required int addressId,
    bool fromCart = true,
    List<Map<String, dynamic>>? items,
    int? userCouponId,
    required String currency,
    required String locale,
  }) async {
    final data = await _api.post('/api/checkout/preview', body: {
      'addressId': addressId,
      if (items != null) 'items': items else 'fromCart': fromCart,
      'userCouponId': ?userCouponId,
      'currency': currency,
      'locale': locale,
    });
    return CheckoutPreview.fromJson((data as Map).cast<String, dynamic>());
  }

  /// 返回 {orderNo,payAmountMinor,payCurrency}。
  static Future<Map<String, dynamic>> createOrder({
    required int addressId,
    bool fromCart = true,
    List<Map<String, dynamic>>? items,
    int? userCouponId,
    int? identityId,
    required String payCurrency,
    required String locale,
    String? remark,
  }) async {
    final data = await _api.post('/api/order/create', body: {
      'addressId': addressId,
      if (items != null) 'items': items else 'fromCart': fromCart,
      'userCouponId': ?userCouponId,
      'identityId': ?identityId,
      'payCurrency': payCurrency,
      'locale': locale,
      if (remark != null && remark.isNotEmpty) 'remark': remark,
    });
    return (data as Map).cast<String, dynamic>();
  }
}

/// 支付。
class PayService {
  /// 返回 {payNo,channel,payloadType,payload:{redirectUrl}}。
  static Future<Map<String, dynamic>> create(String orderNo,
      {String channel = 'SIMULATOR'}) async {
    final data = await _api.post('/api/pay/create', body: {
      'orderNo': orderNo,
      'channel': channel,
    });
    return (data as Map).cast<String, dynamic>();
  }

  /// 模拟支付完成回调（演示用，等价于 simulator 页面点击"支付成功"）。
  static Future<void> notifySimulator(String payNo, {String result = 'SUCCESS'}) async {
    await _api.post('/api/pay/notify/simulator', body: {
      'payNo': payNo,
      'result': result,
      'eventId': 'SIMEVT-$payNo-$result',
    });
  }

  static Future<void> sync(String payNo) async {
    await _api.post('/api/pay/$payNo/sync');
  }

  /// 返回支付单状态对象。
  static Future<Map<String, dynamic>> query(String payNo) async {
    final data = await _api.get('/api/pay/$payNo');
    return (data as Map).cast<String, dynamic>();
  }
}

/// 订单。
class OrderService {
  static Future<Map<String, dynamic>> page({
    String? status,
    int pageNum = 1,
    int pageSize = 10,
    required String currency,
    required String locale,
  }) async {
    final data = await _api.get('/api/order/page', query: {
      if (status != null && status.isNotEmpty) 'status': status,
      'pageNum': pageNum,
      'pageSize': pageSize,
      'currency': currency,
      'locale': locale,
    });
    final m = (data as Map).cast<String, dynamic>();
    return {
      'total': m['total'] ?? 0,
      'list': ((m['list'] as List?) ?? [])
          .map((e) => OrderSummary.fromJson(e as Map<String, dynamic>))
          .toList(),
    };
  }

  static Future<OrderDetail> detail(String orderNo,
      {required String currency, required String locale}) async {
    final data = await _api.get('/api/order/$orderNo', query: {
      'currency': currency,
      'locale': locale,
    });
    return OrderDetail.fromJson((data as Map).cast<String, dynamic>());
  }

  static Future<void> cancel(String orderNo) async {
    await _api.post('/api/order/$orderNo/cancel');
  }

  static Future<void> confirm(String orderNo) async {
    await _api.post('/api/order/$orderNo/confirm');
  }
}

/// 物流。
class LogisticsService {
  static Future<Logistics?> track(String orderNo) async {
    final data = await _api.get('/api/logistics/track/$orderNo');
    if (data is Map) {
      return Logistics.fromJson(data.cast<String, dynamic>());
    }
    return null;
  }
}
