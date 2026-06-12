-- =====================================================
-- 种子数据：币种 / 汇率 / 类目 / 商品(SPU+i18n+图册+SKU) / 尺码表 / 闪购
-- 图片存 MinIO 对象 key（bucket: shop-images），M1 由脚本生成并上传
-- =====================================================
USE shop_product;

-- ---------- 币种 ----------
INSERT INTO `currency` (`code`, `symbol`, `name_zh`, `name_en`, `decimal_digits`, `sort`, `enabled`) VALUES
('USD', '$', '美元',   'US Dollar',      2, 1, 1),
('CNY', '¥', '人民币', 'Chinese Yuan',   2, 2, 1),
('EUR', '€', '欧元',   'Euro',           2, 3, 1),
('GBP', '£', '英镑',   'British Pound',  2, 4, 1),
('JPY', '¥', '日元',   'Japanese Yen',   0, 5, 1);

-- ---------- 汇率(初始 MANUAL，定时任务自动刷新为 API) ----------
INSERT INTO `exchange_rate` (`base_currency`, `quote_currency`, `rate`, `source`) VALUES
('USD', 'CNY', 7.25000000, 'MANUAL'),
('USD', 'EUR', 0.92000000, 'MANUAL'),
('USD', 'GBP', 0.79000000, 'MANUAL'),
('USD', 'JPY', 155.00000000, 'MANUAL');

-- ---------- 类目 ----------
INSERT INTO `category` (`id`, `parent_id`, `level`, `name_zh`, `name_en`, `sort`, `postal_tax_rate`) VALUES
(1, 0, 1, '女装',   'Women',       1, 20),
(2, 0, 1, '男装',   'Men',         2, 20),
(3, 0, 1, '童装',   'Kids',        3, 20),
(4, 0, 1, '鞋靴',   'Shoes',       4, 20),
(5, 0, 1, '箱包',   'Bags',        5, 20),
(6, 0, 1, '配饰',   'Accessories', 6, 20),
(7, 0, 1, '美妆',   'Beauty',      7, 50),
(11, 1, 2, '连衣裙', 'Dresses',        1, 20),
(12, 1, 2, '上衣',   'Tops & Tees',    2, 20),
(13, 1, 2, '裤装',   'Pants',          3, 20),
(21, 2, 2, 'T恤',    'Men T-Shirts',   1, 20),
(22, 2, 2, '衬衫',   'Men Shirts',     2, 20),
(31, 3, 2, '套装',   'Kids Sets',      1, 20),
(41, 4, 2, '运动鞋', 'Sneakers',       1, 20),
(51, 5, 2, '单肩包', 'Shoulder Bags',  1, 20),
(61, 6, 2, '项链',   'Necklaces',      1, 20),
(71, 7, 2, '彩妆',   'Makeup',         1, 50);

-- ---------- 尺码表 ----------
INSERT INTO `size_chart` (`category_id`, `locale`, `content_json`) VALUES
(11, 'zh-CN', '[{"尺码":"S","胸围cm":"84","腰围cm":"66","衣长cm":"110"},{"尺码":"M","胸围cm":"88","腰围cm":"70","衣长cm":"112"},{"尺码":"L","胸围cm":"92","腰围cm":"74","衣长cm":"114"},{"尺码":"XL","胸围cm":"96","腰围cm":"78","衣长cm":"116"}]'),
(11, 'en-US', '[{"Size":"S","Bust(in)":"33.1","Waist(in)":"26.0","Length(in)":"43.3"},{"Size":"M","Bust(in)":"34.6","Waist(in)":"27.6","Length(in)":"44.1"},{"Size":"L","Bust(in)":"36.2","Waist(in)":"29.1","Length(in)":"44.9"},{"Size":"XL","Bust(in)":"37.8","Waist(in)":"30.7","Length(in)":"45.7"}]'),
(21, 'zh-CN', '[{"尺码":"M","胸围cm":"100","衣长cm":"69"},{"尺码":"L","胸围cm":"104","衣长cm":"71"},{"尺码":"XL","胸围cm":"108","衣长cm":"73"},{"尺码":"XXL","胸围cm":"112","衣长cm":"75"}]'),
(21, 'en-US', '[{"Size":"M","Chest(in)":"39.4","Length(in)":"27.2"},{"Size":"L","Chest(in)":"40.9","Length(in)":"28.0"},{"Size":"XL","Chest(in)":"42.5","Length(in)":"28.7"},{"Size":"XXL","Chest(in)":"44.1","Length(in)":"29.5"}]');

