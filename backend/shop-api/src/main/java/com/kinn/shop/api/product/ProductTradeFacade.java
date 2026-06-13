package com.kinn.shop.api.product;

import com.kinn.shop.api.product.dto.RateDTO;
import com.kinn.shop.api.product.dto.SkuTradeDTO;
import com.kinn.shop.api.product.dto.StockOpDTO;

import java.util.List;

/**
 * 商品服务对内 RPC：交易链路取 SKU 快照、扣/回库存、取汇率。
 */
public interface ProductTradeFacade {

    /**
     * 批量取交易用 SKU 快照：价格为当前生效闪购折后 USD 分（无闪购=原价），
     * 名称按 locale（缺失回退 en-US）。查无的 skuId 不在返回列表中。
     */
    List<SkuTradeDTO> getSkusForTrade(List<Long> skuIds, String locale);

    /**
     * 扣减库存（DB 乐观锁 stock>=q），全部成功才提交；任一不足回滚并返回 false。
     * 命中闪购的商品同步累计 flash_sale_item.sold。
     */
    boolean deductStock(List<StockOpDTO> ops);

    /** 回滚库存（关单/取消时），闪购 sold 同步回减（不小于0）。 */
    void restoreStock(List<StockOpDTO> ops);

    /** 汇率与币种信息：currency=USD 返回 rate=1。不支持的币种返回 null。 */
    RateDTO getRate(String currency);

    /** 支付成功后累计商品销量（按 productId 聚合，幂等性由调用方保证只调一次）。 */
    void addSales(List<StockOpDTO> ops);
}
