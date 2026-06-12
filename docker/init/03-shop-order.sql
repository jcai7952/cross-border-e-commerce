SET NAMES utf8mb4;
-- =====================================================
-- shop_order 交易库：购物车 / 优惠券 / 税则 / 订单 / 本地消息表
-- =====================================================
CREATE DATABASE IF NOT EXISTS shop_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shop_order;

CREATE TABLE `cart_item` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL,
  `product_id`  BIGINT   NOT NULL,
  `sku_id`      BIGINT   NOT NULL,
  `quantity`    INT      NOT NULL DEFAULT 1,
  `checked`     TINYINT  NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`)
) ENGINE=InnoDB COMMENT='购物车';

CREATE TABLE `coupon` (
  `id`               BIGINT      NOT NULL AUTO_INCREMENT,
  `title`            VARCHAR(64) NOT NULL,
  `type`             VARCHAR(8)  NOT NULL COMMENT 'FIXED满减 PERCENT折扣',
  `value`            BIGINT      NOT NULL COMMENT 'FIXED=USD分 PERCENT=off百分比',
  `min_amount_cents` BIGINT      NOT NULL DEFAULT 0 COMMENT '门槛(商品金额USD分)',
  `total_count`      INT         NOT NULL DEFAULT 0 COMMENT '0不限量',
  `received_count`   INT         NOT NULL DEFAULT 0,
  `per_user_limit`   INT         NOT NULL DEFAULT 1,
  `valid_from`       DATETIME    NOT NULL,
  `valid_to`         DATETIME    NOT NULL,
  `status`           TINYINT     NOT NULL DEFAULT 1,
  `create_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='优惠券';

CREATE TABLE `user_coupon` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `coupon_id`   BIGINT      NOT NULL,
  `user_id`     BIGINT      NOT NULL,
  `status`      TINYINT     NOT NULL DEFAULT 0 COMMENT '0未用 1已用 2过期',
  `order_no`    VARCHAR(40) DEFAULT NULL,
  `received_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `used_at`     DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB COMMENT='用户持券';

CREATE TABLE `tax_rule` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `country_code`      CHAR(2)      NOT NULL COMMENT '目的国',
  `tax_type`          VARCHAR(20)  NOT NULL COMMENT 'CN_CROSS_BORDER中国跨境(保税9.1%/直邮行邮税) VAT NONE',
  `rate_percent`      DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT 'VAT 税率%(CN 模式忽略此列按类目算)',
  `threshold_cents`   BIGINT       NOT NULL DEFAULT 0 COMMENT '免税额度 USD分(如美国 de minimis 800美元)',
  `identity_required` TINYINT      NOT NULL DEFAULT 0 COMMENT '清关是否需订购人实名',
  `note`              VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_country` (`country_code`)
) ENGINE=InnoDB COMMENT='目的国税则';

CREATE TABLE `orders` (
  `id`                    BIGINT        NOT NULL AUTO_INCREMENT,
  `order_no`              VARCHAR(40)   NOT NULL,
  `user_id`               BIGINT        NOT NULL,
  `status`                VARCHAR(20)   NOT NULL DEFAULT 'WAIT_PAY' COMMENT 'WAIT_PAY/PAID/SHIPPED/FINISHED/CLOSED',
  `trade_mode`            VARCHAR(8)    NOT NULL DEFAULT 'BONDED',
  `goods_amount_cents`    BIGINT        NOT NULL COMMENT '商品金额 USD分(成交价含闪购)',
  `shipping_amount_cents` BIGINT        NOT NULL DEFAULT 0,
  `tax_amount_cents`      BIGINT        NOT NULL DEFAULT 0,
  `discount_amount_cents` BIGINT        NOT NULL DEFAULT 0 COMMENT '券抵扣',
  `total_amount_cents`    BIGINT        NOT NULL COMMENT '应付 USD分',
  `pay_currency`          CHAR(3)       NOT NULL DEFAULT 'USD',
  `exchange_rate`         DECIMAL(18,8) NOT NULL DEFAULT 1 COMMENT '下单锁定 USD->支付币',
  `pay_amount_cents`      BIGINT        NOT NULL COMMENT '支付币最小单位金额',
  `user_coupon_id`        BIGINT        DEFAULT NULL,
  `locale`                VARCHAR(8)    NOT NULL DEFAULT 'en-US' COMMENT '下单语言(快照语种)',
  `receiver_json`         TEXT          NOT NULL COMMENT '收货地址快照',
  `identity_json`         TEXT          COMMENT '清关实名快照(姓名+脱敏证号)',
  `remark`                VARCHAR(255)  DEFAULT NULL,
  `pay_deadline`          DATETIME      NOT NULL,
  `paid_at`               DATETIME      DEFAULT NULL,
  `shipped_at`            DATETIME      DEFAULT NULL,
  `finished_at`           DATETIME      DEFAULT NULL,
  `closed_at`             DATETIME      DEFAULT NULL,
  `version`               INT           NOT NULL DEFAULT 0,
  `create_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_status_deadline` (`status`, `pay_deadline`)
) ENGINE=InnoDB COMMENT='订单';