-- ---------- 商品 SPU ----------
INSERT INTO `product` (`id`, `category_id`, `spu_code`, `brand`, `trade_mode`, `origin_country`, `main_image`, `min_price_cents`, `sales_count`, `rating_avg`, `rating_count`, `status`) VALUES
(1,  11, 'SPU0001', 'KINN',   'BONDED', 'CN', 'products/p1_main.svg',  2599, 1286, 4.8, 342, 1),
(2,  11, 'SPU0002', 'KINN',   'BONDED', 'CN', 'products/p2_main.svg',  1999, 2310, 4.7, 561, 1),
(3,  12, 'SPU0003', 'KINN',   'BONDED', 'CN', 'products/p3_main.svg',  1599, 876,  4.6, 198, 1),
(4,  12, 'SPU0004', 'BASIC+', 'BONDED', 'CN', 'products/p4_main.svg',  999,  5420, 4.9, 1203, 1),
(5,  13, 'SPU0005', 'KINN',   'BONDED', 'CN', 'products/p5_main.svg',  2299, 943,  4.7, 207, 1),
(6,  13, 'SPU0006', 'DENIMX', 'BONDED', 'CN', 'products/p6_main.svg',  2799, 1652, 4.8, 388, 1),
(7,  21, 'SPU0007', 'BASIC+', 'BONDED', 'CN', 'products/p7_main.svg',  1199, 3105, 4.8, 720, 1),
(8,  21, 'SPU0008', 'KINN',   'BONDED', 'CN', 'products/p8_main.svg',  1699, 689,  4.5, 134, 1),
(9,  22, 'SPU0009', 'KINN',   'BONDED', 'CN', 'products/p9_main.svg',  2199, 511,  4.6, 98,  1),
(10, 31, 'SPU0010', 'KIDDO',  'BONDED', 'CN', 'products/p10_main.svg', 1899, 1207, 4.9, 305, 1),
(11, 41, 'SPU0011', 'AEROFIT','BONDED', 'CN', 'products/p11_main.svg', 3299, 2876, 4.8, 644, 1),
(12, 41, 'SPU0012', 'RETROGO','BONDED', 'CN', 'products/p12_main.svg', 2499, 1432, 4.7, 311, 1),
(13, 51, 'SPU0013', 'KINN',   'BONDED', 'CN', 'products/p13_main.svg', 1799, 967,  4.6, 215, 1),
(14, 51, 'SPU0014', 'KINN',   'BONDED', 'CN', 'products/p14_main.svg', 2699, 734,  4.7, 169, 1),
(15, 61, 'SPU0015', 'LUMIA',  'DIRECT', 'KR', 'products/p15_main.svg', 899,  1855, 4.8, 423, 1),
(16, 71, 'SPU0016', 'VELVA',  'DIRECT', 'KR', 'products/p16_main.svg', 1299, 4210, 4.9, 980, 1);

