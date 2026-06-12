package com.kinn.shop.product.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * 启动时确保 bucket 存在并放开匿名只读（s3:GetObject）。
 * MinIO 不可用时仅告警，不阻断服务启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketInitializer implements ApplicationRunner, Ordered {

    private final MinioClient minioClient;
    private final MinioProperties props;

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void run(ApplicationArguments args) {
        String bucket = props.getBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                String policy = """
                        {
                          "Version": "2012-10-17",
                          "Statement": [
                            {
                              "Effect": "Allow",
                              "Principal": {"AWS": ["*"]},
                              "Action": ["s3:GetObject"],
                              "Resource": ["arn:aws:s3:::%s/*"]
                            }
                          ]
                        }
                        """.formatted(bucket);
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
                log.info("[minio] bucket {} created with anonymous read policy", bucket);
            } else {
                log.info("[minio] bucket {} already exists", bucket);
            }
        } catch (Exception e) {
            log.warn("[minio] bucket init failed (service degraded, images unavailable): {}", e.getMessage());
        }
    }
}
