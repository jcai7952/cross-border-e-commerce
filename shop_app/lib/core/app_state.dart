import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'api_client.dart';
import 'i18n.dart';

/// 全局应用状态：登录态、用户、语言、币种、购物车角标。
/// 使用 provider/ChangeNotifier，并以 shared_preferences 持久化。
class AppState extends ChangeNotifier {
  static const _kToken = 'token';
  static const _kUser = 'user';
  static const _kLocale = 'locale';
  static const _kCurrency = 'currency';

  String? _token;
  Map<String, dynamic>? _user;
  String _locale = 'zh-CN';
  String _currency = 'USD';
  int _cartCount = 0;
  bool _ready = false;

  String? get token => _token;
  Map<String, dynamic>? get user => _user;
  String get locale => _locale;
  String get currency => _currency;
  int get cartCount => _cartCount;
  bool get isLoggedIn => _token != null && _token!.isNotEmpty;
  bool get ready => _ready;

  /// 当前语言下取词的便捷方法。
  String t(String key) => I18n.t(_locale, key);

  Future<void> load() async {
    final sp = await SharedPreferences.getInstance();
    _token = sp.getString(_kToken);
    final userStr = sp.getString(_kUser);
    if (userStr != null) {
      try {
        _user = jsonDecode(userStr) as Map<String, dynamic>;
      } catch (_) {}
    }
    _locale = sp.getString(_kLocale) ?? 'zh-CN';
    _currency = sp.getString(_kCurrency) ?? 'USD';
    ApiClient.instance.configure(token: _token, locale: _locale);
    _ready = true;
    notifyListeners();
  }

  Future<void> setSession(String token, Map<String, dynamic>? user) async {
    _token = token;
    _user = user;
    final sp = await SharedPreferences.getInstance();
    await sp.setString(_kToken, token);
    if (user != null) {
      await sp.setString(_kUser, jsonEncode(user));
    }
    ApiClient.instance.configure(token: _token, locale: _locale);
    notifyListeners();
  }

  Future<void> logout() async {
    _token = null;
    _user = null;
    _cartCount = 0;
    final sp = await SharedPreferences.getInstance();
    await sp.remove(_kToken);
    await sp.remove(_kUser);
    ApiClient.instance.configure(token: null, locale: _locale);
    notifyListeners();
  }

  Future<void> setLocale(String locale) async {
    _locale = locale;
    final sp = await SharedPreferences.getInstance();
    await sp.setString(_kLocale, locale);
    ApiClient.instance.configure(token: _token, locale: _locale);
    notifyListeners();
  }

  Future<void> setCurrency(String currency) async {
    _currency = currency;
    final sp = await SharedPreferences.getInstance();
    await sp.setString(_kCurrency, currency);
    notifyListeners();
  }

  void setCartCount(int count) {
    _cartCount = count;
    notifyListeners();
  }
}