-- ---------- 商品多语言 ----------
INSERT INTO `product_i18n` (`product_id`, `locale`, `name`, `subtitle`, `detail`) VALUES
(1, 'zh-CN', '法式碎花中长连衣裙', '约会通勤两相宜，垂坠显瘦', '<p>法式浪漫碎花，雪纺面料垂坠透气，收腰A字版型遮肉显瘦。春夏出游、约会、通勤皆宜。</p>'),
(1, 'en-US', 'French Floral Midi Dress', 'Flowy & flattering for dates and office', '<p>Romantic French floral print on breathable chiffon. Cinched waist with A-line silhouette flatters every figure. Perfect for spring outings, dates and work.</p>'),
(2, 'zh-CN', '修身罗纹针织连衣裙', '弹力罗纹，气质显身材', '<p>高弹罗纹针织面料，修身不紧绷，纯色百搭，单穿叠穿都好看。</p>'),
(2, 'en-US', 'Ribbed Knit Bodycon Dress', 'Stretchy rib fabric hugs your curves', '<p>High-stretch ribbed knit, body-hugging yet comfortable. Solid colors easy to style alone or layered.</p>'),
(3, 'zh-CN', '泡泡袖雪纺衬衫', '法式泡泡袖，温柔减龄', '<p>轻盈雪纺，泡泡袖设计修饰手臂线条，方领露出锁骨，温柔又减龄。</p>'),
(3, 'en-US', 'Puff Sleeve Chiffon Blouse', 'Romantic puff sleeves, square neckline', '<p>Lightweight chiffon with puff sleeves that flatter your arms. Square neckline highlights the collarbone.</p>'),
(4, 'zh-CN', '基础款纯棉圆领T恤', '100%纯棉，四色可选', '<p>100%精梳棉，亲肤透气，版型不挑人，多色可选的衣橱必备基础款。</p>'),
(4, 'en-US', 'Essential Cotton Crew Tee', '100% cotton wardrobe staple', '<p>100% combed cotton, soft and breathable. A true wardrobe essential in multiple colors.</p>'),
(5, 'zh-CN', '高腰垂感阔腿裤', '高腰显腿长，垂坠遮肉', '<p>高腰设计拉长腿部比例，垂坠面料显瘦遮肉，通勤休闲两相宜。</p>'),
(5, 'en-US', 'High Waist Wide Leg Pants', 'Leg-lengthening drape fit', '<p>High-rise waist elongates your legs; fluid drape fabric skims the body. Works for office and weekends.</p>'),
(6, 'zh-CN', '复古直筒牛仔裤', '微弹牛仔，复古水洗', '<p>复古水洗工艺，直筒版型不挑腿型，微弹面料久穿不变形。</p>'),
(6, 'en-US', 'Vintage Straight Fit Jeans', 'Retro wash, comfort stretch', '<p>Vintage washed denim with a universally flattering straight fit and comfort stretch that keeps its shape.</p>'),
(7, 'zh-CN', '男士潮流印花T恤', '纯棉舒适，街头风印花', '<p>纯棉面料，胸前原创街头风印花，宽松落肩版型，潮流百搭。</p>'),
(7, 'en-US', 'Men Graphic Street Tee', 'Original print, relaxed fit', '<p>Pure cotton tee with original streetwear graphic. Relaxed drop-shoulder fit for an effortless look.</p>'),
(8, 'zh-CN', '男士条纹翻领Polo衫', '商务休闲，透气珠地棉', '<p>珠地棉透气吸汗，经典条纹翻领设计，商务休闲皆宜。</p>'),
(8, 'en-US', 'Men Striped Polo Shirt', 'Breathable piqué cotton', '<p>Breathable piqué cotton polo with classic stripes. Smart-casual ready.</p>'),
(9, 'zh-CN', '男士牛津纺休闲衬衫', '免烫易打理，纽扣领', '<p>牛津纺面料挺括免烫，纽扣领设计，单穿内搭都有型。</p>'),
(9, 'en-US', 'Men Oxford Casual Shirt', 'Non-iron, button-down collar', '<p>Crisp non-iron oxford fabric with button-down collar. Sharp on its own or layered.</p>'),
(10, 'zh-CN', '儿童卡通连帽卫衣两件套', '加绒保暖，A类亲肤', '<p>A类婴幼儿标准面料，内里加绒，卡通印花连帽卫衣+长裤两件套。</p>'),
(10, 'en-US', 'Kids Cartoon Hoodie Set (2pcs)', 'Fleece-lined, Class A fabric', '<p>Class A baby-safe fabric with cozy fleece lining. Cartoon hoodie + pants two-piece set.</p>'),
(11, 'zh-CN', '轻量缓震跑步鞋', '蜂窝缓震，透气飞织', '<p>飞织鞋面透气包裹，蜂窝缓震中底回弹輕盈，日常通勤运动皆宜。</p>'),
(11, 'en-US', 'Lightweight Cushion Running Sneakers', 'Honeycomb cushioning, knit upper', '<p>Breathable knit upper with honeycomb cushioning midsole for a light, springy ride.</p>'),
(12, 'zh-CN', '复古经典帆布鞋', '硫化工艺，百搭板鞋', '<p>经典硫化工艺帆布鞋，复古配色，怎么搭都好看。</p>'),
(12, 'en-US', 'Retro Classic Canvas Shoes', 'Vulcanized sole, timeless look', '<p>Classic vulcanized canvas shoes in retro colorways. Goes with everything.</p>'),
(13, 'zh-CN', '单肩斜挎小方包', '链条装饰，质感五金', '<p>精致小方包，链条肩带可调节，金属五金质感满分，通勤约会百搭。</p>'),
(13, 'en-US', 'Mini Crossbody Square Bag', 'Chain strap, premium hardware', '<p>Chic mini square bag with adjustable chain strap and premium metal hardware.</p>'),
(14, 'zh-CN', '大容量通勤托特包', '软面PU，装得下A4', '<p>柔软PU面料，大容量可装A4文件与笔记本电脑，通勤出行首选。</p>'),
(14, 'en-US', 'Large Commuter Tote Bag', 'Soft PU, fits A4 & laptop', '<p>Soft PU leather tote that fits A4 documents and a laptop. Your everyday commuter companion.</p>'),
(15, 'zh-CN', '锆石吊坠锁骨链', '闪耀锆石，不易过敏', '<p>精选闪耀锆石，铜镀金链体不易过敏，日常叠戴优雅精致。</p>'),
(15, 'en-US', 'Zircon Pendant Necklace', 'Sparkling zircon, hypoallergenic', '<p>Sparkling zircon pendant on hypoallergenic gold-plated chain. Elegant alone or layered.</p>'),
(16, 'zh-CN', '丝绒哑光唇膏', '雾面质地，持久不掉色', '<p>丝绒雾面质地，一抹显色，持久不沾杯，三色可选。</p>'),
(16, 'en-US', 'Velvet Matte Lipstick', 'Long-lasting, transfer-proof', '<p>Velvety matte finish with rich payoff in one swipe. Transfer-proof and long-wearing, in 3 shades.</p>');

