package com.kinn.shop.logistics.vo;

import com.kinn.shop.logistics.entity.ShippingZone;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 运费模板 + 其下区域计费全量（管理端）。 */
@Data
public class TemplateVO {

    private Long id;
    private String name;
    /** 1 启用 0 停用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<ShippingZone> zones;
}
