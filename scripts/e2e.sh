#!/usr/bin/env bash
# =====================================================================
# KinnShop 后端全链路 E2E（演示渠道）
# 链路：注册 → 浏览(多语言/多币种) → 加购 → 领券 → 地址/实名 → 试算 →
#       下单(锁汇率/扣库存) → 模拟器支付 → 订单PAID → 管理端发货 →
#       轨迹推进(含清关) → 签收 → 订单FINISHED → 评论 → 审核 → 评分聚合
# 用法：bash scripts/e2e.sh [gateway_base]   默认 http://localhost:9600
# 依赖：curl、python(3)、docker(查库断言)
# =====================================================================
set -u
BASE="${1:-http://localhost:9600}"
MYSQL="docker exec shop-mysql mysql -uroot -p123456 -N -e"
PASS=0; FAIL=0
TS=$(date +%s)
EMAIL="e2e_${TS}@kinn.dev"

say()  { printf '\n\033[1;36m== %s ==\033[0m\n' "$*"; }
ok()   { PASS=$((PASS+1)); printf '\033[32mPASS\033[0m %s\n' "$*"; }
bad()  { FAIL=$((FAIL+1)); printf '\033[31mFAIL\033[0m %s\n' "$*"; }
assert_eq() { # assert_eq <desc> <expect> <actual>
  if [ "$2" = "$3" ]; then ok "$1 ($3)"; else bad "$1 expect=[$2] actual=[$3]"; fi
}
jget() { # jget <json> <python-expr over d>
  python -X utf8 -c "import json,sys; d=json.loads(sys.argv[1]); print($2)" "$1" 2>/dev/null
}

say "0. 服务健康"
n=0; for p in 9600 9601 9602 9603 9604 9605; do (exec 3<>/dev/tcp/127.0.0.1/$p) 2>/dev/null && n=$((n+1)); done
assert_eq "6 服务端口监听" 6 "$n"

say "1. 邮箱注册（验证码走控制台 mock）"
curl -s -X POST "$BASE/api/auth/email-code" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"scene\":\"register\"}" >/dev/null
sleep 1
CODE=$(grep -o "$EMAIL code=[0-9]*" backend/logs/user.log | tail -1 | grep -o '[0-9]*$')
REG=$(curl -s -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"code\":\"$CODE\",\"password\":\"E2e@12345\",\"nickname\":\"E2E\"}")
TOKEN=$(jget "$REG" "d['data']['token']")
[ -n "$TOKEN" ] && ok "注册并登录 token 获取" || bad "注册失败: $REG"
AUTH=(-H "satoken: $TOKEN")

say "2. 多语言/多币种浏览"
NAME_ZH=$(curl -s "$BASE/api/product/1?locale=zh-CN&currency=CNY" | python -X utf8 -c "import json,sys; print(json.load(sys.stdin)['data']['name'])")
NAME_EN=$(curl -s "$BASE/api/product/1?locale=en-US&currency=EUR" | python -X utf8 -c "import json,sys; print(json.load(sys.stdin)['data']['name'])")
assert_eq "中文名" "法式碎花中长连衣裙" "$NAME_ZH"
assert_eq "英文名" "French Floral Midi Dress" "$NAME_EN"

say "3. 加购 + 领券"
curl -s -X POST "$BASE/api/cart" "${AUTH[@]}" -H "Content-Type: application/json" -d '{"skuId":2,"quantity":2}' >/dev/null
CART=$(curl -s "$BASE/api/cart/count" "${AUTH[@]}")
assert_eq "购物车数量" 2 "$(jget "$CART" "d['data']['count'] if isinstance(d['data'],dict) else d['data']")"
curl -s -X POST "$BASE/api/coupon/1/claim" "${AUTH[@]}" >/dev/null
UC=$(curl -s "$BASE/api/coupon/mine?status=0" "${AUTH[@]}")
UCID=$(jget "$UC" "(d['data'] if isinstance(d['data'],list) else d['data']['list'])[0]['id']")
[ -n "$UCID" ] && ok "新人券领取 userCouponId=$UCID" || bad "领券失败"

say "4. 地址 + 实名"
curl -s -X POST "$BASE/api/address" "${AUTH[@]}" -H "Content-Type: application/json" \
  -d '{"receiverName":"E2E Buyer","phone":"+86 13900000000","countryCode":"CN","state":"Zhejiang","city":"Hangzhou","addressLine1":"No.1 West Lake Ave","postcode":"310000","isDefault":true}' >/dev/null
