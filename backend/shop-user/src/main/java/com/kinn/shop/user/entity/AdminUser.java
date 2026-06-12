package com.kinn.shop.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台管理员。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_user")
public class AdminUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String username;

    /** BCrypt 密码 */
    private String password;

    private String nickname;

    private String role;

    /** 1正常 0禁用 */
    private Integer status;
}
