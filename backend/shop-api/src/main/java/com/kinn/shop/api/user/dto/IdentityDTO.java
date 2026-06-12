package com.kinn.shop.api.user.dto;

import lombok.Data;

import java.io.Serializable;

/** 清关实名快照（只携带脱敏信息，密文不出用户服务）。 */
@Data
public class IdentityDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String realName;
    private String idCardMask;
}
