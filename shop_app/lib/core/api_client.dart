import 'package:dio/dio.dart';

/// 后端 API 网关地址。Flutter web 端通过此基址访问。
const String kApiBase = 'http://localhost:9600';

/// 业务异常：当后端返回 code != 0 时抛出，携带可展示的 message。
class ApiException implements Exception {
  final int code;
  final String message;
  ApiException(this.code, this.message);

  @override
  String toString() => 'ApiException($code, $message)';
}

/// dio 单例封装。
/// - 注入 satoken（登录态）与 Accept-Language（多语言）请求头。
/// - 统一解包 {code,message,data}：code==0 返回 data，否则抛 [ApiException]。
class ApiClient {
  ApiClient._() {
    _dio = Dio(
      BaseOptions(
        baseUrl: kApiBase,
        connectTimeout: const Duration(seconds: 15),
        receiveTimeout: const Duration(seconds: 20),
        contentType: 'application/json',
      ),
    );
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          if (_token != null && _token!.isNotEmpty) {
            options.headers['satoken'] = _token;
          }
          options.headers['Accept-Language'] = _locale;
          handler.next(options);
        },
      ),
    );
  }

  static final ApiClient instance = ApiClient._();

  late final Dio _dio;
  String? _token;
  String _locale = 'zh-CN';

  /// 由 AppState 调用以同步登录态与语言。
  void configure({String? token, String? locale}) {
    _token = token;
    if (locale != null) _locale = locale;
  }

  Map<String, dynamic> _unwrap(Response res) {
    final body = res.data;
    if (body is Map<String, dynamic>) {
      final code = body['code'];
      if (code == 0) {
        return body;
      }
      throw ApiException(
        code is int ? code : -1,
        (body['message'] ?? 'request failed').toString(),
      );
    }
    throw ApiException(-1, 'unexpected response');
  }

  /// 返回 data 字段（可能为 Map/List/标量/null）。
  Future<dynamic> get(String path, {Map<String, dynamic>? query}) async {
    try {
      final res = await _dio.get(path, queryParameters: query);
      return _unwrap(res)['data'];
    } on DioException catch (e) {
      throw _toApiException(e);
    }
  }

  Future<dynamic> post(String path, {Object? body, Map<String, dynamic>? query}) async {
    try {
      final res = await _dio.post(path, data: body, queryParameters: query);
      return _unwrap(res)['data'];
    } on DioException catch (e) {
      throw _toApiException(e);
    }
  }

  Future<dynamic> put(String path, {Object? body, Map<String, dynamic>? query}) async {
    try {
      final res = await _dio.put(path, data: body, queryParameters: query);
      return _unwrap(res)['data'];
    } on DioException catch (e) {
      throw _toApiException(e);
    }
  }

  Future<dynamic> delete(String path, {Object? body, Map<String, dynamic>? query}) async {
    try {
      final res = await _dio.delete(path, data: body, queryParameters: query);
      return _unwrap(res)['data'];
    } on DioException catch (e) {
      throw _toApiException(e);
    }
  }

  ApiException _toApiException(DioException e) {
    final data = e.response?.data;
    if (data is Map && data['message'] != null) {
      final code = data['code'];
      return ApiException(code is int ? code : -1, data['message'].toString());
    }
    return ApiException(-1, e.message ?? 'network error');
  }
}
