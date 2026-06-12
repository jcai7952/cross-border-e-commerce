package com.kinn.shop.user.vo;

import com.kinn.shop.user.entity.UserIdentity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 清关实名信息（只回脱敏 mask，不回明文/密文）。
 */
@Data
@Schema(description = "清关实名信息（脱敏）")
public class IdentityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String realName;

    @Schema(description = "身份证号脱敏展示")
    private String idCardMask;

    private Integer isDefault;
    private Integer verified;
    private LocalDateTime createTime;

    public static IdentityVO from(UserIdentity identity) {
        IdentityVO vo = new IdentityVO();
        vo.setId(identity.getId());
        vo.setRealName(identity.getRealName());
        vo.setIdCardMask(identity.getIdCardMask());
        vo.setIsDefault(identity.getIsDefault());
        vo.setVerified(identity.getVerified());
        vo.setCreateTime(identity.getCreateTime());
        return vo;
    }
}
