/// 收货地址。
class Address {
  final int id;
  final String receiverName;
  final String phone;
  final String countryCode;
  final String state;
  final String city;
  final String addressLine1;
  final String? addressLine2;
  final String postcode;
  final bool isDefault;

  Address({
    required this.id,
    required this.receiverName,
    required this.phone,
    required this.countryCode,
    required this.state,
    required this.city,
    required this.addressLine1,
    this.addressLine2,
    required this.postcode,
    this.isDefault = false,
  });

  factory Address.fromJson(Map<String, dynamic> j) => Address(
        id: _asInt(j['id']),
        receiverName: (j['receiverName'] ?? '').toString(),
        phone: (j['phone'] ?? '').toString(),
        countryCode: (j['countryCode'] ?? '').toString(),
        state: (j['state'] ?? '').toString(),
        city: (j['city'] ?? '').toString(),
        addressLine1: (j['addressLine1'] ?? '').toString(),
        addressLine2: j['addressLine2']?.toString(),
        postcode: (j['postcode'] ?? '').toString(),
        isDefault: _asInt(j['isDefault']) == 1 || j['isDefault'] == true,
      );

  String get fullAddress {
    final parts = [
      addressLine1,
      if (addressLine2 != null && addressLine2!.isNotEmpty) addressLine2,
      city,
      state,
      countryCode,
      postcode,
    ].where((e) => e != null && e.toString().isNotEmpty).toList();
    return parts.join(', ');
  }
}

/// 实名信息。
class Identity {
  final int id;
  final String realName;
  final String idCardMask;
  final bool isDefault;
  final bool verified;

  Identity({
    required this.id,
    required this.realName,
    required this.idCardMask,
    this.isDefault = false,
    this.verified = false,
  });

  factory Identity.fromJson(Map<String, dynamic> j) => Identity(
        id: _asInt(j['id']),
        realName: (j['realName'] ?? '').toString(),
        idCardMask: (j['idCardMask'] ?? '').toString(),
        isDefault: _asInt(j['isDefault']) == 1 || j['isDefault'] == true,
        verified: _asInt(j['verified']) == 1 || j['verified'] == true,
      );
}

int _asInt(dynamic v) {
  if (v is int) return v;
  if (v is num) return v.toInt();
  return int.tryParse(v?.toString() ?? '') ?? 0;
}
