-- =====================================================
-- shop_logistics 物流库：运费模板 / 区域计费 / 物流单 / 轨迹
-- =====================================================
CREATE DATABASE IF NOT EXISTS shop_logistics DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shop_logistics;

CREATE TABLE `shipping_template` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(64) NOT NULL,
  `status`      TINYINT     NOT NULL DEFAULT 1,
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='运费模板';

CREATE TABLE `shipping_zone` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `template_id`     BIGINT       NOT NULL,
  `zone_name`       VARCHAR(64)  NOT NULL,
  `countries`       VARCHAR(500) NOT NULL COMMENT '逗号分隔 ISO2，如 US,CA',
  `first_weight_g`  INT          NOT NULL DEFAULT 500,
  `first_fee_cents` BIGINT       NOT NULL COMMENT 'USD分',
  `add_weight_g`    INT          NOT NULL DEFAULT 500,
  `add_fee_cents`   BIGINT       NOT NULL,
  `est_days_min`    INT          NOT NULL DEFAULT 7,
  `est_days_max`    INT          NOT NULL DEFAULT 15,
  PRIMARY KEY (`id`),
  KEY `idx_template` (`template_id`)
) ENGINE=InnoDB COMMENT='区域计费(首重+续重)';

CREATE TABLE `logistics_order` (
  `id`            BIGINT      NOT NULL AUTO_INCREMENT,
  `shipment_no`   VARCHAR(40) NOT NULL,
  `order_no`      VARCHAR(40) NOT NULL,
  `user_id`       BIGINT      NOT NULL,
  `carrier`       VARCHAR(32) NOT NULL DEFAULT 'KinnExpress',
  `country`       CHAR(2)     NOT NULL,
  `receiver_json` TEXT        NOT NULL,
  `weight_g`      INT         NOT NULL DEFAULT 0,
  `fee_cents`     BIGINT      NOT NULL DEFAULT 0,
  `status`        VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/IN_TRANSIT/SIGNED',
  `current_node`  VARCHAR(24) DEFAULT NULL,
  `signed_at`     DATETIME    DEFAULT NULL,
  `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shipment` (`shipment_no`),
  UNIQUE KEY `uk_order` (`order_no`)
) ENGINE=InnoDB COMMENT='物流单';

CREATE TABLE `logistics_track` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `shipment_no` VARCHAR(40)  NOT NULL,
  `node_code`   VARCHAR(24)  NOT NULL COMMENT 'PICKED/EXPORT_CUSTOMS/INTL_TRANSIT/IMPORT_CUSTOMS/DELIVERING/SIGNED',
  `node_zh`     VARCHAR(64)  NOT NULL,
  `node_en`     VARCHAR(128) NOT NULL,
  `location`    VARCHAR(64)  DEFAULT NULL,
  `remark`      VARCHAR(255) DEFAULT NULL,
  `track_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_shipment` (`shipment_no`)
) ENGINE=InnoDB COMMENT='物流轨迹';

-- ---------- 运费模板种子 ----------
INSERT INTO `shipping_template` (`id`, `name`, `status`) VALUES (1, '标准跨境专线', 1);

INSERT INTO `shipping_zone`
  (`template_id`, `zone_name`, `countries`, `first_weight_g`, `first_fee_cents`, `add_weight_g`, `add_fee_cents`, `est_days_min`, `est_days_max`) VALUES
(1, '北美',      'US,CA',                500, 800, 500, 400, 7, 12),
(1, '欧洲',      'GB,DE,FR,IT,ES,NL,BE', 500, 900, 500, 450, 8, 14),
(1, '亚太',      'JP,KR,SG,AU,NZ',       500, 700, 500, 350, 5, 10),
(1, '中国大陆',  'CN',                   500, 500, 500, 250, 3, 7);
