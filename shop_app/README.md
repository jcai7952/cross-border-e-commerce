# shop_app — 跨境电商移动端（对标 SHEIN 核心购物链路）

Flutter (Material 3) 客户端，对接后端 API 网关 `http://localhost:9600`。
支持中英文 + 多币种（USD/CNY/EUR/GBP/JPY），金额一律使用后端返回的展示文本。

## 启动（PowerShell）

```powershell
$env:PUB_HOSTED_URL='https://pub.flutter-io.cn'
$env:FLUTTER_STORAGE_BASE_URL='https://storage.flutter-io.cn'
$env:Path="D:\0-kinn\9-software\0-Dev\flutter\bin;$env:Path"
cd C:\Users\b1370\Desktop\跨境电商\shop_app

# 开发运行（Chrome）
flutter run -d chrome --web-port=5185

# 静态分析（注意：因工作目录含中文，flutter analyze 的 LSP 会崩，请用 dart analyze）
dart analyze lib test

# 生产构建（产出 build\web）
flutter build web
```

测试账号：`buyer2@kinn.dev` / `Passw0rd!`
注册验证码：dev 环境打印在后端控制台日志（页面有提示）。

## 后端基址与 CORS

`lib/core/api_client.dart` 中 `const String kApiBase = 'http://localhost:9600'`。
网关已对 `localhost:*` 开启 CORS。`flutter run -d chrome` 端口不固定，建议用
`--web-port=5185` 固定端口，便于与网关白名单对齐；如遇 CORS，确认网关放行该端口。

## 目录结构（lib 分层）

- `core/` — `api_client.dart`（dio 单例 + satoken/Accept-Language 注入 + {code,data} 解包）、
  `app_state.dart`（provider/ChangeNotifier：token/user/locale/currency/cartCount，shared_preferences 持久化）、
  `i18n.dart`（中英文词条 + `t(key)`）
- `models/` — `money`/`product`/`cart`/`address`/`checkout`/`order`/`logistics`
- `api/services.dart` — 各域 service（Auth/Catalog/Cart/Address/Checkout/Pay/Order/Logistics）
- `widgets/` — `common.dart`（网络图占位、空态/错误态、toast）、`product_card.dart`（商品卡 + 价格组件）
- `pages/` — 登录/注册、主框架(4 tab)、首页、分类、购物车、我的、商品详情、搜索结果、
  结算、支付、订单列表/详情(物流时间线)、地址管理、实名管理

## 支付说明（演示）

下单后进入支付页，点击「确认支付（模拟）」：调用 `POST /api/pay/create` 取 payNo →
`POST /api/pay/notify/simulator`(SUCCESS) 触发模拟支付成功回调 → `POST /api/pay/{payNo}/sync`
兜底 + `GET /api/pay/{payNo}` 轮询状态 → 成功后展示并跳订单详情。无需外部浏览器/WebView。
