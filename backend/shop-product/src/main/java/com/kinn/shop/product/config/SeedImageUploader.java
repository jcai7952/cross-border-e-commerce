package com.kinn.shop.product.config;

import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 种子商品图生成器：首启时为 16 个种子 SPU 各生成 3 张 SVG 占位图
 * （products/p{1..16}_main.svg、_1.svg、_2.svg）并上传 MinIO。
 * 以 products/p1_main.svg 是否存在做幂等判断。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedImageUploader implements ApplicationRunner, Ordered {

    private static final String CHECK_KEY = "products/p1_main.svg";
    private static final String[] VARIANTS = {"main", "1", "2"};

    /** 与 06-seed-product.sql 种子一致的 16 个英文品名 */
    private static final String[] NAMES = {
            "French Floral Midi Dress",
            "Ribbed Knit Bodycon Dress",
            "Puff Sleeve Chiffon Blouse",
            "Essential Cotton Crew Tee",
            "High Waist Wide Leg Pants",
            "Vintage Straight Fit Jeans",
            "Men Graphic Street Tee",
            "Men Striped Polo Shirt",
            "Men Oxford Casual Shirt",
            "Kids Cartoon Hoodie Set",
            "Lightweight Cushion Running Sneakers",
            "Retro Classic Canvas Shoes",
            "Mini Crossbody Square Bag",
            "Large Commuter Tote Bag",
            "Zircon Pendant Necklace",
            "Velvet Matte Lipstick"
    };

    /** 16 组时尚渐变双色 */
    private static final String[][] COLORS = {
            {"#7F7FD5", "#91EAE4"},
            {"#1F1C2C", "#928DAB"},
            {"#F7971E", "#FFD200"},
            {"#11998E", "#38EF7D"},
            {"#FC5C7D", "#6A82FB"},
            {"#4B79A1", "#283E51"},
            {"#373B44", "#4286F4"},
            {"#283C86", "#45A247"},
            {"#5C258D", "#4389A2"},
            {"#FF9966", "#FF5E62"},
            {"#00C6FF", "#0072FF"},
            {"#ED213A", "#93291E"},
            {"#AA076B", "#61045F"},
            {"#B79891", "#94716B"},
            {"#FFB75E", "#ED8F03"},
            {"#EC008C", "#FC6767"}
    };

    private final MinioClient minioClient;
    private final MinioProperties props;

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (objectExists(CHECK_KEY)) {
                log.info("[seed] product seed images already present, skip");
                return;
            }
            int count = 0;
            for (int i = 1; i <= NAMES.length; i++) {
                for (int v = 0; v < VARIANTS.length; v++) {
                    String key = "products/p" + i + "_" + VARIANTS[v] + ".svg";
                    byte[] bytes = buildSvg(i, v).getBytes(StandardCharsets.UTF_8);
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(key)
                            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                            .contentType("image/svg+xml")
                            .build());
                    count++;
                }
            }
            log.info("[seed] uploaded {} seed product images to bucket {}", count, props.getBucket());
        } catch (Exception e) {
            log.warn("[seed] seed image upload failed (will retry on next startup): {}", e.getMessage());
        }
    }

    private boolean objectExists(String key) throws Exception {
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(props.getBucket()).object(key).build());
            return true;
        } catch (ErrorResponseException e) {
            // NoSuchKey / NoSuchBucket 视为不存在
            return false;
        }
    }

    /** 720x960：渐变双色背景 + 居中英文品名 + 底部 KinnShop 水印。 */
    private String buildSvg(int productId, int variant) {
        String[] pair = COLORS[(productId - 1) % COLORS.length];
        String c1 = variant == 1 ? pair[1] : pair[0];
        String c2 = variant == 1 ? pair[0] : pair[1];
        // variant 2 改为纵向渐变，区分图册视觉
        String x2 = variant == 2 ? "0%" : "100%";
        String gradId = "g" + productId + "v" + variant;
        String label = variant == 0 ? "MAIN" : "STYLE 0" + variant;

        List<String> lines = wrap(NAMES[productId - 1]);
        StringBuilder tspans = new StringBuilder();
        int startY = 470 - (lines.size() - 1) * 30;
        for (int i = 0; i < lines.size(); i++) {
            tspans.append("<tspan x=\"360\" y=\"").append(startY + i * 60).append("\">")
                    .append(escapeXml(lines.get(i))).append("</tspan>");
        }

        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="720" height="960" viewBox="0 0 720 960">
                  <defs>
                    <linearGradient id="{GRAD}" x1="0%" y1="0%" x2="{X2}" y2="100%">
                      <stop offset="0%" stop-color="{C1}"/>
                      <stop offset="100%" stop-color="{C2}"/>
                    </linearGradient>
                  </defs>
                  <rect width="720" height="960" fill="url(#{GRAD})"/>
                  <circle cx="600" cy="150" r="220" fill="#FFFFFF" opacity="0.08"/>
                  <circle cx="110" cy="830" r="260" fill="#FFFFFF" opacity="0.06"/>
                  <text text-anchor="middle" fill="#FFFFFF" font-family="Arial, Helvetica, sans-serif"
                        font-size="46" font-weight="bold" letter-spacing="1">{TSPANS}</text>
                  <text x="360" y="565" text-anchor="middle" fill="#FFFFFF" opacity="0.8"
                        font-family="Arial, Helvetica, sans-serif" font-size="26" letter-spacing="6">{LABEL}</text>
                  <text x="360" y="916" text-anchor="middle" fill="#FFFFFF" opacity="0.5"
                        font-family="Arial, Helvetica, sans-serif" font-size="30" font-style="italic">KinnShop</text>
                </svg>
                """
                .replace("{GRAD}", gradId)
                .replace("{X2}", x2)
                .replace("{C1}", c1)
                .replace("{C2}", c2)
                .replace("{TSPANS}", tspans.toString())
                .replace("{LABEL}", label);
    }

    private static List<String> wrap(String name) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!cur.isEmpty() && cur.length() + word.length() + 1 > 16) {
                lines.add(cur.toString());
                cur = new StringBuilder();
            }
            if (!cur.isEmpty()) {
                cur.append(' ');
            }
            cur.append(word);
        }
        if (!cur.isEmpty()) {
            lines.add(cur.toString());
        }
        return lines;
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
