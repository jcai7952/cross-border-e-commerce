package com.kinn.shop.logistics.service;

import lombok.Getter;

import java.util.Map;

/**
 * 跨境物流轨迹节点链（按 ordinal 顺序推进）：
 * 揽收 → 出口报关 → 国际干线 → 进口清关 → 末端派送 → 签收。
 */
@Getter
public enum TrackNode {

    PICKED("已揽收", "Package picked up by carrier"),
    EXPORT_CUSTOMS("出口报关完成", "Export customs cleared"),
    INTL_TRANSIT("国际干线运输中", "In international transit"),
    IMPORT_CUSTOMS("进口清关完成(税金核验通过)", "Import customs cleared (duty verified)"),
    DELIVERING("末端派送中", "Out for delivery"),
    SIGNED("已签收", "Delivered & signed");

    private final String zh;
    private final String en;

    TrackNode(String zh, String en) {
        this.zh = zh;
        this.en = en;
    }

    /** 目的国 → 目的城市文案（演示用写死映射，未匹配回退通用文案）。 */
    private static final Map<String, String> DEST_CITY = Map.ofEntries(
            Map.entry("US", "Los Angeles"),
            Map.entry("CA", "Toronto"),
            Map.entry("GB", "London"),
            Map.entry("DE", "Frankfurt"),
            Map.entry("FR", "Paris"),
            Map.entry("IT", "Milan"),
            Map.entry("ES", "Madrid"),
            Map.entry("NL", "Amsterdam"),
            Map.entry("BE", "Brussels"),
            Map.entry("JP", "Tokyo"),
            Map.entry("KR", "Seoul"),
            Map.entry("SG", "Singapore"),
            Map.entry("AU", "Sydney"),
            Map.entry("NZ", "Auckland"),
            Map.entry("CN", "Shanghai"));

    /** code 不认识返回 null。 */
    public static TrackNode of(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return TrackNode.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 下一节点；已是 SIGNED 返回 null。 */
    public TrackNode next() {
        TrackNode[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }

    /** 按目的国生成节点位置文案。 */
    public String locationOf(String country) {
        String cc = country == null ? "" : country.trim().toUpperCase();
        String city = DEST_CITY.get(cc);
        return switch (this) {
            case PICKED -> "Guangzhou Sorting Center, CN";
            case EXPORT_CUSTOMS -> "Guangzhou Customs, CN";
            case INTL_TRANSIT -> "International Route";
            case IMPORT_CUSTOMS -> "CN".equals(cc) ? "Shanghai Bonded Warehouse, CN"
                    : (city == null ? "Destination Customs" : city + " Customs, " + cc);
            case DELIVERING -> city == null ? "Local Delivery Station" : city + " Delivery Station, " + cc;
            case SIGNED -> city == null ? "Recipient Address" : "Recipient Address, " + city + ", " + cc;
        };
    }
}
