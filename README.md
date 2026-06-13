# KinnShop 跨境电商系统（SHEIN 同型）

自营 B2C 时尚跨境电商，覆盖 **多语言 / 多币种 / 海外支付（Stripe + PayPal）/ 国际物流轨迹 / 清关与税费** 全链路。后端 Apache Dubbo 微服务，三端：买家商城 Web、运营管理后台、Flutter 移动 App。

> 定位：可演示、可继续生产化的完整应用。第三方渠道（Stripe/PayPal）按生产标准接入，开发期走沙箱 + 内置模拟渠道兜底。

---

## 架构总览

```
                          ┌─────────────┐  ┌─────────────┐  ┌──────────────┐
                          │  mall-web   │  │  admin-web  │  │  shop_app    │
                          │ 买家商城:5183│  │ 管理后台:5184│  │ Flutter 移动端│
                          └──────┬──────┘  └──────┬──────┘  └──────┬───────┘
                                 └────────────────┼────────────────┘
                                          ┌───────▼────────┐
                                          │  shop-gateway  │ :9600  Spring Cloud Gateway
                                          │  Sa-Token 鉴权 │        + 可信头注入(X-User-Id)
                                          └───────┬────────┘
        ┌──────────────┬─────────────────┬───────┼────────────┬─────────────────┐
   ┌────▼────┐   ┌─────▼─────┐   ┌────────▼───┐ ┌─▼────────┐ ┌─▼──────────┐
   │shop-user│   │shop-product│   │ shop-order │ │shop-pay  │ │shop-logist.│
   │  :9601  │   │   :9602    │   │   :9603    │ │  :9604   │ │   :9605    │
   └────┬────┘   └─────┬──────┘   └─────┬──────┘ └────┬─────┘ └─────┬──────┘
        └──────── Dubbo RPC (Nacos 注册中心) 服务间互调 ──────────────┘
                                 │
   ┌─────────────┬──────────────┼──────────────┬─────────────┐
┌──▼──┐      ┌───▼───┐     ┌────▼────┐    ┌─────▼────┐  ┌─────▼─────┐
│MySQL│      │ Redis │     │  Nacos  │    │  MinIO   │  │frankfurter│
│13307│      │ 16380 │     │  8950   │    │  19000   │  │ (汇率API) │
└─────┘      └───────┘     └─────────┘    └──────────┘  └───────────┘
```

- **服务间通信**：Apache Dubbo 3.3 RPC（接口契约集中在 `shop-api` 模块），Nacos 作注册中心
- **鉴权**：Sa-Token 会话存 Redis，网关统一校验后剥离伪造头、注入可信 `X-User-Id`/`X-User-Type`，下游用 `LoginContext` 还原；买家 `u_` / 管理员 `a_` 前缀隔离
- **最终一致性**：本地消息表 + 定时补偿 + Redisson 锁（未引入 MQ/Seata）
- **金额**：一律 BIGINT「USD 分」存储，展示/支付按下单锁定汇率换算

## 技术栈

| 层 | 选型 |
|---|---|
| 后端 | JDK 21 · Spring Boot 3.5.5 · Apache Dubbo 3.3.4 · MyBatis-Plus 3.5.12 · Sa-Token 1.39 · Redisson · Knife4j |
| 网关 | Spring Cloud Gateway 2025.0（profile 静态路由，不依赖 Spring Cloud Alibaba） |
| 存储 | MySQL 8（5 库分服务）· Redis 7 · MinIO（S3 兼容，商品/评论图） |
| 支付 | stripe-java SDK + PayPal Server REST（沙箱）+ 内置 SimulatorChannel（`@ConditionalOnProperty` 开关） |
| 买家/管理端 | Vue3 · Vite · Pinia · vue-i18n · Element Plus · ECharts |
| 移动端 | Flutter 3.44 · Dart 3.12 · dio · provider · intl |

## 目录结构

```
跨境电商/
├── docker/                 # docker-compose(mysql/redis/nacos/minio) + init/ 五库 DDL+种子
├── backend/                # Maven 多模块 groupId com.kinn.shop
│   ├── shop-common/        # Result/异常/BaseEntity/LoginContext/i18n/币种工具/RedisKeys
│   ├── shop-api/           # 全部 Dubbo 接口 + DTO（服务间唯一共享）
│   ├── shop-gateway/       # 路由 + Sa-Token 鉴权 + 头注入
│   ├── shop-user/          # 用户/邮箱注册/地址/清关实名(AES)/管理员
│   ├── shop-product/       # 类目税率/SPU/SKU/多语言/汇率/MinIO/评论/收藏/闪购/推荐
│   ├── shop-order/         # 购物车/优惠券/税费引擎/订单状态机/超时关单/发货/确认收货
│   ├── shop-payment/       # 收银台/Stripe/PayPal/模拟器/三层幂等/退款/本地消息表
│   └── shop-logistics/     # 运费模板/物流单/轨迹推进(含清关)
├── mall-web/               # 买家商城 (Vue3, :5183)
├── admin-web/              # 运营后台 (Vue3 + EP + ECharts, :5184)
├── shop_app/               # Flutter 移动端 (Web 调试 :5185)
└── scripts/                # e2e.sh 全链路验收 / start-backend.ps1 一键起服务
```

数据库分库：`shop_user` / `shop_product` / `shop_order` / `shop_payment` / `shop_logistics`。

## 跨境特色