-- ---------- 商品图册 ----------
INSERT INTO `product_image` (`product_id`, `url`, `sort`)
SELECT id, CONCAT('products/p', id, '_main.svg'), 0 FROM `product`;
INSERT INTO `product_image` (`product_id`, `url`, `sort`)
SELECT id, CONCAT('products/p', id, '_1.svg'), 1 FROM `product`;
INSERT INTO `product_image` (`product_id`, `url`, `sort`)
SELECT id, CONCAT('products/p', id, '_2.svg'), 2 FROM `product`;

-- ---------- SKU(颜色×尺码) ----------
INSERT INTO `product_sku` (`product_id`, `sku_code`, `color`, `color_zh`, `size`, `price_cents`, `stock`, `image`, `weight_grams`) VALUES
-- P1 法式碎花连衣裙
(1, 'SKU0001-BLU-S',  'Floral Blue', '碎花蓝', 'S',  2599, 200, 'products/p1_main.svg', 350),
(1, 'SKU0001-BLU-M',  'Floral Blue', '碎花蓝', 'M',  2599, 300, 'products/p1_main.svg', 360),
(1, 'SKU0001-BLU-L',  'Floral Blue', '碎花蓝', 'L',  2599, 260, 'products/p1_main.svg', 370),
(1, 'SKU0001-BLU-XL', 'Floral Blue', '碎花蓝', 'XL', 2799, 150, 'products/p1_main.svg', 380),
(1, 'SKU0001-PNK-S',  'Floral Pink', '碎花粉', 'S',  2599, 180, 'products/p1_1.svg', 350),
(1, 'SKU0001-PNK-M',  'Floral Pink', '碎花粉', 'M',  2599, 240, 'products/p1_1.svg', 360),
(1, 'SKU0001-PNK-L',  'Floral Pink', '碎花粉', 'L',  2599, 210, 'products/p1_1.svg', 370),
(1, 'SKU0001-PNK-XL', 'Floral Pink', '碎花粉', 'XL', 2799, 120, 'products/p1_1.svg', 380),
-- P2 罗纹针织连衣裙
(2, 'SKU0002-BLK-S', 'Black', '黑色', 'S', 1999, 320, 'products/p2_main.svg', 320),
(2, 'SKU0002-BLK-M', 'Black', '黑色', 'M', 1999, 400, 'products/p2_main.svg', 330),
(2, 'SKU0002-BLK-L', 'Black', '黑色', 'L', 1999, 350, 'products/p2_main.svg', 340),
(2, 'SKU0002-BRN-S', 'Brown', '棕色', 'S', 1999, 260, 'products/p2_1.svg', 320),
(2, 'SKU0002-BRN-M', 'Brown', '棕色', 'M', 1999, 310, 'products/p2_1.svg', 330),
(2, 'SKU0002-BRN-L', 'Brown', '棕色', 'L', 1999, 270, 'products/p2_1.svg', 340),
-- P3 泡泡袖雪纺衬衫
(3, 'SKU0003-WHT-S', 'White',  '白色', 'S', 1599, 280, 'products/p3_main.svg', 200),
(3, 'SKU0003-WHT-M', 'White',  '白色', 'M', 1599, 330, 'products/p3_main.svg', 210),
(3, 'SKU0003-WHT-L', 'White',  '白色', 'L', 1599, 290, 'products/p3_main.svg', 220),
(3, 'SKU0003-YLW-S', 'Yellow', '黄色', 'S', 1599, 200, 'products/p3_1.svg', 200),
(3, 'SKU0003-YLW-M', 'Yellow', '黄色', 'M', 1599, 250, 'products/p3_1.svg', 210),
(3, 'SKU0003-YLW-L', 'Yellow', '黄色', 'L', 1599, 220, 'products/p3_1.svg', 220),
-- P4 基础纯棉T恤
(4, 'SKU0004-BLK-S',  'Black', '黑色', 'S',  999, 500, 'products/p4_main.svg', 180),
(4, 'SKU0004-BLK-M',  'Black', '黑色', 'M',  999, 600, 'products/p4_main.svg', 190),
(4, 'SKU0004-BLK-L',  'Black', '黑色', 'L',  999, 550, 'products/p4_main.svg', 200),
(4, 'SKU0004-BLK-XL', 'Black', '黑色', 'XL', 999, 400, 'products/p4_main.svg', 210),
(4, 'SKU0004-WHT-S',  'White', '白色', 'S',  999, 480, 'products/p4_1.svg', 180),
(4, 'SKU0004-WHT-M',  'White', '白色', 'M',  999, 580, 'products/p4_1.svg', 190),
(4, 'SKU0004-WHT-L',  'White', '白色', 'L',  999, 520, 'products/p4_1.svg', 200),
(4, 'SKU0004-WHT-XL', 'White', '白色', 'XL', 999, 380, 'products/p4_1.svg', 210),
(4, 'SKU0004-PNK-S',  'Pink',  '粉色', 'S',  999, 300, 'products/p4_2.svg', 180),
(4, 'SKU0004-PNK-M',  'Pink',  '粉色', 'M',  999, 360, 'products/p4_2.svg', 190),
(4, 'SKU0004-PNK-L',  'Pink',  '粉色', 'L',  999, 320, 'products/p4_2.svg', 200),
-- P5 高腰阔腿裤
(5, 'SKU0005-BLK-S', 'Black', '黑色', 'S', 2299, 240, 'products/p5_main.svg', 420),
(5, 'SKU0005-BLK-M', 'Black', '黑色', 'M', 2299, 300, 'products/p5_main.svg', 440),
(5, 'SKU0005-BLK-L', 'Black', '黑色', 'L', 2299, 260, 'products/p5_main.svg', 460),
(5, 'SKU0005-KHK-S', 'Khaki', '卡其', 'S', 2299, 200, 'products/p5_1.svg', 420),
(5, 'SKU0005-KHK-M', 'Khaki', '卡其', 'M', 2299, 250, 'products/p5_1.svg', 440),
(5, 'SKU0005-KHK-L', 'Khaki', '卡其', 'L', 2299, 210, 'products/p5_1.svg', 460),
-- P6 直筒牛仔裤
(6, 'SKU0006-LBL-S',  'Light Blue', '浅蓝', 'S',  2799, 220, 'products/p6_main.svg', 580),
(6, 'SKU0006-LBL-M',  'Light Blue', '浅蓝', 'M',  2799, 280, 'products/p6_main.svg', 600),
(6, 'SKU0006-LBL-L',  'Light Blue', '浅蓝', 'L',  2799, 240, 'products/p6_main.svg', 620),
(6, 'SKU0006-LBL-XL', 'Light Blue', '浅蓝', 'XL', 2999, 160, 'products/p6_main.svg', 640),
(6, 'SKU0006-DBL-S',  'Dark Blue',  '深蓝', 'S',  2799, 200, 'products/p6_1.svg', 580),
(6, 'SKU0006-DBL-M',  'Dark Blue',  '深蓝', 'M',  2799, 260, 'products/p6_1.svg', 600),
(6, 'SKU0006-DBL-L',  'Dark Blue',  '深蓝', 'L',  2799, 230, 'products/p6_1.svg', 620),
(6, 'SKU0006-DBL-XL', 'Dark Blue',  '深蓝', 'XL', 2999, 150, 'products/p6_1.svg', 640),
-- P7 男士印花T恤
(7, 'SKU0007-BLK-M',   'Black', '黑色', 'M',   1199, 450, 'products/p7_main.svg', 220),
(7, 'SKU0007-BLK-L',   'Black', '黑色', 'L',   1199, 520, 'products/p7_main.svg', 230),
(7, 'SKU0007-BLK-XL',  'Black', '黑色', 'XL',  1199, 480, 'products/p7_main.svg', 240),
(7, 'SKU0007-BLK-XXL', 'Black', '黑色', 'XXL', 1299, 300, 'products/p7_main.svg', 250),
(7, 'SKU0007-WHT-M',   'White', '白色', 'M',   1199, 420, 'products/p7_1.svg', 220),
(7, 'SKU0007-WHT-L',   'White', '白色', 'L',   1199, 490, 'products/p7_1.svg', 230),
(7, 'SKU0007-WHT-XL',  'White', '白色', 'XL',  1199, 450, 'products/p7_1.svg', 240),
(7, 'SKU0007-WHT-XXL', 'White', '白色', 'XXL', 1299, 280, 'products/p7_1.svg', 250),
-- P8 男士条纹Polo
(8, 'SKU0008-NVY-M',  'Navy',  '藏青', 'M',  1699, 260, 'products/p8_main.svg', 260),
(8, 'SKU0008-NVY-L',  'Navy',  '藏青', 'L',  1699, 310, 'products/p8_main.svg', 270),
(8, 'SKU0008-NVY-XL', 'Navy',  '藏青', 'XL', 1699, 270, 'products/p8_main.svg', 280),
(8, 'SKU0008-GRN-M',  'Green', '绿色', 'M',  1699, 220, 'products/p8_1.svg', 260),
(8, 'SKU0008-GRN-L',  'Green', '绿色', 'L',  1699, 260, 'products/p8_1.svg', 270),
(8, 'SKU0008-GRN-XL', 'Green', '绿色', 'XL', 1699, 230, 'products/p8_1.svg', 280),
-- P9 男士牛津衬衫
(9, 'SKU0009-WHT-M',   'White',      '白色', 'M',   2199, 240, 'products/p9_main.svg', 300),
(9, 'SKU0009-WHT-L',   'White',      '白色', 'L',   2199, 290, 'products/p9_main.svg', 310),
(9, 'SKU0009-WHT-XL',  'White',      '白色', 'XL',  2199, 250, 'products/p9_main.svg', 320),
(9, 'SKU0009-WHT-XXL', 'White',      '白色', 'XXL', 2299, 160, 'products/p9_main.svg', 330),
(9, 'SKU0009-LBL-M',   'Light Blue', '浅蓝', 'M',   2199, 220, 'products/p9_1.svg', 300),
(9, 'SKU0009-LBL-L',   'Light Blue', '浅蓝', 'L',   2199, 270, 'products/p9_1.svg', 310),
(9, 'SKU0009-LBL-XL',  'Light Blue', '浅蓝', 'XL',  2199, 230, 'products/p9_1.svg', 320),
-- P10 儿童卫衣套装
(10, 'SKU0010-GRY-110', 'Grey', '灰色', '110', 1899, 200, 'products/p10_main.svg', 380),
(10, 'SKU0010-GRY-120', 'Grey', '灰色', '120', 1899, 240, 'products/p10_main.svg', 400),
(10, 'SKU0010-GRY-130', 'Grey', '灰色', '130', 1999, 220, 'products/p10_main.svg', 420),
(10, 'SKU0010-GRY-140', 'Grey', '灰色', '140', 1999, 180, 'products/p10_main.svg', 440),
(10, 'SKU0010-PNK-110', 'Pink', '粉色', '110', 1899, 190, 'products/p10_1.svg', 380),
(10, 'SKU0010-PNK-120', 'Pink', '粉色', '120', 1899, 230, 'products/p10_1.svg', 400),
(10, 'SKU0010-PNK-130', 'Pink', '粉色', '130', 1999, 200, 'products/p10_1.svg', 420),
(10, 'SKU0010-PNK-140', 'Pink', '粉色', '140', 1999, 170, 'products/p10_1.svg', 440),
-- P11 跑步鞋
(11, 'SKU0011-WHT-38', 'White', '白色', '38', 3299, 180, 'products/p11_main.svg', 720),
(11, 'SKU0011-WHT-39', 'White', '白色', '39', 3299, 220, 'products/p11_main.svg', 740),
(11, 'SKU0011-WHT-40', 'White', '白色', '40', 3299, 260, 'products/p11_main.svg', 760),
(11, 'SKU0011-WHT-41', 'White', '白色', '41', 3299, 240, 'products/p11_main.svg', 780),
(11, 'SKU0011-WHT-42', 'White', '白色', '42', 3299, 200, 'products/p11_main.svg', 800),
(11, 'SKU0011-WHT-43', 'White', '白色', '43', 3299, 150, 'products/p11_main.svg', 820),
(11, 'SKU0011-BLK-39', 'Black', '黑色', '39', 3299, 200, 'products/p11_1.svg', 740),
(11, 'SKU0011-BLK-40', 'Black', '黑色', '40', 3299, 240, 'products/p11_1.svg', 760),
(11, 'SKU0011-BLK-41', 'Black', '黑色', '41', 3299, 220, 'products/p11_1.svg', 780),
(11, 'SKU0011-BLK-42', 'Black', '黑色', '42', 3299, 180, 'products/p11_1.svg', 800),
-- P12 帆布鞋
(12, 'SKU0012-WHT-38', 'White', '白色', '38', 2499, 200, 'products/p12_main.svg', 620),
(12, 'SKU0012-WHT-39', 'White', '白色', '39', 2499, 240, 'products/p12_main.svg', 640),
(12, 'SKU0012-WHT-40', 'White', '白色', '40', 2499, 220, 'products/p12_main.svg', 660),
(12, 'SKU0012-WHT-41', 'White', '白色', '41', 2499, 180, 'products/p12_main.svg', 680),
(12, 'SKU0012-RED-38', 'Red',   '红色', '38', 2499, 160, 'products/p12_1.svg', 620),
(12, 'SKU0012-RED-39', 'Red',   '红色', '39', 2499, 190, 'products/p12_1.svg', 640),
(12, 'SKU0012-RED-40', 'Red',   '红色', '40', 2499, 170, 'products/p12_1.svg', 660),
-- P13 小方包
(13, 'SKU0013-BLK-OS', 'Black', '黑色', 'One Size', 1799, 350, 'products/p13_main.svg', 380),
(13, 'SKU0013-CRM-OS', 'Cream', '奶油白', 'One Size', 1799, 300, 'products/p13_1.svg', 380),
-- P14 托特包
(14, 'SKU0014-BRN-OS', 'Brown', '棕色', 'One Size', 2699, 280, 'products/p14_main.svg', 560),
(14, 'SKU0014-BLK-OS', 'Black', '黑色', 'One Size', 2699, 320, 'products/p14_1.svg', 560),
-- P15 项链
(15, 'SKU0015-GLD-OS', 'Gold',   '金色', 'One Size', 899, 500, 'products/p15_main.svg', 50),
(15, 'SKU0015-SLV-OS', 'Silver', '银色', 'One Size', 899, 450, 'products/p15_1.svg', 50),
-- P16 口红
(16, 'SKU0016-RBY-OS', 'Ruby Red', '宝石红', 'One Size', 1299, 600, 'products/p16_main.svg', 80),
(16, 'SKU0016-CRL-OS', 'Coral',    '珊瑚色', 'One Size', 1299, 550, 'products/p16_1.svg', 80),
(16, 'SKU0016-NUD-OS', 'Nude',     '裸色',   'One Size', 1299, 500, 'products/p16_2.svg', 80);

-- ---------- 闪购 ----------
INSERT INTO `flash_sale` (`id`, `title`, `start_time`, `end_time`, `status`) VALUES
(1, '夏日闪购 Summer Flash Sale', '2026-06-01 00:00:00', '2026-09-30 23:59:59', 1);

INSERT INTO `flash_sale_item` (`sale_id`, `product_id`, `discount_percent`, `quota`, `sold`) VALUES
(1, 1,  30, 100, 0),
(1, 4,  20, 200, 0),
(1, 7,  25, 150, 0),
(1, 11, 30, 100, 0);
