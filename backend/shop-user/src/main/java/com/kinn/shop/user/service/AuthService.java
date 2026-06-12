package com.kinn.shop.user.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kinn.shop.common.constant.RedisKeys;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.user.dto.EmailCodeDTO;
import com.kinn.shop.user.dto.LoginDTO;
import com.kinn.shop.user.dto.RegisterDTO;
import com.kinn.shop.user.dto.ResetPasswordDTO;
import com.kinn.shop.user.entity.User;
import com.kinn.shop.user.mapper.UserMapper;
import com.kinn.shop.user.vo.LoginVO;
import com.kinn.shop.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * C 端认证：邮箱验证码 / 注册 / 登录 / 登出 / 重置密码。
 * Sa-Token loginId 约定：用户 "u_"+id，管理员 "a_"+id（网关按前缀还原 X-User-Type）。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String SCENE_REGISTER = "register";
    public static final String SCENE_RESET = "reset";
    private static final String USER_LOGIN_PREFIX = "u_";

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;
    private final BCryptPasswordEncoder passwordEncoder;

    /** 发送邮箱验证码：register 场景查重 + 60s 频控 + 5min TTL */
    public void sendEmailCode(EmailCodeDTO dto) {
        String email = dto.getEmail();
        String scene = dto.getScene();
        if (SCENE_REGISTER.equals(scene) && findByEmail(email) != null) {
            throw new BizException(ErrorCode.EMAIL_EXISTS);
        }
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(RedisKeys.emailCodeLimit(scene, email), "1", Duration.ofSeconds(60));
        if (!Boolean.TRUE.equals(first)) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS);
        }
        String code = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue()
                .set(RedisKeys.emailCode(scene, email), code, Duration.ofMinutes(5));
        mailService.sendEmailCode(email, scene, code);
    }

    /** 注册：验证码取出即删，BCrypt 加密，注册即登录 */
    public LoginVO register(RegisterDTO dto) {
        verifyAndConsumeCode(SCENE_REGISTER, dto.getEmail(), dto.getCode());
        if (findByEmail(dto.getEmail()) != null) {
            throw new BizException(ErrorCode.EMAIL_EXISTS);
        }
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmailVerified(1);
        userMapper.insert(user);
        // 回读取 DB 默认值（locale/currency/status/create_time）
        User saved = userMapper.selectById(user.getId());
        StpUtil.login(USER_LOGIN_PREFIX + saved.getId());
        return new LoginVO(StpUtil.getTokenValue(), UserVO.from(saved));
    }

    /** 登录 */
    public LoginVO login(LoginDTO dto) {
        User user = findByEmail(dto.getEmail());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        StpUtil.login(USER_LOGIN_PREFIX + user.getId());
        return new LoginVO(StpUtil.getTokenValue(), UserVO.from(user));
    }

    /** 登出（无 token 时 Sa-Token 静默处理） */
    public void logout() {
        StpUtil.logout();
    }

    /** reset 场景验证码重置密码，重置后踢出该用户所有会话 */
    public void resetPassword(ResetPasswordDTO dto) {
        verifyAndConsumeCode(SCENE_RESET, dto.getEmail(), dto.getCode());
        User user = findByEmail(dto.getEmail());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
        StpUtil.logout(USER_LOGIN_PREFIX + user.getId());
    }

    /** 校验验证码：取出即删（无论对错只允许比对一次，防爆破） */
    private void verifyAndConsumeCode(String scene, String email, String code) {
        String key = RedisKeys.emailCode(scene, email);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            redisTemplate.delete(key);
        }
        if (cached == null || !cached.equals(code)) {
            throw new BizException(ErrorCode.EMAIL_CODE_ERROR);
        }
    }

    private User findByEmail(String email) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    }
}
