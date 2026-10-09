package com.trade.virtual_trade.task;

import com.trade.virtual_trade.entity.Order;
import com.trade.virtual_trade.mapper.OrderMapper;
import com.trade.virtual_trade.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消定时任务
 */
@Component
public class OrderTimeoutTask {

    private static final Logger logger = LoggerFactory.getLogger(OrderTimeoutTask.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    /**
     * 订单超时时间（分钟）
     * 15分钟未支付自动取消
     */
    private static final int ORDER_TIMEOUT_MINUTES = 15;

    /**
     * 定时任务：每5分钟检查一次超时订单
     * cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cancelTimeoutOrders() {
        logger.info("开始检查超时订单...");
        
        try {
            // 查询所有待支付且超时的订单
            LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(ORDER_TIMEOUT_MINUTES);
            List<Order> timeoutOrders = orderMapper.selectTimeoutOrders(timeoutTime);
            
            if (timeoutOrders.isEmpty()) {
                logger.info("没有超时订单需要处理");
                return;
            }
            
            logger.info("发现 {} 个超时订单", timeoutOrders.size());
            
            // 逐个取消超时订单
            for (Order order : timeoutOrders) {
                try {
                    // cancelOrder 会重新锁定订单并再次确认其仍是待支付状态。
                    // 若买家刚好完成支付，方法返回 false，不会把已支付订单错误取消。
                    boolean cancelled = orderService.cancelOrder(order.getId(), order.getBuyerId());
                    if (cancelled) {
                        logger.info("订单已自动取消，订单号：{}，订单ID：{}", order.getOrderNo(), order.getId());
                    }
                    
                } catch (Exception e) {
                    logger.error("取消订单失败，订单ID：{}，错误：{}", order.getId(), e.getMessage());
                }
            }
            
            logger.info("超时订单处理完成");
            
        } catch (Exception e) {
            logger.error("检查超时订单时发生异常：{}", e.getMessage(), e);
        }
    }
}
