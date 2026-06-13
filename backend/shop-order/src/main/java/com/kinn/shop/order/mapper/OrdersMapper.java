package com.kinn.shop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kinn.shop.order.entity.Orders;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface OrdersMapper extends BaseMapper<Orders> {

    /** 近 N 天已支付订单按日聚合（含当天）；日期升序。 */
    @Select("""
            SELECT DATE(paid_at) AS day,
                   COUNT(*) AS orders,
                   COALESCE(SUM(total_amount_cents), 0) AS sales_cents
            FROM orders
            WHERE paid_at IS NOT NULL
              AND paid_at >= DATE_SUB(CURDATE(), INTERVAL #{days} - 1 DAY)
            GROUP BY DATE(paid_at)
            ORDER BY day
            """)
    List<Map<String, Object>> dailyPaidStats(int days);

    /** 各状态订单数。 */
    @Select("SELECT status, COUNT(*) AS cnt FROM orders GROUP BY status")
    List<Map<String, Object>> statusCounts();

    /** 累计已支付销售额（USD 分）与单数。 */
    @Select("""
            SELECT COUNT(*) AS orders, COALESCE(SUM(total_amount_cents), 0) AS sales_cents
            FROM orders WHERE paid_at IS NOT NULL
            """)
    Map<String, Object> totalPaid();

    /** 今日已支付销售额（USD 分）与单数。 */
    @Select("""
            SELECT COUNT(*) AS orders, COALESCE(SUM(total_amount_cents), 0) AS sales_cents
            FROM orders WHERE paid_at IS NOT NULL AND paid_at >= CURDATE()
            """)
    Map<String, Object> todayPaid();
}
