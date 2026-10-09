package com.trade.virtual_trade.service.impl;

import com.trade.virtual_trade.entity.Order;
import com.trade.virtual_trade.entity.Product;
import com.trade.virtual_trade.entity.TransactionLog;
import com.trade.virtual_trade.entity.User;
import com.trade.virtual_trade.mapper.OrderMapper;
import com.trade.virtual_trade.mapper.ProductMapper;
import com.trade.virtual_trade.mapper.TransactionLogMapper;
import com.trade.virtual_trade.mapper.UserMapper;
import com.trade.virtual_trade.service.NotificationService;
import com.trade.virtual_trade.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final Integer STATUS_UNPAID = 1;      // 待付款
    private static final Integer STATUS_PAID = 2;        // 已付款（待发货）
    private static final Integer STATUS_DELIVERED = 3;   // 已发货（待收货）
    private static final Integer STATUS_COMPLETED = 4;   // 已完成
    private static final Integer STATUS_CANCELLED = 5;   // 已取消

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TransactionLogMapper transactionLogMapper;

    @Autowired
    private NotificationService notificationService;

    private String getStatusDescription(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "待支付";
            case 2: return "待发货";
            case 3: return "待收货";
            case 4: return "已完成";
            case 5: return "已取消";
            default: return "未知";
        }
    }

    @Override
    @Transactional
    public boolean payByBalance(Integer orderId, Integer buyerId) {
        logger.info("余额支付，订单ID：{}，买家ID：{}", orderId, buyerId);

        Order order = orderMapper.getByIdForUpdate(orderId);
        if (order == null) {
            logger.warn("订单不存在，订单ID：{}", orderId);
            return false;
        }

        if (!STATUS_UNPAID.equals(order.getStatus())) {
            logger.warn("订单状态不是待支付，当前状态：{}", order.getStatus());
            return false;
        }

        User buyer = userMapper.selectByIdForUpdate(buyerId);
        if (buyer == null) return false;

        BigDecimal balance = buyer.getBalance() == null ? BigDecimal.ZERO : buyer.getBalance();
        if (balance.compareTo(order.getPrice()) < 0) return false;

        int rows = userMapper.deductBalance(buyerId, order.getPrice().doubleValue());
        if (rows == 0) return false;

        // 买家支付后，款项托管到卖家冻结余额（确认收货前不可动用）
        // 先记录流水（流水内的冻结前/后余额基于加款前的值计算），再实际增加冻结余额
        insertSellerFreezeLog(order);
        userMapper.addFrozenBalance(order.getSellerId(), order.getPrice().doubleValue());

        Order update = new Order();
        update.setId(orderId);
        update.setStatus(STATUS_PAID);
        update.setPayTime(LocalDateTime.now());
        update.setPaymentMethod("balance");
        orderMapper.updateStatus(update);

        TransactionLog log = new TransactionLog();
        log.setUserId(buyerId);
        log.setAmount(order.getPrice().negate());
        log.setType("pay");
        log.setOrderId(Long.valueOf(orderId));
        log.setBalanceBefore(balance);
        log.setBalanceAfter(balance.subtract(order.getPrice()));
        log.setRemark("购买商品");
        transactionLogMapper.insert(log);

        // 卖家侧流水：款项冻结中（账单取证依据）
        // 注意：此处不再重复调用，已在上面 addFrozenBalance 前记录

        notificationService.sendOrderNotification(order.getBuyerId(), "支付成功", "订单" + order.getOrderNo() + "已支付成功，等待卖家发货", orderId);
        notificationService.sendOrderNotification(order.getSellerId(), "订单已支付", "订单" + order.getOrderNo() + "买家已支付，请尽快发货", orderId);

        logger.info("余额支付成功，订单已改为待发货");
        return true;
    }

    @Override
    @Transactional
    public boolean confirmOrder(Integer orderId, Integer buyerId) {
        Order order = orderMapper.getByIdForUpdate(orderId);
        if (order == null || !STATUS_DELIVERED.equals(order.getStatus())) {
            return false;
        }

        // 结算：卖家冻结余额转为可用余额（托管期结束）
        User seller = userMapper.selectByIdForUpdate(order.getSellerId());
        if (seller == null) return false;
        BigDecimal sellerFrozen = seller.getFrozenBalance() == null ? BigDecimal.ZERO : seller.getFrozenBalance();
        if (sellerFrozen.compareTo(order.getPrice()) < 0) {
            logger.warn("卖家冻结余额不足，无法结算，订单ID：{}，卖家ID：{}", orderId, order.getSellerId());
            return false;
        }

        // 给卖家结算：冻结转可用，并记录卖家收入流水（账单“卖出记录”取证依据）
        BigDecimal sellerBefore = seller.getBalance() == null ? BigDecimal.ZERO : seller.getBalance();
        userMapper.settleFrozenToBalance(order.getSellerId(), order.getPrice().doubleValue());
        BigDecimal sellerAfter = sellerBefore.add(order.getPrice());
        TransactionLog sellerLog = new TransactionLog();
        sellerLog.setUserId(order.getSellerId());
        sellerLog.setAmount(order.getPrice());
        sellerLog.setType("income");
        sellerLog.setOrderId(Long.valueOf(orderId));
        sellerLog.setBalanceBefore(sellerBefore);
        sellerLog.setBalanceAfter(sellerAfter);
        sellerLog.setRemark("订单收入 " + order.getOrderNo());
        transactionLogMapper.insert(sellerLog);

        // 更新订单为已完成
        Order update = new Order();
        update.setId(orderId);
        update.setStatus(STATUS_COMPLETED);
        orderMapper.updateStatus(update);

        notificationService.sendOrderNotification(order.getBuyerId(), "交易完成", "订单" + order.getOrderNo() + "交易已完成，感谢您的购买", orderId);
        notificationService.sendOrderNotification(order.getSellerId(), "交易完成", "订单" + order.getOrderNo() + "交易已完成，余额已到账", orderId);

        return true;
    }

    @Override
    @Transactional
    public boolean cancelOrder(Integer orderId, Integer buyerId) {
        Order order = orderMapper.getByIdForUpdate(orderId);
        if (order == null) {
            logger.warn("订单不存在，订单ID：{}", orderId);
            return false;
        }

        // 项目规则：只有未支付订单可以取消。已支付或已发货订单不提供退款/撤销能力。
        if (!STATUS_UNPAID.equals(order.getStatus())) {
            logger.warn("订单不是待支付状态，无法取消，订单ID：{}，状态：{}", orderId, order.getStatus());
            return false;
        }

        Order update = new Order();
        update.setId(orderId);
        update.setStatus(STATUS_CANCELLED);
        orderMapper.updateStatus(update);

        // 如果商品还没恢复，恢复商品状态
        Product product = productMapper.findById(order.getProductId());
        if (product != null && product.getStatus() == 3) {
            productMapper.updateById(order.getProductId(), 1);
        }

        notificationService.sendOrderNotification(order.getBuyerId(), "订单已取消", "订单" + order.getOrderNo() + "已取消", orderId);
        notificationService.sendOrderNotification(order.getSellerId(), "订单已取消", "订单" + order.getOrderNo() + "已被买家取消", orderId);

        logger.info("订单已取消，订单ID：{}", orderId);
        return true;
    }

    @Override
    public void updateStatus(Integer orderId, Integer status) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(status);
        orderMapper.updateStatus(order);
    }

    @Override
    public List<Order> listAll() {
        return orderMapper.listAll();
    }

    @Override
    public void deleteById(Integer id) {
        orderMapper.deleteById(id);
    }

    /** 每个买家最多同时进行的订单数（防恶意下单占库存） */
    private static final int MAX_ACTIVE_ORDERS_PER_BUYER = 3;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Order order) {
        if (order.getBuyerId() == null) {
            throw new RuntimeException("登录已失效，请重新登录");
        }

        // 同一买家的创建请求串行执行，进行中订单数量不会被并发请求绕过。
        User buyer = userMapper.selectByIdForUpdate(order.getBuyerId());
        if (buyer == null) {
            throw new RuntimeException("用户不存在");
        }

        // 限制买家进行中的订单数，防止恶意下单占用商品。
        Long activeCount = orderMapper.countActiveByBuyerId(order.getBuyerId());
        if (activeCount != null && activeCount >= MAX_ACTIVE_ORDERS_PER_BUYER) {
            throw new RuntimeException("您有" + activeCount + "笔订单正在进行中，最多可同时下单" + MAX_ACTIVE_ORDERS_PER_BUYER + "笔，请先支付或取消部分订单");
        }

        // 原子更新是最终裁判：只允许一个请求把上架商品（status=1）占用为 status=3。
        int reserveRows = productMapper.reserveIfAvailable(order.getProductId());
        if (reserveRows != 1) {
            throw new RuntimeException("商品已被其他用户抢先下单");
        }

        // 占用成功后再读取商品，卖家和价格使用数据库最新记录，绝不信任客户端字段。
        Product product = productMapper.findById(order.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (product.getSellerId() == null || product.getPrice() == null
                || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("商品价格或卖家信息异常");
        }
        if (order.getBuyerId().equals(product.getSellerId())) {
            throw new RuntimeException("不能购买自己发布的商品");
        }

        // 成交关键字段只以数据库商品记录为准，忽略任何客户端伪造的卖家、价格和状态。
        order.setSellerId(product.getSellerId());
        order.setPrice(product.getPrice());
        order.setStatus(STATUS_UNPAID);
        order.setCreateTime(LocalDateTime.now());

        orderMapper.insert(order);

        notificationService.sendOrderNotification(order.getBuyerId(), "下单成功", "订单" + order.getOrderNo() + "已创建，请尽快支付", order.getId());
        notificationService.sendOrderNotification(order.getSellerId(), "新订单提醒", "收到新订单" + order.getOrderNo() + "，买家已下单", order.getId());

        return order;
    }

    @Override
    public void payOrder(Integer orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateStatus(order);
    }

    @Override
    public void deliverOrder(Integer orderId, String accountInfo) {
        Order order = orderMapper.getById(orderId);
        if (order == null || !STATUS_PAID.equals(order.getStatus())) return;

        if (accountInfo == null || accountInfo.trim().isEmpty()) {
            Product product = productMapper.findById(order.getProductId());
            if (product != null) {
                accountInfo = "账号：" + product.getGameAccount() + "\n密码：" + product.getGamePassword();
            }
        }

        Order update = new Order();
        update.setId(orderId);
        update.setAccountInfo(accountInfo);
        update.setStatus(STATUS_DELIVERED);

        orderMapper.updateAccountInfo(update);
        orderMapper.updateStatus(update);

        notificationService.sendOrderNotification(order.getBuyerId(), "卖家已发货", "订单" + order.getOrderNo() + "卖家已发货，请确认收货", orderId);
        notificationService.sendOrderNotification(order.getSellerId(), "已发货", "订单" + order.getOrderNo() + "已发货，等待买家确认", orderId);

        logger.info("订单已发货，状态改为3");
    }

    @Override
    public void deliverGoods(Integer id, String accountInfo) {
        deliverOrder(id, accountInfo);
    }

    @Override
    public void confirmOrder(Integer orderId) {
        Order order = orderMapper.getById(orderId);
        if (order != null && STATUS_DELIVERED.equals(order.getStatus())) {
            Order update = new Order();
            update.setId(orderId);
            update.setStatus(STATUS_COMPLETED);
            orderMapper.updateStatus(update);
        }
    }

    // ========================= 管理员查看待发货 =========================
    @Override
    public List<Order> getAdminWaitDelivery() {
        return orderMapper.findByStatus(STATUS_PAID);
    }

    // ========================= 支付宝支付成功回调 =========================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo, String tradeNo) {
        Order order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null || order.getStatus() >= STATUS_PAID) return;

        Order update = new Order();
        update.setId(order.getId());
        update.setStatus(STATUS_PAID);
        update.setPayTime(LocalDateTime.now());
        update.setPaymentMethod("alipay");
        update.setTradeNo(tradeNo);
        orderMapper.updateStatus(update);

        // 支付宝已从外部账户完成扣款，平台余额不应再次扣减。
        // 这里只把同等金额托管到卖家冻结余额，等待买家确认收货后结算。

        // 款项托管到卖家冻结余额（先记录流水再增加冻结，保证流水中的冻结前余额准确）
        insertSellerFreezeLog(order);
        userMapper.addFrozenBalance(order.getSellerId(), order.getPrice().doubleValue());
        logger.info("支付宝支付成功，订单改为待发货");
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        return orderMapper.selectByOrderNo(orderNo);
    }

    @Override
    public List<Order> getBuyerOrders(Integer buyerId) {
        return orderMapper.selectByBuyerId(buyerId);
    }

    @Override
    public List<Order> getSellerOrders(Integer sellerId) {
        return orderMapper.selectBySellerId(sellerId);
    }

    @Override
    public Order getOrderById(Integer orderId) {
        return orderMapper.getById(orderId);
    }

    @Override
    public Order getById(Integer orderId) {
        return orderMapper.getById(orderId);
    }

    @Override
    public Order getDetailById(Integer orderId) {
        return orderMapper.selectDetailById(orderId);
    }

    /** 买家支付后，在卖家账单中记录一笔“资金冻结”流水（金额为正，表示款项冻结在卖家账户中） */
    private void insertSellerFreezeLog(Order order) {
        User seller = userMapper.selectById(order.getSellerId());
        if (seller == null) return;

        TransactionLog sellerLog = new TransactionLog();
        sellerLog.setUserId(order.getSellerId());
        sellerLog.setAmount(order.getPrice());
        sellerLog.setType("freeze");
        sellerLog.setOrderId(Long.valueOf(order.getId()));
        BigDecimal sellerFrozen = seller.getFrozenBalance() == null ? BigDecimal.ZERO : seller.getFrozenBalance();
        sellerLog.setBalanceBefore(sellerFrozen);
        sellerLog.setBalanceAfter(sellerFrozen.add(order.getPrice()));
        sellerLog.setRemark("订单 " + order.getOrderNo() + " 买家已支付，款项冻结中");
        transactionLogMapper.insert(sellerLog);
    }

    // ====================== 按状态查询 ======================

    @Override
    public List<Order> getBuyerUnpaidOrders(Integer buyerId) {
        return orderMapper.selectByBuyerIdAndStatus(buyerId, STATUS_UNPAID);
    }

    @Override
    public List<Order> getBuyerPaidOrders(Integer buyerId) {
        return orderMapper.selectByBuyerIdAndStatus(buyerId, STATUS_PAID);
    }

    @Override
    public List<Order> getBuyerDeliveredOrders(Integer buyerId) {
        return orderMapper.selectByBuyerIdAndStatus(buyerId, STATUS_DELIVERED);
    }

    @Override
    public List<Order> getBuyerCompletedOrders(Integer buyerId) {
        return orderMapper.selectByBuyerIdAndStatus(buyerId, STATUS_COMPLETED);
    }

    @Override
    public List<Order> getBuyerCancelledOrders(Integer buyerId) {
        return orderMapper.selectByBuyerIdAndStatus(buyerId, STATUS_CANCELLED);
    }

    @Override
    public List<Order> getSellerPaidOrders(Integer sellerId) {
        return orderMapper.selectBySellerIdAndStatus(sellerId, STATUS_PAID);
    }

    @Override
    public List<Order> getSellerDeliveredOrders(Integer sellerId) {
        return orderMapper.selectBySellerIdAndStatus(sellerId, STATUS_DELIVERED);
    }

    @Override
    public List<Order> getSellerCompletedOrders(Integer sellerId) {
        return orderMapper.selectBySellerIdAndStatus(sellerId, STATUS_COMPLETED);
    }

    @Override
    public List<Order> getSellerCancelledOrders(Integer sellerId) {
        return orderMapper.selectBySellerIdAndStatus(sellerId, STATUS_CANCELLED);
    }

    @Override
    public Map<String, Long> getBuyerOrderStats(Integer buyerId) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("unpaid", orderMapper.countByBuyerIdAndStatus(buyerId, STATUS_UNPAID));
        stats.put("paid", orderMapper.countByBuyerIdAndStatus(buyerId, STATUS_PAID));
        stats.put("delivered", orderMapper.countByBuyerIdAndStatus(buyerId, STATUS_DELIVERED));
        stats.put("completed", orderMapper.countByBuyerIdAndStatus(buyerId, STATUS_COMPLETED));
        stats.put("cancelled", orderMapper.countByBuyerIdAndStatus(buyerId, STATUS_CANCELLED));
        return stats;
    }
    @Override
    public List<Order> getOrdersByStatus(Integer status) {
        return orderMapper.findByStatus(status);
    }
    @Override
    public Map<String, Long> getSellerOrderStats(Integer sellerId) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("unpaid", orderMapper.countBySellerIdAndStatus(sellerId, STATUS_UNPAID));
        stats.put("paid", orderMapper.countBySellerIdAndStatus(sellerId, STATUS_PAID));
        stats.put("delivered", orderMapper.countBySellerIdAndStatus(sellerId, STATUS_DELIVERED));
        stats.put("completed", orderMapper.countBySellerIdAndStatus(sellerId, STATUS_COMPLETED));
        stats.put("cancelled", orderMapper.countBySellerIdAndStatus(sellerId, STATUS_CANCELLED));
        return stats;
    }
}
