package com.trade.virtual_trade.dto;

import lombok.Data;

/**
 * 买家创建订单时唯一允许提交的业务字段。
 * 买家、卖家、价格、状态和订单号均由后端生成或查询，不能由客户端决定。
 */
@Data
public class CreateOrderRequest {
    private Integer productId;
}
