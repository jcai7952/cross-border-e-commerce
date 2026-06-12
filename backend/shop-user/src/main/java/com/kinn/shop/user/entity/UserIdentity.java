package com.kinn.shop.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 清关实名信息（中国大陆订单海关申报用）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_identity")
public class UserIdentity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;

    /** 订购人姓名 */
    private String realName;

    /** 身份证号 AES 密文 */
    private String idCardCipher;

    /** 脱敏展示 */
    private String idCardMask;

    /** 1默认 0否 */
    private Integer isDefault;

    /** 校验位算法校验通过 */
    private Integer verified;
}
