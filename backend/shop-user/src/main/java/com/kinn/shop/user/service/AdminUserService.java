package com.kinn.shop.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.user.entity.User;
import com.kinn.shop.user.mapper.UserMapper;
import com.kinn.shop.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 管理端用户管理。
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;

    public PageResult<UserVO> page(String email, Integer status, long pageNum, long pageSize) {
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<User>()
                        .like(StringUtils.hasText(email), User::getEmail, email)
                        .eq(status != null, User::getStatus, status)
                        .orderByDesc(User::getId));
        List<UserVO> list = page.getRecords().stream().map(UserVO::from).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, list);
    }

    /** 禁用/启用；禁用同时踢出该用户所有会话 */
    public void updateStatus(long id, int status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        User update = new User();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
        if (status == 0) {
            StpUtil.logout("u_" + id);
        }
    }
}
