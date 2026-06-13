package com.kinn.shop.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新运费模板入参（管理端）")
public class TemplateUpdateDTO {

    @NotBlank
    @Size(max = 64)
    private String name;

    @NotNull
    @Min(0)
    @Max(1)
    @Schema(description = "1 启用 0 停用")
    private Integer status;
}
