package com.kinn.shop.user.service;

import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.user.dto.PasswordUpdateDTO;
import com.kinn.shop.user.dto.ProfileUpdateDTO;
import com.kinn.shop.user.entity.User;
import com.kinn.shop.user.mapper.UserMapper;
import com.kinn.shop.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * C 端用户资料。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserVO profile(long userId) {
        return UserVO.from(requireUser(userId));
    }

    /** 局部更新：仅更新传入的非空字段 */
    public UserVO updateProfile(long userId, ProfileUpdateDTO dto) {
        requireUser(userId);
        User update = new User();
        update.setId(userId);
        update.setNickname(dto.getNickname());
        update.setAvatar(dto.getAvatar());
        update.setLocale(dto.getLocale());
        update.setCurrency(dto.getCurrency() == null ? null : dto.getCurrency().toUpperCase());
        userMapper.updateById(update);
        return UserVO.from(userMapper.selectById(userId));
    }

    public void updatePassword(long userId, PasswordUpdateDTO dto) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
    }

    private User requireUser(long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
