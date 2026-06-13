/// 物流轨迹节点。
class TrackNode {
  final String nodeCode;
  final String nodeZh;
  final String nodeEn;
  final String location;
  final String? remark;
  final String trackTime;

  TrackNode({
    required this.nodeCode,
    required this.nodeZh,
    required this.nodeEn,
    required this.location,
    this.remark,
    required this.trackTime,
  });

  factory TrackNode.fromJson(Map<String, dynamic> j) => TrackNode(
        nodeCode: (j['nodeCode'] ?? '').toString(),
        nodeZh: (j['nodeZh'] ?? '').toString(),
        nodeEn: (j['nodeEn'] ?? '').toString(),
        location: (j['location'] ?? '').toString(),
        remark: j['remark']?.toString(),
        trackTime: (j['trackTime'] ?? '').toString(),
      );

  String label(bool zh) => zh ? nodeZh : nodeEn;
}

/// 物流跟踪整体。
class Logistics {
  final String carrier;
  final String shipmentNo;
  final String status;
  final String currentNode;
  final List<TrackNode> tracks;

  Logistics({
    required this.carrier,
    required this.shipmentNo,
    required this.status,
    required this.currentNode,
    required this.tracks,
  });

  factory Logistics.fromJson(Map<String, dynamic> j) => Logistics(
        carrier: (j['carrier'] ?? '').toString(),
        shipmentNo: (j['shipmentNo'] ?? '').toString(),
        status: (j['status'] ?? '').toString(),
        currentNode: (j['currentNode'] ?? '').toString(),
        tracks: ((j['tracks'] as List?) ?? [])
            .map((e) => TrackNode.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}
