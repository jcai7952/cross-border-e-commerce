package com.kinn.shop.product.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 前台类目树（两级，按 locale 取名）。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "类目树节点")
public class CategoryTreeVO implements Serializable {

    private Long id;

    @Schema(description = "按 locale 解析后的类目名")
    private String name;

    private String icon;

    private List<CategoryTreeVO> children;
}
