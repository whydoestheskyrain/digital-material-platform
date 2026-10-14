package com.trade.virtual_trade.controller;

import com.trade.virtual_trade.common.Result;
import com.trade.virtual_trade.entity.Order;
import com.trade.virtual_trade.entity.Product;
import com.trade.virtual_trade.entity.RechargeOrder;
import com.trade.virtual_trade.entity.TransactionLog;
import com.trade.virtual_trade.entity.User;
import com.trade.virtual_trade.mapper.OrderMapper;
import com.trade.virtual_trade.mapper.ProductMapper;
import com.trade.virtual_trade.mapper.RechargeOrderMapper;
import com.trade.virtual_trade.mapper.TransactionLogMapper;
import com.trade.virtual_trade.mapper.UserMapper;
import com.trade.virtual_trade.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/stats")
public class AdminController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TransactionLogMapper transactionLogMapper;

    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;

    // ====================== 用户资金取证 ======================

    /** 某用户的全部资金流水（含变动前后余额，用于余额异议取证） */
    @GetMapping("/user/{userId}/logs")
    public Result<List<TransactionLog>> userLogs(@PathVariable Integer userId) {
        return Result.success(transactionLogMapper.findByUserId(userId));
    }

    /** 某用户的全部充值记录（含支付宝交易号） */
    @GetMapping("/user/{userId}/recharge")
    public Result<List<RechargeOrder>> userRecharge(@PathVariable Integer userId) {
        return Result.success(rechargeOrderMapper.findByUserId(userId));
    }

    /** 某用户作为买家或卖家的全部订单，按时间倒序，tradeRole 标记买入/卖出 */
    @GetMapping("/user/{userId}/orders")
    public Result<List<Order>> userOrders(@PathVariable Integer userId) {
        List<Order> buyerOrders = orderMapper.selectByBuyerId(userId);
        if (buyerOrders != null) {
            buyerOrders.forEach(o -> o.setTradeRole("买入"));
        }
        List<Order> sellerOrders = orderMapper.selectBySellerId(userId);
        if (sellerOrders != null) {
            sellerOrders.forEach(o -> o.setTradeRole("卖出"));
        }
        List<Order> all = new ArrayList<>();
        if (buyerOrders != null) all.addAll(buyerOrders);
        if (sellerOrders != null) all.addAll(sellerOrders);
        all.sort((a, b) -> {
            if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
            if (a.getCreateTime() == null) return 1;
            if (b.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });
        return Result.success(all);
    }

    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();

        List<User> users = userMapper.selectAll();
        result.put("userCount", users.size());

        List<Product> products = productMapper.findAll();
        result.put("productCount", products.size());
        result.put("pendingCount", products.stream().filter(p -> p.getStatus() == 0).count());

        List<Order> orders = orderMapper.listAll();
        result.put("orderCount", orders.size());

        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == 4)
                .map(o -> o.getPrice() != null ? o.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("totalRevenue", totalRevenue.doubleValue());

        return result;
    }

    @GetMapping("/orderByStatus")
    public Map<String, Object> getOrderByStatus() {
        Map<String, Object> result = new HashMap<>();

        List<Order> orders = orderMapper.listAll();

        long unpaid = orders.stream().filter(o -> o.getStatus() == 1).count();
        long paid = orders.stream().filter(o -> o.getStatus() == 2).count();
        long delivered = orders.stream().filter(o -> o.getStatus() == 3).count();
        long completed = orders.stream().filter(o -> o.getStatus() == 4).count();
        long cancelled = orders.stream().filter(o -> o.getStatus() == 5).count();

        result.put("labels", Arrays.asList("待支付", "待发货", "待收货", "已完成", "已取消"));
        result.put("data", Arrays.asList(unpaid, paid, delivered, completed, cancelled));

        return result;
    }

    @GetMapping("/revenueByDay")
    public Map<String, Object> getRevenueByDay() {
        Map<String, Object> result = new HashMap<>();

        List<LocalDate> dates = new ArrayList<>();
        List<Double> revenues = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date);

            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            List<Order> orders = orderMapper.listAll();
            BigDecimal revenue = orders.stream()
                    .filter(o -> o.getStatus() == 4)
                    .filter(o -> o.getPayTime() != null && !o.getPayTime().isBefore(start) && !o.getPayTime().isAfter(end))
                    .map(o -> o.getPrice() != null ? o.getPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            revenues.add(revenue.doubleValue());
        }

        result.put("labels", dates.stream().map(d -> d.getMonthValue() + "/" + d.getDayOfMonth()).collect(Collectors.toList()));
        result.put("data", revenues);

        return result;
    }

    @GetMapping("/productByGame")
    public Map<String, Object> getProductByGame() {
        Map<String, Object> result = new HashMap<>();

        List<Product> products = productMapper.findAll();

        Map<Integer, Long> gameTypeCount = products.stream()
                .filter(p -> p.getGameTypeId() != null)
                .collect(Collectors.groupingBy(Product::getGameTypeId, Collectors.counting()));

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        List<String> finalLabels = labels;
        List<Long> finalData = data;
        gameTypeCount.forEach((gameTypeId, count) -> {
            finalLabels.add("游戏" + gameTypeId);
            finalData.add(count);
        });

        if (labels.isEmpty()) {
            labels = Arrays.asList("暂无数据");
            data = Arrays.asList(0L);
        }

        result.put("labels", labels);
        result.put("data", data);

        return result;
    }

    @GetMapping("/activeUsers")
    public Map<String, Object> getActiveUsers() {
        Map<String, Object> result = new HashMap<>();

        List<User> users = userMapper.selectAll();

        long todayCount = users.stream()
                .filter(u -> u.getCreateTime() != null && u.getCreateTime().toLocalDate().equals(LocalDate.now()))
                .count();

        long weekCount = users.stream()
                .filter(u -> u.getCreateTime() != null && !u.getCreateTime().toLocalDate().isBefore(LocalDate.now().minusDays(7)))
                .count();

        long monthCount = users.stream()
                .filter(u -> u.getCreateTime() != null && !u.getCreateTime().toLocalDate().isBefore(LocalDate.now().minusDays(30)))
                .count();

        result.put("today", todayCount);
        result.put("week", weekCount);
        result.put("month", monthCount);

        return result;
    }

    @PostMapping("/broadcast")
    public Map<String, Object> broadcast(@RequestParam String title, @RequestParam String content) {
        notificationService.sendBroadcast(title, content);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "广播发送成功");
        return result;
    }
}
