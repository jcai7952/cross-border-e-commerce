package com.kinn.shop.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 配置（shop.minio.*）。
 */
@Data
@ConfigurationProperties(prefix = "shop.minio")
public class MinioProperties {

    /** 服务端内部访问地址 */
    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucket;

    /** 对外可访问地址（拼接图片完整 URL 用） */
    private String publicEndpoint;
}