1. **多语言**：站点中/英切换；商品 `product_i18n` 按 locale 出参（缺失回退 en-US）；后端错误码按 `Accept-Language` i18n
2. **多币种**：USD 基准定价，`exchange_rate` 表（frankfurter.app 每日刷新 + 管理端手工覆盖 + Redis 缓存）；**下单锁定汇率**快照存订单
3. **清关与税费**（`tax_rule` 表，结算实时试算并分项展示）：
   - 中国跨境：保税仓 = 跨境综合税 9.1%；海外直邮 = 行邮税 13/20/50% 按类目，税额 ≤ 50 元免征
   - 其他国：VAT（英 20% / 德 19% / 日 10% …）、美国 de minimis 800 USD 免税
   - **订购人实名**：身份证校验位本地校验 + AES 加密存储 + 脱敏展示，CN 订单清关强制
4. **海外支付**：统一收银台 → Stripe(卡) / PayPal / 模拟器；webhook 验签 + 主动查询兜底；三层幂等（Redis SETNX → DB 唯一索引 → 状态 CAS）；全额/部分退款
5. **国际物流**：运费模板（国家区域 + 首重续重）；轨迹自动推进 揽收→出口报关→国际干线→进口清关(关税核验)→末端派送→签收；签收驱动订单完成

## 快速开始

### 1. 基础设施（需 Docker Desktop 已启动）

```powershell
cd docker
docker compose up -d          # MySQL:13307 / Redis:16380 / Nacos:8950 / MinIO:19000
```

五库 DDL 与种子（17 类目 / 16 SPU 中英双语 / 100 SKU / 税则 / 运费 / 优惠券 / 闪购）随容器首次启动自动导入。

### 2. 后端（JDK 21）

```powershell
$env:JAVA_HOME = 'D:\0-kinn\9-software\0-Dev\0-Jdk\jdk21'
cd backend
mvn -DskipTests clean install
# 一键起 6 个服务（后台）：
powershell -ExecutionPolicy Bypass -File ..\scripts\start-backend.ps1
```

各服务 Knife4j 文档：`http://localhost:9601/doc.html`（user）、`:9602`（product）… 以此类推。

### 3. 前端

```powershell
cd mall-web  && npm install && npm run dev     # 买家商城 http://localhost:5183
cd admin-web && npm install && npm run dev     # 管理后台 http://localhost:5184  (admin / 123456)
```

### 4. 移动端（Flutter）

```powershell
$env:Path = "D:\0-kinn\9-software\0-Dev\flutter\bin;$env:Path"
cd shop_app
flutter pub get
flutter run -d chrome --web-port=5185          # 浏览器调试；或 flutter build apk 出安装包
```

## 配置与密钥（生产规范）

敏感配置全部通过环境变量注入，`.env` 不入库，模板见 [`.env.example`](.env.example)：

- **Stripe/PayPal**：`STRIPE_ENABLED=true` + `STRIPE_API_KEY`/`STRIPE_WEBHOOK_SECRET`，`PAYPAL_ENABLED=true` + `PAYPAL_CLIENT_ID`/`PAYPAL_CLIENT_SECRET`（沙箱）。**均未启用时自动使用内置模拟渠道**，保证链路可演示。
- **SMTP**：配置 `MAIL_HOST` 后真实发注册验证码；dev 默认 `shop.mail.mock=true` 打印到后端控制台。
- **身份证 AES 密钥**：`IDENTITY_AES_KEY`（16/24/32 字节）。

## 演示路径

| 端 | 路径 |
|---|---|
| 买家商城 | 注册/登录 → 首页闪购 → 商品详情(选色码) → 加购 → 结算(地址+实名+税费) → 收银台 → 模拟支付 → 订单轨迹 → 评价 |
| 管理后台 | admin/123456 → 仪表盘(ECharts) → 商品/类目/库存 → 订单发货 → 支付流水退款 → 汇率刷新 → 评论审核 |
| 移动端 | 首页/分类/购物车/我的 四 tab → 详情下单 → 模拟支付 → 订单物流 |

## 后端全链路自测

```bash
bash scripts/e2e.sh          # 12 环节 18 断言：注册→浏览→加购→税费试算→下单→支付→发货→轨迹→签收→评论
```

## 端口一览

| 组件 | 端口 | | 组件 | 端口 |
|---|---|---|---|---|
| MySQL | 13307 | | gateway | 9600 |
| Redis | 16380 | | user / product / order / payment / logistics | 9601–9605 |
| Nacos | 8950 (gRPC 9950) | | mall-web / admin-web | 5183 / 5184 |
| MinIO | 19000 (控制台 19001) | | Flutter Web 调试 | 5185 |

## 关键技术决策（经得起追问）

- **金额 BIGINT 分**：杜绝浮点误差；多币种用基准币 USD 分存储，下单锁汇率快照
- **库存防超卖**：Redis 不在热路径，扣减用 DB 乐观锁 `stock>=q` + version；超时未支付定时关单回滚库存与优惠券（Redisson 锁防并发关单与支付回调竞争）
- **支付幂等**：Redis SETNX 前置 + `pay_notify_log` 唯一索引兜底 + 支付单状态 CAS；通知订单失败落本地消息表，定时任务指数退避重试，8 次转死信
- **订单状态机**：自实现 CAS 流转（`WHERE status=from`），不引入状态机框架；全程流转日志
- **服务边界**：分库 + Dubbo 接口契约集中 `shop-api`，DTO 全部 `Serializable`

## 许可

仅用于学习与演示。
