package com.kinn.shop.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.api.product.ProductTradeFacade;
import com.kinn.shop.api.product.dto.RateDTO;
import com.kinn.shop.order.entity.TaxRule;
import com.kinn.shop.order.mapper.TaxRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 进口税试算（核心规则引擎）。
 *
 * <p>税则按目的国唯一配置（tax_rule.country_code），三种模型：
 * <ul>
 *   <li><b>CN_CROSS_BORDER</b> 中国跨境电商进口：
 *     <ul>
 *       <li>计税基数按「券后商品净额」：每件商品净额 itemNet = itemTotal × goodsAfterDiscount / goodsTotal
 *           等比例分摊，最后一件取差额，保证 Σ itemNet == goodsAfterDiscount，不漏分不重分；</li>
 *       <li>运费同样按净额比例分摊到每件（最后一件取差额）；</li>
 *       <li>BONDED（保税仓发货）件：跨境综合税 =（itemNet + 该件分摊运费）× 9.1%
 *           （= 增值税13% × 70%，零关税，2026 现行政策简化模型）；</li>
 *       <li>DIRECT（海外直邮）件：行邮税 = itemNet × 类目行邮税率（13/20/50%），运费不计税；
 *           DIRECT 部分税额合计 ≤ 50 元人民币等值（按实时汇率折 USD 分）则全部免征——
 *           即行邮税 50 元免征额政策；CNY 汇率不可得时保守起见不免征；</li>
 *       <li>金额运算全程 USD 分，HALF_UP 取整到分。</li>
 *     </ul>
 *   </li>
 *   <li><b>VAT</b>：税额 =（券后商品净额 + 运费）× rate_percent%（欧盟/英/日/澳/新等价税模型）。</li>
 *   <li><b>NONE</b>：0 税。threshold_cents 为 de minimis 免税额度备注字段
 *       （如美国 800 USD），超额后的关税申报不在本期建模范围。</li>
 * </ul>
 * 目的国无税则记录时视为 NONE（0 税、不需实名）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxService {

    /** 跨境综合税率 9.1% */
    private static final BigDecimal BONDED_RATE = new BigDecimal("0.091");
    /** 行邮税免征额：50 元人民币（折 USD 分比较） */
    private static final BigDecimal POSTAL_EXEMPT_CNY_CENTS = BigDecimal.valueOf(5000);

    private final TaxRuleMapper taxRuleMapper;
    private final ProductTradeFacade productTradeFacade;

    /** 单件计税明细：贸易模式 + 类目行邮税率 + 成交小计（USD 分）。 */
    public record TaxItem(String tradeMode, int postalTaxRate, long itemTotalCents) {
    }

    /** 计税结果：税额（USD 分）+ 是否需订购人实名 + 简短中文说明。 */
    public record TaxResult(long taxCents, boolean identityRequired, String taxNote) {
    }

    /**
     * @param countryCode        目的国二位码
     * @param items              按明细行的计税信息（itemTotalCents 为券前成交小计）
     * @param goodsAfterDiscount 券后商品净额 USD 分（= goods - discount）
     * @param shippingCents      运费 USD 分
     */
    public TaxResult calculate(String countryCode, List<TaxItem> items,
                               long goodsAfterDiscount, long shippingCents) {
        TaxRule rule = taxRuleMapper.selectOne(Wrappers.<TaxRule>lambdaQuery()
                .eq(TaxRule::getCountryCode, countryCode));
        if (rule == null) {
            return new TaxResult(0, false, "免税");
        }
        boolean identityRequired = rule.getIdentityRequired() != null && rule.getIdentityRequired() == 1;
        return switch (rule.getTaxType()) {
            case "CN_CROSS_BORDER" -> calcCnCrossBorder(items, goodsAfterDiscount, shippingCents, identityRequired);
            case "VAT" -> calcVat(rule, goodsAfterDiscount, shippingCents, identityRequired);
            // NONE：de minimis 免税额（threshold_cents）以下免税；超额关税不在本期范围
            default -> new TaxResult(0, identityRequired, "免税");
        };
    }

    /** VAT：(净额 + 运费) × rate_percent%，HALF_UP 到分。 */
    private TaxResult calcVat(TaxRule rule, long goodsAfterDiscount, long shippingCents, boolean identityRequired) {
        long tax = BigDecimal.valueOf(goodsAfterDiscount + shippingCents)
                .multiply(rule.getRatePercent())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValueExact();
        String note = rule.getCountryCode() + " VAT " + rule.getRatePercent().stripTrailingZeros().toPlainString() + "%";
        return new TaxResult(tax, identityRequired, note);
    }

    /** 中国跨境：保税件综合税 9.1%（含分摊运费），直邮件行邮税（≤50 CNY 免征）。 */
    private TaxResult calcCnCrossBorder(List<TaxItem> items, long goodsAfterDiscount,
                                        long shippingCents, boolean identityRequired) {
        int n = items.size();
        if (n == 0 || goodsAfterDiscount <= 0) {
            return new TaxResult(0, identityRequired, "免税");
        }
        long goodsTotal = items.stream().mapToLong(TaxItem::itemTotalCents).sum();

        // 1) 券后净额按成交小计等比例分摊；最后一件用差额，防分摊误差
        long[] nets = new long[n];
        long allocatedNet = 0;
        for (int i = 0; i < n; i++) {
            if (i == n - 1) {
                nets[i] = goodsAfterDiscount - allocatedNet;
            } else {
                nets[i] = goodsTotal == 0 ? 0
                        : Math.multiplyExact(items.get(i).itemTotalCents(), goodsAfterDiscount) / goodsTotal;
                allocatedNet += nets[i];
            }
        }
        // 2) 运费按净额比例分摊；最后一件用差额
        long[] shipShares = new long[n];
        long allocatedShip = 0;
        for (int i = 0; i < n; i++) {
            if (i == n - 1) {
                shipShares[i] = shippingCents - allocatedShip;
            } else {
                shipShares[i] = Math.multiplyExact(shippingCents, nets[i]) / goodsAfterDiscount;
                allocatedShip += shipShares[i];
            }
        }
        // 3) 分模式累计（BigDecimal 累加，最终一次 HALF_UP 到分）
        BigDecimal bondedTax = BigDecimal.ZERO;
        BigDecimal directTax = BigDecimal.ZERO;
        boolean hasDirect = false;
        for (int i = 0; i < n; i++) {
            TaxItem item = items.get(i);
            if ("DIRECT".equals(item.tradeMode())) {
                hasDirect = true;
                directTax = directTax.add(BigDecimal.valueOf(nets[i])
                        .multiply(BigDecimal.valueOf(item.postalTaxRate()))
                        .divide(BigDecimal.valueOf(100)));
            } else {
                bondedTax = bondedTax.add(BigDecimal.valueOf(nets[i] + shipShares[i]).multiply(BONDED_RATE));
            }
        }
        long bondedCents = bondedTax.setScale(0, RoundingMode.HALF_UP).longValueExact();
        long directCents = directTax.setScale(0, RoundingMode.HALF_UP).longValueExact();

        // 4) 行邮税 50 元免征：DIRECT 部分合计 ≤ 50 CNY 等值则免征
        boolean directExempted = false;
        if (directCents > 0) {
            long thresholdUsdCents = cny50InUsdCents();
            if (thresholdUsdCents >= 0 && directCents <= thresholdUsdCents) {
                directCents = 0;
                directExempted = true;
            }
        }

        long tax = bondedCents + directCents;
        List<String> parts = new ArrayList<>();
        if (bondedCents > 0) {
            parts.add("跨境综合税9.1%");
        }
        if (directCents > 0) {
            parts.add("行邮税");
        }
        if (directExempted) {
            parts.add("行邮税≤50元免征");
        }
        String note = parts.isEmpty() ? "免税" : String.join("+", parts);
        return new TaxResult(tax, identityRequired, note);
    }

    /** 50 元人民币折 USD 分（HALF_UP）；CNY 汇率不可得返回 -1（保守不免征）。 */
    private long cny50InUsdCents() {
        try {
            RateDTO cny = productTradeFacade.getRate("CNY");
            if (cny == null || cny.getRate() == null || cny.getRate().signum() <= 0) {
                return -1;
            }
            return POSTAL_EXEMPT_CNY_CENTS.divide(cny.getRate(), 0, RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            log.warn("[tax] getRate(CNY) failed, postal-tax exemption skipped: {}", e.getMessage());
            return -1;
        }
    }
}