AID=$(curl -s "$BASE/api/address/list" "${AUTH[@]}" | python -X utf8 -c "import json,sys; print(json.load(sys.stdin)['data'][0]['id'])")
IDNO=$(python -c "
b='33010619950101001'
w=[7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2]
print(b+'10X98765432'[sum(int(x)*y for x,y in zip(b,w))%11])")
curl -s -X POST "$BASE/api/identity" "${AUTH[@]}" -H "Content-Type: application/json" \
  -d "{\"realName\":\"E2E Buyer\",\"idCardNo\":\"$IDNO\"}" >/dev/null
ok "地址($AID) 与实名(校验位合法) 创建"

say "5. 结算试算（CNY，含保税税费）"
PREVIEW=$(curl -s -X POST "$BASE/api/checkout/preview" "${AUTH[@]}" -H "Content-Type: application/json" \
  -d "{\"addressId\":$AID,\"fromCart\":true,\"userCouponId\":$UCID,\"currency\":\"CNY\",\"locale\":\"zh-CN\"}")
TAX=$(jget "$PREVIEW" "d['data']['tax']['usdCents']")
IDREQ=$(jget "$PREVIEW" "d['data']['identityRequired']")
[ "$IDREQ" = "True" ] && ok "CN 清关实名要求触发" || bad "identityRequired=$IDREQ"
[ -n "$TAX" ] && [ "$TAX" -gt 0 ] && ok "税费计算 tax=${TAX}分(USD)" || bad "税费异常: $TAX"

say "6. 下单（锁汇率）"
ORDER=$(curl -s -X POST "$BASE/api/order/create" "${AUTH[@]}" -H "Content-Type: application/json" \
  -d "{\"addressId\":$AID,\"fromCart\":true,\"userCouponId\":$UCID,\"payCurrency\":\"CNY\",\"locale\":\"zh-CN\"}")
ONO=$(jget "$ORDER" "d['data']['orderNo']")
PAYAMT=$(jget "$ORDER" "d['data']['payAmountMinor']")
[ -n "$ONO" ] && ok "下单 $ONO 应付(CNY分)=$PAYAMT" || bad "下单失败: $ORDER"

say "7. 模拟器支付 → 回调入账"
PAY=$(curl -s -X POST "$BASE/api/pay/create" "${AUTH[@]}" -H "Content-Type: application/json" \
  -d "{\"orderNo\":\"$ONO\",\"channel\":\"SIMULATOR\"}")
PNO=$(jget "$PAY" "d['data']['payNo']")
curl -s -X POST "$BASE/api/pay/notify/simulator" -H "Content-Type: application/json" \
  -d "{\"payNo\":\"$PNO\",\"result\":\"SUCCESS\",\"eventId\":\"SIMEVT-$PNO-SUCCESS\"}" >/dev/null
sleep 2
OS=$($MYSQL "SELECT status FROM shop_order.orders WHERE order_no='$ONO';" 2>/dev/null)
assert_eq "支付后订单状态" "PAID" "$OS"

say "8. 管理端发货"
ADM=$(curl -s -X POST "$BASE/api/admin/auth/login" -H "Content-Type: application/json" -d '{"username":"admin","password":"123456"}')
AT=$(jget "$ADM" "d['data']['token']")
SHIP=$(curl -s -X POST "$BASE/api/admin/order/$ONO/ship" -H "satoken: $AT")
SNO=$(jget "$SHIP" "d['data']['shipmentNo']")
[ -n "$SNO" ] && ok "发货建单 $SNO" || bad "发货失败: $SHIP"

say "9. 轨迹推进（含出口报关/进口清关，约 2.5~3 分钟到签收）"
SIGNED=""
for i in $(seq 1 40); do
  sleep 10
  TRACK=$(curl -s "$BASE/api/logistics/track/$ONO" "${AUTH[@]}")
  NODE=$(jget "$TRACK" "d['data']['currentNode']")
  printf '  t+%02ds currentNode=%s\n' $((i*10)) "$NODE"
  if [ "$NODE" = "SIGNED" ]; then SIGNED=1; break; fi
done
[ -n "$SIGNED" ] && ok "物流签收" || bad "签收超时"
NODES=$(jget "$TRACK" "len(d['data']['tracks'])")
assert_eq "轨迹节点数(6)" 6 "$NODES"

say "10. 订单自动完成"
sleep 3
OS2=$($MYSQL "SELECT status FROM shop_order.orders WHERE order_no='$ONO';" 2>/dev/null)
assert_eq "签收后订单状态" "FINISHED" "$OS2"

say "11. 评论 → 审核 → 评分聚合"
# 注意：购物车里 skuId=2 属于商品 1（颜色/尺码 SKU），评论对象是商品 1
RC_BEFORE=$($MYSQL "SELECT rating_count FROM shop_product.product WHERE id=1;" 2>/dev/null)
curl -s -X POST "$BASE/api/review" "${AUTH[@]}" -H "Content-Type: application/json" \
  -d "{\"orderNo\":\"$ONO\",\"productId\":1,\"rating\":5,\"content\":\"Great quality, true to size!\",\"images\":[]}" >/dev/null
RID=$($MYSQL "SELECT id FROM shop_product.product_review WHERE order_no='$ONO' AND product_id=1;" 2>/dev/null)
[ -n "$RID" ] && ok "评论提交 id=$RID" || bad "评论提交失败"
curl -s -X PUT "$BASE/api/admin/review/$RID/audit" -H "satoken: $AT" -H "Content-Type: application/json" -d '{"approve":true}' >/dev/null
RC_AFTER=$($MYSQL "SELECT rating_count FROM shop_product.product WHERE id=1;" 2>/dev/null)
assert_eq "审核通过后评分数+1" "$((RC_BEFORE+1))" "$RC_AFTER"

say "12. 推荐接口"
REC=$(curl -s "$BASE/api/product/recommend?productId=1&locale=en-US&currency=USD&limit=4")
RN=$(jget "$REC" "len(d['data'])")
[ -n "$RN" ] && [ "$RN" -ge 1 ] && ok "同类目推荐返回 $RN 条" || bad "推荐为空"

printf '\n\033[1m========== E2E 结果: PASS=%d FAIL=%d ==========\033[0m\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
