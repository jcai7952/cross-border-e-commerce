package com.kinn.shop.product.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 管理端类目树（含税率/排序/状态）。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "管理端类目树节点")
public class AdminCategoryVO implements Serializable {

    private Long id;

    private Long parentId;

    private Integer level;

    private String nameZh;

    private String nameEn;

    private String icon;

    private Integer sort;

    @Schema(description = "直邮行邮税率%")
    private Integer postalTaxRate;

    private Integer status;

    private List<AdminCategoryVO> children;
}
