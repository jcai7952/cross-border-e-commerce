package com.kinn.shop.product.service;

import cn.hutool.core.util.IdUtil;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.product.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * MinIO 文件存取：对象 key 入库，出参一律拼完整 URL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;
    private final MinioProperties props;

    /** 上传，返回对象 key（dirPrefix/uuid.扩展名）。 */
    public String upload(MultipartFile file, String dirPrefix) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0 && dot < original.length() - 1) {
                ext = "." + original.substring(dot + 1).toLowerCase();
            }
        }
        String key = dirPrefix + "/" + IdUtil.fastSimpleUUID() + ext;
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(key)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .build());
        } catch (Exception e) {
            log.error("[minio] upload failed, key={}", key, e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
        return key;
    }

    /** 对象 key → 完整 URL；key 为空返回 null；已是完整 URL 原样返回。 */
    public String url(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.startsWith("http://") || key.startsWith("https://")) {
            return key;
        }
        return props.getPublicEndpoint() + "/" + props.getBucket() + "/" + key;
    }
}
