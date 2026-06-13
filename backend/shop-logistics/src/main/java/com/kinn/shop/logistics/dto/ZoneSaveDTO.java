package com.kinn.shop.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "区域计费入参（管理端，新增/更新共用）")
public class ZoneSaveDTO {

    @NotNull
    private Long templateId;

    @NotBlank
    @Size(max = 64)
    private String zoneName;

    @NotBlank
    @Size(max = 500)
    @Schema(description = "逗号分隔 ISO2，如 US,CA")
    private String countries;

    @NotNull
    @Min(1)
    @Schema(description = "首重克数")
    private Integer firstWeightG;

    @NotNull
    @Min(0)
    @Schema(description = "首重运费 USD 分")
    private Long firstFeeCents;

    @NotNull
    @Min(1)
    @Schema(description = "续重克数（每满一档加一次续重费）")
    private Integer addWeightG;

    @NotNull
    @Min(0)
    @Schema(description = "续重运费 USD 分")
    private Long addFeeCents;

    @NotNull
    @Min(1)
    private Integer estDaysMin;

    @NotNull
    @Min(1)
    private Integer estDaysMax;
}
