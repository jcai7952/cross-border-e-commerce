/// 价格/金额展示对象。后端统一返回 {currency,symbol,amountMinor,text}（price 风格）
/// 或 MoneyVO {usdCents,display:{...}}（结算/订单金额风格）。
/// 这里统一吸收为 symbol + text，金额一律用后端文本，不本地计算。
class Money {
  final String currency;
  final String symbol;
  final int amountMinor;
  final String text;

  Money({
    required this.currency,
    required this.symbol,
    required this.amountMinor,
    required this.text,
  });

  /// 完整展示串，如 "$23.98"。
  String get display => '$symbol$text';

  /// price 风格：{currency,symbol,amountMinor,text}
  static Money? fromPrice(dynamic json) {
    if (json == null || json is! Map) return null;
    return Money(
      currency: (json['currency'] ?? '').toString(),
      symbol: (json['symbol'] ?? '').toString(),
      amountMinor: _asInt(json['amountMinor']),
      text: (json['text'] ?? '0').toString(),
    );
  }

  /// MoneyVO 风格：{usdCents,display:{currency,symbol,amountMinor,text}}
  static Money? fromMoneyVO(dynamic json) {
    if (json == null || json is! Map) return null;
    final d = json['display'];
    if (d is Map) {
      return fromPrice(d);
    }
    return null;
  }

  static int _asInt(dynamic v) {
    if (v is int) return v;
    if (v is num) return v.toInt();
    return int.tryParse(v?.toString() ?? '') ?? 0;
  }
}
