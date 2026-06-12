package com.kinn.shop.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "收货地址")
public class AddressDTO {

    @NotBlank
    @Size(max = 64)
    @Schema(description = "收件人姓名")
    private String receiverName;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "电话")
    private String phone;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "必须为 ISO 3166-1 alpha-2 两位国家码")
    @Schema(description = "国家码 ISO 3166-1 alpha-2，如 US")
    private String countryCode;

    @Size(max = 64)
    @Schema(description = "州/省（可选）")
    private String state;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "城市")
    private String city;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "地址行1")
    private String addressLine1;

    @Size(max = 255)
    @Schema(description = "地址行2（可选）")
    private String addressLine2;

    @Size(max = 20)
    @Schema(description = "邮编（可选）")
    private String postcode;

    @Schema(description = "是否设为默认（可选）")
    private Boolean isDefault;
}
