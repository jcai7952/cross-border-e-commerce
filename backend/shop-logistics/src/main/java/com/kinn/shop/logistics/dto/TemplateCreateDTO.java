package com.kinn.shop.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建运费模板入参（管理端）")
public class TemplateCreateDTO {

    @NotBlank
    @Size(max = 64)
    private String name;
}
