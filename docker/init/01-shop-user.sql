-- =====================================================
-- shop_user 用户库：用户 / 地址 / 清关实名 / 管理员
-- =====================================================
CREATE DATABASE IF NOT EXISTS shop_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shop_user;

CREATE TABLE `user` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `email`          VARCHAR(128) NOT NULL COMMENT '登录邮箱',
  `password`       VARCHAR(100) NOT NULL COMMENT 'BCrypt',
  `nickname`       VARCHAR(64)  DEFAULT NULL,
  `avatar`         VARCHAR(255) DEFAULT NULL,
  `locale`         VARCHAR(8)   NOT NULL DEFAULT 'en-US' COMMENT '偏好语言 zh-CN/en-US',
  `currency`       CHAR(3)      NOT NULL DEFAULT 'USD' COMMENT '偏好币种',
  `email_verified` TINYINT      NOT NULL DEFAULT 0,
  `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB COMMENT='用户';

CREATE TABLE `user_address` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL,
  `receiver_name` VARCHAR(64)  NOT NULL,
  `phone`         VARCHAR(32)  NOT NULL,
  `country_code`  CHAR(2)      NOT NULL COMMENT 'ISO 3166-1 alpha-2',
  `state`         VARCHAR(64)  DEFAULT NULL COMMENT '州/省',
  `city`          VARCHAR(64)  NOT NULL,
  `address_line1` VARCHAR(255) NOT NULL,
  `address_line2` VARCHAR(255) DEFAULT NULL,
  `postcode`      VARCHAR(20)  DEFAULT NULL,
  `is_default`    TINYINT      NOT NULL DEFAULT 0,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB COMMENT='收货地址';

CREATE TABLE `user_identity` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT       NOT NULL,
  `real_name`      VARCHAR(64)  NOT NULL COMMENT '订购人姓名',
  `id_card_cipher` VARCHAR(256) NOT NULL COMMENT '身份证号 AES 密文',
  `id_card_mask`   VARCHAR(32)  NOT NULL COMMENT '脱敏展示 110***********1234',
  `is_default`     TINYINT      NOT NULL DEFAULT 0,
  `verified`       TINYINT      NOT NULL DEFAULT 1 COMMENT '校验位算法校验通过',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB COMMENT='清关实名信息(中国大陆订单海关申报用)';

CREATE TABLE `admin_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(64)  NOT NULL,
  `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt(首启由 DataInitializer 将明文种子升级)',
  `nickname`    VARCHAR(64)  DEFAULT NULL,
  `role`        VARCHAR(32)  NOT NULL DEFAULT 'ADMIN',
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT='后台管理员';

INSERT INTO `admin_user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '123456', '超级管理员', 'SUPER_ADMIN');
