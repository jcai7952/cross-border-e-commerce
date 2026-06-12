package com.kinn.shop.user.config;

import com.kinn.shop.user.entity.AdminUser;
import com.kinn.shop.user.mapper.AdminUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动初始化：admin_user 种子数据为明文密码（约定 123456），
 * 首启检测到非 BCrypt（不以 $2 开头）则就地升级为 BCrypt。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminUserMapper adminUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<AdminUser> admins = adminUserMapper.selectList(null);
        for (AdminUser admin : admins) {
            String password = admin.getPassword();
            if (password != null && !password.startsWith("$2")) {
                AdminUser update = new AdminUser();
                update.setId(admin.getId());
                update.setPassword(passwordEncoder.encode(password));
                adminUserMapper.updateById(update);
                log.info("[init] admin '{}' plaintext password upgraded to BCrypt", admin.getUsername());
            }
        }
    }
}
