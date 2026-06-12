package com.kinn.shop.user.vo;

import com.kinn.shop.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息（不暴露 password）。
 */
@Data
@Schema(description = "用户信息")
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String email;
    private String nickname;
    private String avatar;
    private String locale;
    private String currency;
    private Integer emailVerified;
    private Integer status;
    private LocalDateTime createTime;

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setLocale(user.getLocale());
        vo.setCurrency(user.getCurrency());
        vo.setEmailVerified(user.getEmailVerified());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
