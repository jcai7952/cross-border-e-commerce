package com.kinn.shop.user.vo;

import com.kinn.shop.user.entity.AdminUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员信息（不暴露 password）。
 */
@Data
@Schema(description = "管理员信息")
public class AdminVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private String role;

    public static AdminVO from(AdminUser admin) {
        AdminVO vo = new AdminVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setNickname(admin.getNickname());
        vo.setRole(admin.getRole());
        return vo;
    }
}
