package com.kinn.shop.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.user.dto.AdminLoginDTO;
import com.kinn.shop.user.entity.AdminUser;
import com.kinn.shop.user.mapper.AdminUserMapper;
import com.kinn.shop.user.vo.AdminLoginVO;
import com.kinn.shop.user.vo.AdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 管理端认证。Sa-Token loginId 约定："a_"+id。
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final String ADMIN_LOGIN_PREFIX = "a_";

    private final AdminUserMapper adminUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminLoginVO login(AdminLoginDTO dto) {
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, dto.getUsername()));
        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        StpUtil.login(ADMIN_LOGIN_PREFIX + admin.getId());
        return new AdminLoginVO(StpUtil.getTokenValue(), AdminVO.from(admin));
    }

    public AdminVO profile(long adminId) {
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return AdminVO.from(admin);
    }

    public void logout() {
        StpUtil.logout();
    }
}
