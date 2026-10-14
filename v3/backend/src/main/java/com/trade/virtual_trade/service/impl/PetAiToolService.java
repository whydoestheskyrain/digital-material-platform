package com.trade.virtual_trade.service.impl;

import com.trade.virtual_trade.entity.Order;
import com.trade.virtual_trade.entity.User;
import com.trade.virtual_trade.service.OrderService;
import com.trade.virtual_trade.service.ProductService;
import com.trade.virtual_trade.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI 客服业务上下文查询工具。
 * 根据用户意图，从数据库取真实业务数据，拼入提示词让 AI 有据可答，避免凭空编造。
 */
@Service
public class PetAiToolService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    /**
     * 汇总用户余额信息
     */
    public String getBalanceContext(Integer userId) {
        try {
            User user = userService.selectBalanceById(userId);
            if (user == null) {
                return "用户暂无余额信息。";
            }
            return String.format("可用余额：%.2f 元，冻结余额（交易托管中）：%.2f 元。",
                    user.getBalance() != null ? user.getBalance().doubleValue() : 0.0,
                    user.getFrozenBalance() != null ? user.getFrozenBalance().doubleValue() : 0.0);
        } catch (Exception e) {
            return "余额信息暂时无法获取。";
        }
    }

    /**
     * 汇总用户作为买家的订单统计
     */
    public String getBuyerOrderContext(Integer userId) {
        try {
            Map<String, Long> stats = orderService.getBuyerOrderStats(userId);
            if (stats == null || stats.isEmpty()) {
                return "您还没有任何订单哦~";
            }
            StringBuilder sb = new StringBuilder("您的买入订单情况：");
            appendStat(sb, stats, "1", "待支付");
            appendStat(sb, stats, "2", "已支付待发货");
            appendStat(sb, stats, "3", "已发货待收货");
            appendStat(sb, stats, "4", "已完成");
            appendStat(sb, stats, "5", "已取消");
            return sb.toString();
        } catch (Exception e) {
            return "订单信息暂时无法获取。";
        }
    }

    /**
     * 汇总用户作为卖家的订单统计
     */
    public String getSellerOrderContext(Integer userId) {
        try {
            Map<String, Long> stats = orderService.getSellerOrderStats(userId);
            if (stats == null || stats.isEmpty()) {
                return "您还没有卖出订单哦~";
            }
            StringBuilder sb = new StringBuilder("您的卖出订单情况：");
            appendStat(sb, stats, "1", "待买家付款");
            appendStat(sb, stats, "2", "待发货");
            appendStat(sb, stats, "3", "已发货待收货");
            appendStat(sb, stats, "4", "已完成");
            appendStat(sb, stats, "5", "已取消");
            return sb.toString();
        } catch (Exception e) {
            return "订单信息暂时无法获取。";
        }
    }

    /**
     * 获取用户最新一笔订单的简要信息（用于回答"我的订单到哪了"）
     */
    public String getLatestOrderContext(Integer userId) {
        try {
            List<Order> buyerOrders = orderService.getBuyerOrders(userId);
            if (buyerOrders != null && !buyerOrders.isEmpty()) {
                Order latest = buyerOrders.get(0);
                return String.format("您最新的买入订单号 %s，商品：%s，金额：%.2f 元，当前状态：%s。",
                        latest.getOrderNo(),
                        latest.getProductName() != null ? latest.getProductName() : "（未关联商品）",
                        latest.getPrice() != null ? latest.getPrice().doubleValue() : 0.0,
                        statusText(latest.getStatus()));
            }
            List<Order> sellerOrders = orderService.getSellerOrders(userId);
            if (sellerOrders != null && !sellerOrders.isEmpty()) {
                Order latest = sellerOrders.get(0);
                return String.format("您最新的卖出订单号 %s，商品：%s，金额：%.2f 元，当前状态：%s。",
                        latest.getOrderNo(),
                        latest.getProductName() != null ? latest.getProductName() : "（未关联商品）",
                        latest.getPrice() != null ? latest.getPrice().doubleValue() : 0.0,
                        statusText(latest.getStatus()));
            }
            return "您还没有任何订单哦~";
        } catch (Exception e) {
            return "订单信息暂时无法获取。";
        }
    }

    private void appendStat(StringBuilder sb, Map<String, Long> stats, String key, String label) {
        Long val = stats.get(key);
        if (val != null && val > 0) {
            sb.append(label).append(" ").append(val).append(" 笔，");
        }
    }

    private String statusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "待支付";
            case 2: return "已支付，等待卖家发货";
            case 3: return "卖家已发货，等待您确认收货";
            case 4: return "已完成";
            case 5: return "已取消";
            default: return "未知";
        }
    }
}
