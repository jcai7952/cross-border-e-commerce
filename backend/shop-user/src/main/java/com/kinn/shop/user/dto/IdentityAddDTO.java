package com.kinn.shop.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "新增清关实名信息")
public class IdentityAddDTO {

    @NotBlank
    @Size(max = 64)
    @Schema(description = "订购人姓名")
    private String realName;

    @NotBlank
    @Schema(description = "身份证号（明文提交，仅存密文+脱敏）")
    private String idCardNo;
}
