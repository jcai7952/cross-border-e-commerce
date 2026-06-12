package com.kinn.shop.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`user`")
public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 登录邮箱 */
    private String email;

    /** BCrypt 密码 */
    private String password;

    private String nickname;

    private String avatar;

    /** 偏好语言 zh-CN/en-US */
    private String locale;

    /** 偏好币种 */
    private String currency;

    /** 邮箱已验证 1是 0否 */
    private Integer emailVerified;

    /** 1正常 0禁用 */
    private Integer status;
}
