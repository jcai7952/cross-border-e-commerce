-- =====================================================
-- shop_product 商品库：类目(税率) / SPU / SKU(颜色×尺码) / 多语言
--                     / 币种汇率 / 评论 / 收藏 / 闪购 / 尺码表
-- =====================================================
CREATE DATABASE IF NOT EXISTS shop_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shop_product;

CREATE TABLE `category` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT,
  `parent_id`       BIGINT      NOT NULL DEFAULT 0,
  `level`           TINYINT     NOT NULL DEFAULT 1,
  `name_zh`         VARCHAR(64) NOT NULL,
  `name_en`         VARCHAR(64) NOT NULL,
  `icon`            VARCHAR(255) DEFAULT NULL,
  `sort`            INT         NOT NULL DEFAULT 0,
  `postal_tax_rate` INT         NOT NULL DEFAULT 20 COMMENT '直邮行邮税率% 13/20/50(中国进口)',
  `status`          TINYINT     NOT NULL DEFAULT 1,
  `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB COMMENT='商品类目';

CREATE TABLE `product` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `category_id`     BIGINT       NOT NULL COMMENT '叶子类目',
  `spu_code`        VARCHAR(40)  NOT NULL,
  `brand`           VARCHAR(64)  DEFAULT NULL,
  `trade_mode`      VARCHAR(8)   NOT NULL DEFAULT 'BONDED' COMMENT 'BONDED保税仓 DIRECT海外直邮(影响中国进口税)',
  `origin_country`  CHAR(2)      NOT NULL DEFAULT 'CN' COMMENT '发货地',
  `main_image`      VARCHAR(255) NOT NULL COMMENT 'MinIO 对象 key',
  `min_price_cents` BIGINT       NOT NULL DEFAULT 0 COMMENT '冗余:最低SKU价 USD分',
  `sales_count`     INT          NOT NULL DEFAULT 0,
  `rating_avg`      DECIMAL(2,1) NOT NULL DEFAULT 5.0,
  `rating_count`    INT          NOT NULL DEFAULT 0,
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_spu` (`spu_code`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status_sales` (`status`, `sales_count`)
) ENGINE=InnoDB COMMENT='商品SPU';

CREATE TABLE `product_i18n` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT       NOT NULL,
  `locale`     VARCHAR(8)   NOT NULL COMMENT 'zh-CN / en-US',
  `name`       VARCHAR(255) NOT NULL,
  `subtitle`   VARCHAR(255) DEFAULT NULL,
  `detail`     TEXT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_locale` (`product_id`, `locale`)
) ENGINE=InnoDB COMMENT='商品多语言';

CREATE TABLE `product_image` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT       NOT NULL,
  `url`        VARCHAR(255) NOT NULL COMMENT 'MinIO 对象 key',
  `sort`       INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='商品图册';

CREATE TABLE `product_sku` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `product_id`   BIGINT       NOT NULL,
  `sku_code`     VARCHAR(40)  NOT NULL,
  `color`        VARCHAR(32)  NOT NULL COMMENT '颜色(英文)',
  `color_zh`     VARCHAR(32)  NOT NULL COMMENT '颜色(中文)',
  `size`         VARCHAR(16)  NOT NULL COMMENT 'XS/S/M/L/XL/XXL 或 均码/鞋码',
  `price_cents`  BIGINT       NOT NULL COMMENT 'USD 分',
  `stock`        INT          NOT NULL DEFAULT 0,
  `image`        VARCHAR(255) DEFAULT NULL COMMENT '色卡图 对象key',
  `weight_grams` INT          NOT NULL DEFAULT 300,
  `version`      INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `status`       TINYINT      NOT NULL DEFAULT 1,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku` (`sku_code`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='SKU 颜色×尺码';

CREATE TABLE `size_chart` (
  `id`           BIGINT     NOT NULL AUTO_INCREMENT,
  `category_id`  BIGINT     NOT NULL,
  `locale`       VARCHAR(8) NOT NULL,
  `content_json` TEXT       NOT NULL COMMENT '[{"size":"S","bust":"86","waist":"66",...}]',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cat_locale` (`category_id`, `locale`)
) ENGINE=InnoDB COMMENT='尺码表(类目级)';

CREATE TABLE `currency` (
  `code`           CHAR(3)     NOT NULL,
  `symbol`         VARCHAR(8)  NOT NULL,
  `name_zh`        VARCHAR(32) NOT NULL,
  `name_en`        VARCHAR(32) NOT NULL,
  `decimal_digits` TINYINT     NOT NULL DEFAULT 2,
  `sort`           INT         NOT NULL DEFAULT 0,
  `enabled`        TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB COMMENT='币种';

CREATE TABLE `exchange_rate` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `base_currency`  CHAR(3)       NOT NULL DEFAULT 'USD',
  `quote_currency` CHAR(3)       NOT NULL,
  `rate`           DECIMAL(18,8) NOT NULL COMMENT '1 base = rate quote',
  `source`         VARCHAR(16)   NOT NULL DEFAULT 'MANUAL' COMMENT 'API/MANUAL',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pair` (`base_currency`, `quote_currency`)
) ENGINE=InnoDB COMMENT='汇率(定时API刷新+后台手工覆盖)';

CREATE TABLE `product_review` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `product_id`    BIGINT        NOT NULL,
  `order_no`      VARCHAR(40)   NOT NULL COMMENT '已购验证',
  `user_id`       BIGINT        NOT NULL,
  `user_nickname` VARCHAR(64)   DEFAULT NULL,
  `sku_text`      VARCHAR(64)   DEFAULT NULL COMMENT 'Black / M',
  `rating`        TINYINT       NOT NULL,
  `content`       VARCHAR(1000) DEFAULT NULL,
  `images`        TEXT          COMMENT 'JSON 数组 对象key',
  `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2拒绝',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_product` (`order_no`, `product_id`),
  KEY `idx_product_status` (`product_id`, `status`)
) ENGINE=InnoDB COMMENT='评论晒图';

CREATE TABLE `user_favorite` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL,
  `product_id`  BIGINT   NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB COMMENT='收藏夹';

CREATE TABLE `flash_sale` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `title`       VARCHAR(64) NOT NULL,
  `start_time`  DATETIME    NOT NULL,
  `end_time`    DATETIME    NOT NULL,
  `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='限时闪购活动';

CREATE TABLE `flash_sale_item` (
  `id`               BIGINT NOT NULL AUTO_INCREMENT,
  `sale_id`          BIGINT NOT NULL,
  `product_id`       BIGINT NOT NULL,
  `discount_percent` INT    NOT NULL COMMENT '折扣力度 30 = 7折(off 30%)',
  `quota`            INT    NOT NULL DEFAULT 0 COMMENT '0不限量',
  `sold`             INT    NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sale_product` (`sale_id`, `product_id`)
) ENGINE=InnoDB COMMENT='闪购商品(全SKU按比例折扣)';