CREATE TABLE `order_item` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `order_no`     VARCHAR(40)  NOT NULL,
  `product_id`   BIGINT       NOT NULL,
  `sku_id`       BIGINT       NOT NULL,
  `product_name` VARCHAR(255) NOT NULL COMMENT '下单语言快照',
  `sku_text`     VARCHAR(64)  NOT NULL COMMENT 'Black / M',
  `image`        VARCHAR(255) DEFAULT NULL,
  `price_cents`  BIGINT       NOT NULL COMMENT '成交单价 USD分(含闪购)',
  `quantity`     INT          NOT NULL,
  `total_cents`  BIGINT       NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_no`)
) ENGINE=InnoDB COMMENT='订单明细';

CREATE TABLE `order_status_log` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `order_no`    VARCHAR(40) NOT NULL,
  `from_status` VARCHAR(20) NOT NULL,
  `to_status`   VARCHAR(20) NOT NULL,
  `operator`    VARCHAR(64) NOT NULL COMMENT 'user:1 / admin:1 / system',
  `remark`      VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_no`)
) ENGINE=InnoDB COMMENT='订单状态流转日志';

CREATE TABLE `local_message` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `biz_type`        VARCHAR(32)  NOT NULL COMMENT 'ORDER_PAID_NOTIFY 等',
  `biz_key`         VARCHAR(64)  NOT NULL,
  `payload`         TEXT         NOT NULL,
  `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '0待投递 1成功 2死信',
  `retry_count`     INT          NOT NULL DEFAULT 0,
  `next_retry_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz` (`biz_type`, `biz_key`),
  KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB COMMENT='本地消息表(最终一致性)';

-- ---------- 税则种子 ----------
INSERT INTO `tax_rule` (`country_code`, `tax_type`, `rate_percent`, `threshold_cents`, `identity_required`, `note`) VALUES
('CN', 'CN_CROSS_BORDER', 0,     0,     1, '保税=综合税9.1%；直邮=按类目行邮税13/20/50，税额<=50元免征'),
('US', 'NONE',            0,     80000, 0, 'de minimis 800 USD 以下免税'),
('GB', 'VAT',             20.00, 0,     0, 'UK VAT'),
('DE', 'VAT',             19.00, 0,     0, 'Germany VAT'),
('FR', 'VAT',             20.00, 0,     0, 'France VAT'),
('IT', 'VAT',             22.00, 0,     0, 'Italy VAT'),
('ES', 'VAT',             21.00, 0,     0, 'Spain VAT'),
('JP', 'VAT',             10.00, 0,     0, 'Japan consumption tax'),
('AU', 'VAT',             10.00, 0,     0, 'Australia GST'),
('SG', 'VAT',             9.00,  0,     0, 'Singapore GST');

-- ---------- 优惠券种子 ----------
INSERT INTO `coupon` (`title`, `type`, `value`, `min_amount_cents`, `total_count`, `per_user_limit`, `valid_from`, `valid_to`, `status`) VALUES
('新人专享 $5 OFF',   'FIXED',   500,  2900, 0,    1, '2026-01-01 00:00:00', '2027-12-31 23:59:59', 1),
('全场 9 折',         'PERCENT', 10,   0,    1000, 2, '2026-06-01 00:00:00', '2026-12-31 23:59:59', 1),
('满 $99 减 $15',     'FIXED',   1500, 9900, 500,  1, '2026-06-01 00:00:00', '2026-12-31 23:59:59', 1);
