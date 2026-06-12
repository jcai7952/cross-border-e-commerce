-- =====================================================
-- shop_payment 支付库：支付单 / 退款单 / 回调日志(幂等) / 本地消息表
-- =====================================================
CREATE DATABASE IF NOT EXISTS shop_payment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shop_payment;

CREATE TABLE `pay_order` (
  `id`               BIGINT      NOT NULL AUTO_INCREMENT,
  `pay_no`           VARCHAR(40) NOT NULL,
  `order_no`         VARCHAR(40) NOT NULL,
  `user_id`          BIGINT      NOT NULL,
  `channel`          VARCHAR(16) NOT NULL COMMENT 'STRIPE/PAYPAL/SIMULATOR',
  `amount_cents`     BIGINT      NOT NULL COMMENT '支付币最小单位',
  `currency`         CHAR(3)     NOT NULL,
  `status`           VARCHAR(16) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/PENDING/SUCCESS/FAILED/CLOSED',
  `channel_trade_no` VARCHAR(128) DEFAULT NULL COMMENT '渠道单号 pi_xxx / PayPal order id',
  `channel_payload`  TEXT         COMMENT '渠道响应快照(client_secret/approve链接等)',
  `version`          INT         NOT NULL DEFAULT 0,
  `paid_at`          DATETIME    DEFAULT NULL,
  `create_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_no` (`pay_no`),
  KEY `idx_order` (`order_no`),
  KEY `idx_channel_trade` (`channel_trade_no`)
) ENGINE=InnoDB COMMENT='支付单';

CREATE TABLE `refund_order` (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT,
  `refund_no`         VARCHAR(40) NOT NULL,
  `pay_no`            VARCHAR(40) NOT NULL,
  `order_no`          VARCHAR(40) NOT NULL,
  `user_id`           BIGINT      NOT NULL,
  `amount_cents`      BIGINT      NOT NULL,
  `currency`          CHAR(3)     NOT NULL,
  `status`            VARCHAR(16) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/SUCCESS/FAILED',
  `channel_refund_no` VARCHAR(128) DEFAULT NULL,
  `reason`            VARCHAR(255) DEFAULT NULL,
  `refunded_at`       DATETIME    DEFAULT NULL,
  `create_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_pay` (`pay_no`)
) ENGINE=InnoDB COMMENT='退款单';

CREATE TABLE `pay_notify_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `channel`     VARCHAR(16)  NOT NULL,
  `event_id`    VARCHAR(128) NOT NULL COMMENT '渠道事件ID(Stripe evt_xxx)，幂等键',
  `payload`     MEDIUMTEXT,
  `result`      VARCHAR(16)  NOT NULL DEFAULT 'OK',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_event` (`channel`, `event_id`)
) ENGINE=InnoDB COMMENT='渠道回调日志(DB幂等兜底)';

CREATE TABLE `local_message` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `biz_type`        VARCHAR(32)  NOT NULL,
  `biz_key`         VARCHAR(64)  NOT NULL,
  `payload`         TEXT         NOT NULL,
  `status`          TINYINT      NOT NULL DEFAULT 0,
  `retry_count`     INT          NOT NULL DEFAULT 0,
  `next_retry_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz` (`biz_type`, `biz_key`),
  KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB COMMENT='本地消息表(支付成功通知订单服务)';
