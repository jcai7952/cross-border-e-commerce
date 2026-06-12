# KinnShop 跨境电商（SHEIN 同型）

自营 B2C 时尚跨境电商系统：多语言 / 多币种 / 海外支付（Stripe + PayPal）/ 国际物流跟踪 / 清关与税费。

> 文档完善中，随里程碑迭代更新。完整启动与配置手册见 M7 收尾版本。

## 架构

- **backend/** — JDK 21 · Spring Boot 3.5 · Apache Dubbo 3.3 微服务（Nacos 注册）· MyBatis-Plus · Redis/Redisson · MinIO
  - `shop-gateway` :9600 — Spring Cloud Gateway，Sa-Token 鉴权
  - `shop-user` :9601 — 用户 / 邮箱注册 / 地址 / 实名
  - `shop-product` :9602 — 类目 / SPU / SKU(颜色×尺码) / 多语言 / 汇率 / 评论 / 闪购
  - `shop-order` :9603 — 购物车 / 优惠券 / 结算(运费+税费+汇率) / 订单
  - `shop-payment` :9604 — 收银台 / Stripe / PayPal / 退款
  - `shop-logistics` :9605 — 运费模板 / 物流轨迹 / 清关
- **mall-web/** :5183 — 买家商城（Vue3 + Vite + Pinia + vue-i18n）
- **admin-web/** :5184 — 管理后台（Vue3 + Element Plus + ECharts）
- **shop-uniapp/** :5185(H5) — 移动端（uni-app Vue3，可编译微信小程序）

## 基础设施（docker/）

| 组件 | 宿主机端口 | 说明 |
|---|---|---|
| MySQL 8 | 13307 | 5 个业务库，DDL+种子在 `docker/init/` |
| Redis 7 | 16380 | 缓存 / 库存预扣 / 幂等 / 验证码 |
| Nacos 2 | 8950 (gRPC 9950) | Dubbo 注册中心，standalone |
| MinIO | 19000 (控制台 19001) | 商品图 / 评论图，S3 兼容 |

```bash
cd docker && docker compose up -d
```

## 快速开始（开发环境）

```bash
# 1. 启动基础设施（需 Docker Desktop）
cd docker && docker compose up -d

# 2. 后端（JAVA_HOME 指向 JDK 21）
cd backend && mvn spring-boot:run -pl shop-gateway   # 依次启动各服务

# 3. 前端
cd mall-web && npm i && npm run dev
```

敏感配置（Stripe/PayPal 密钥、SMTP）通过环境变量注入，参见 `.env.example`，密钥不入库。
