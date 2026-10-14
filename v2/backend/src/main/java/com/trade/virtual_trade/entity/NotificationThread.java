package com.trade.virtual_trade.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息会话（消息中心按订单/广播/反馈/系统维度聚合后的列表项）
 */
@Data
public class NotificationThread {
    /** 会话唯一标识：order_{orderId} / broadcast / feedback / system */
    private String threadKey;
    /** 会话类型：order / broadcast / feedback / system */
    private String type;
    /** 会话显示名，如“订单 20261005xxxx” */
    private String title;
    private Integer orderId;
    private String orderNo;
    /** 最新一条消息内容 */
    private String lastContent;
    private LocalDateTime lastTime;
    /** 当前用户在该会话中的未读数 */
    private Integer unreadCount;
    /** 消息总数 */
    private Integer totalCount;
}
