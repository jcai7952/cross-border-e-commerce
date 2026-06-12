package com.kinn.shop.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改个人资料（字段均可选，传了才更新）")
public class ProfileUpdateDTO {

    @Size(max = 64)
    @Schema(description = "昵称")
    private String nickname;

    @Size(max = 255)
    @Schema(description = "头像 URL")
    private String avatar;

    @Size(max = 8)
    @Schema(description = "偏好语言 zh-CN/en-US")
    private String locale;

    @Size(min = 3, max = 3)
    @Schema(description = "偏好币种 ISO 4217，如 USD")
    private String currency;
}
