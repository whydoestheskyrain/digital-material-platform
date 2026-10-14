package com.trade.virtual_trade.controller;

import com.trade.virtual_trade.common.Result;
import com.trade.virtual_trade.entity.Order;
import com.trade.virtual_trade.entity.User;
import com.trade.virtual_trade.dto.CreateOrderRequest;
import com.trade.virtual_trade.mapper.OrderMapper;
import com.trade.virtual_trade.service.OrderService;
import com.trade.virtual_trade.service.UserService;
import com.trade.virtual_trade.utils.JwtUtil;
import com.trade.virtual_trade.security.CurrentUser;
import com.trade.virtual_trade.security.RequireAdmin;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




@RestController
@RequestMapping("/order")
public class OrderController {

    // ====================== 【管理员专用】查看所有待发货订单（状态=2）======================
    @GetMapping("/admin/waitDelivery")
    @RequireAdmin
    public Result<List<Order>> adminWaitDelivery() {
        // 直接查询所有状态=2的订单
        List<Order> list = orderService.getAdminWaitDelivery();
        return Result.success(list);
    }
    /**
     * 创建订单接口（前端购买商品时调用）
     */

    @Resource
    private OrderService orderService;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private UserService userService;

    // 余额支付接口
    @PutMapping("/pay/{id}")
    public Result payByBalance(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        Integer userId = JwtUtil.getUserId(token.substring(7));
        
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        
        if (!userId.equals(order.getBuyerId())) {
            return Result.error("无权操作此订单");
        }
        
        if (order.getStatus() != 1) {
            return Result.error("订单状态不是待支付");
        }
        
        boolean success = orderService.payByBalance(id, userId);
        if (success) {
            return Result.success("支付成功");
        } else {
            return Result.error("支付失败，余额不足");
        }
    }

    @PostMapping("/add")
    public Result<Integer> addOrder(@RequestBody CreateOrderRequest createOrderRequest,
                                    HttpServletRequest request) {
        try {
            if (createOrderRequest.getProductId() == null) {
                return Result.error("商品ID不能为空");
            }

            // 只有这两项来自请求：商品 ID 和经过拦截器验证的当前买家。
            Order order = new Order();
            Integer buyerId = CurrentUser.getUserId(request);
            order.setProductId(createOrderRequest.getProductId());
            order.setBuyerId(buyerId);

            // 订单号由服务器生成，避免客户端伪造或重复使用。
            String orderNo = "ORDER" + System.currentTimeMillis() + String.format("%04d", new java.util.Random().nextInt(10000));
            order.setOrderNo(orderNo);

            // Service 会从数据库商品记录写入卖家、成交价和初始状态。
            Order createdOrder = orderService.createOrder(order);
            return Result.success("订单创建成功", createdOrder.getId());
        } catch (Exception e) {
            return Result.error("订单创建失败：" + e.getMessage());
        }
    }

    // 包装订单状态文字（给前端显示用）
    private List<Order> wrapOrdersWithStatusDesc(List<Order> orders) {
        for (Order order : orders) {
            order.setStatusDesc(getStatusDesc(order.getStatus()));
            order.setStatusColor(getStatusColor(order.getStatus()));
        }
        return orders;
    }

    // 获取状态文字描述
    private String getStatusDesc(Integer status) {
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

    // 获取状态颜色（用于前端展示）
    private String getStatusColor(Integer status) {
        if (status == null) return "#999999";
        switch (status) {
            case 1: return "#E6A23C"; // 橙色 - 待支付
            case 2: return "#409EFF"; // 蓝色 - 待发货
            case 3: return "#67C23A"; // 绿色 - 待收货
            case 4: return "#909399"; // 灰色 - 已完成
            case 5: return "#F56C6C"; // 红色 - 已取消
            default: return "#999999";
        }
    }

    // ====================== 统一列表接口（支持筛选状态 + 买家ID） ======================
    @GetMapping("/listAll")
    public Result<List<Order>> listAll(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "buyerId", required = false) Integer buyerId,
            HttpServletRequest request
    ) {
        // 先识别当前用户是否为管理员（仅用于无 buyerId 时的后台全量查询）
        boolean isAdmin = CurrentUser.isAdmin(request);

        // 1. 只要显式传了 buyerId，一律按买家身份查自己的订单（管理员也不例外），
        //    避免管理员在“我买到的”里看到全量订单
        if (buyerId != null) {
            if (!buyerId.equals(CurrentUser.getUserId(request)) && !isAdmin) {
                return Result.error("无权查看其他用户的订单");
            }
            List<Order> orderList;
            if (status == null) {
                orderList = orderService.getBuyerOrders(buyerId);
            } else {
                switch (status) {
                    case 1: orderList = orderService.getBuyerUnpaidOrders(buyerId); break;
                    case 2: orderList = orderService.getBuyerPaidOrders(buyerId); break;
                    case 3: orderList = orderService.getBuyerDeliveredOrders(buyerId); break;
                    case 4: orderList = orderService.getBuyerCompletedOrders(buyerId); break;
                    default: orderList = orderService.getBuyerOrders(buyerId);
                }
            }
            return Result.success(orderList);
        }

        // 2. 没有 buyerId：仅管理员可查全部订单（管理后台使用）
        if (!isAdmin) {
            return Result.success(Collections.emptyList());
        }
        if (status == null) {
            return Result.success(orderService.listAll());
        }
        return Result.success(orderService.getOrdersByStatus(status));
    }

    // ====================== 下面是你原来所有代码，我完全保留 ======================

    @PostMapping("/confirm/{id}")
    public Result confirm(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        Integer userId = JwtUtil.getUserId(token.substring(7));

        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!userId.equals(order.getBuyerId())) {
            return Result.error("无权操作此订单");
        }

        boolean success = orderService.confirmOrder(id, userId);
        if (success) {
            return Result.success("确认收货成功");
        } else {
            return Result.error("确认收货失败");
        }
    }

    @PostMapping("/cancel/{id}")
    public Result cancel(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        Integer userId = JwtUtil.getUserId(token.substring(7));

        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!userId.equals(order.getBuyerId())) {
            return Result.error("无权操作此订单");
        }

        if (orderService.cancelOrder(id, userId)) {
            return Result.success("订单已取消");
        } else {
            return Result.error("取消订单失败");
        }
    }

    @DeleteMapping("/delete/{id}")
    @RequireAdmin
    public Result delete(@PathVariable Integer id) {
        orderService.deleteById(id);
        return Result.success("删除成功");
    }

    // 更新订单信息（管理后台编辑用）
    @PostMapping("/update")
    @RequireAdmin
    public Result updateOrder(@RequestBody Order order) {
        if (order.getId() == null) {
            return Result.error("订单ID不能为空");
        }
        orderMapper.update(order);
        return Result.success("更新成功");
    }

    @GetMapping("/buyer/{id}/unpaid")
    public Result buyerUnpaidOrders(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSelfOrAdmin(id, request)) return Result.error("无权查看其他用户的订单");
        return Result.success(wrapOrdersWithStatusDesc(orderService.getBuyerUnpaidOrders(id)));
    }

    // 管理员发货接口（支持管理员和卖家发货）
    @PostMapping("/deliver/{id}")
    public Result deliver(@PathVariable Integer id, @RequestBody Map<String, String> request, @RequestHeader("Authorization") String token) {
        Integer userId = JwtUtil.getUserId(token.substring(7));
        String accountInfo = request.get("accountInfo");

        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }

        // 获取当前用户信息
        User user = userService.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 权限验证：管理员或卖家本人可以发货
        boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());
        boolean isSeller = userId.equals(order.getSellerId());
        
        if (!isAdmin && !isSeller) {
            return Result.error("无权操作此订单，只有管理员或卖家可以发货");
        }

        if (order.getStatus() != 2) {
            return Result.error("订单状态不是待发货");
        }

        orderService.deliverGoods(id, accountInfo);
        return Result.success("发货成功");
    }

    @GetMapping("/buyer/{id}/paid")
    public Result buyerPaidOrders(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSelfOrAdmin(id, request)) return Result.error("无权查看其他用户的订单");
        return Result.success(wrapOrdersWithStatusDesc(orderService.getBuyerPaidOrders(id)));
    }

    @GetMapping("/buyer/{id}/delivered")
    public Result buyerDeliveredOrders(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSelfOrAdmin(id, request)) return Result.error("无权查看其他用户的订单");
        return Result.success(wrapOrdersWithStatusDesc(orderService.getBuyerDeliveredOrders(id)));
    }

    @GetMapping("/buyer/{id}/completed")
    public Result buyerCompletedOrders(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSelfOrAdmin(id, request)) return Result.error("无权查看其他用户的订单");
        return Result.success(wrapOrdersWithStatusDesc(orderService.getBuyerCompletedOrders(id)));
    }

    @GetMapping("/buyer/{id}/cancelled")
    public Result buyerCancelledOrders(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSelfOrAdmin(id, request)) return Result.error("无权查看其他用户的订单");
        return Result.success(wrapOrdersWithStatusDesc(orderService.getBuyerCancelledOrders(id)));
    }

    // 获取卖家所有订单
    @GetMapping("/seller/{id}/all")
    public Result sellerAllOrders(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSelfOrAdmin(id, request)) return Result.error("无权查看其他用户的订单");
        return Result.success(wrapOrdersWithStatusDesc(orderService.getSellerOrders(id)));
    }

    @GetMapping("/seller/{id}/paid")
    public Result sellerPaidOrders(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSelfOrAdmin(id, request)) return Result.error("无权查看其他用户的订单");
        return Result.success(wrapOrdersWithStatusDesc(orderService.getSellerPaidOrders(id)));
    }

    @GetMapping("/seller/{id}/delivered")
    public Result sellerDeliveredOrders(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSelfOrAdmin(id, request)) return Result.error("无权查看其他用户的订单");
        return Result.success(wrapOrdersWithStatusDesc(orderService.getSellerDeliveredOrders(id)));
    }

    @GetMapping("/seller/{id}/completed")
    public Result sellerCompletedOrders(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSelfOrAdmin(id, request)) return Result.error("无权查看其他用户的订单");
        return Result.success(wrapOrdersWithStatusDesc(orderService.getSellerCompletedOrders(id)));
    }

    @GetMapping("/seller/{id}/cancelled")
    public Result sellerCancelledOrders(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSelfOrAdmin(id, request)) return Result.error("无权查看其他用户的订单");
        return Result.success(wrapOrdersWithStatusDesc(orderService.getSellerCancelledOrders(id)));
    }

    @GetMapping("/buyer/{id}/stats")
    public Result buyerOrderStats(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSelfOrAdmin(id, request)) return Result.error("无权查看其他用户的订单");
        return Result.success(orderService.getBuyerOrderStats(id));
    }

    @GetMapping("/seller/{id}/stats")
    public Result sellerOrderStats(@PathVariable Integer id, HttpServletRequest request) {
        if (!isSelfOrAdmin(id, request)) return Result.error("无权查看其他用户的订单");
        return Result.success(orderService.getSellerOrderStats(id));
    }

    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Integer id, @RequestHeader("Authorization") String token) {
        Integer userId = JwtUtil.getUserId(token.substring(7));
        Order order = orderService.getDetailById(id);

        if (order == null) {
            return Result.error("订单不存在");
        }

        // 权限验证：买家、卖家本人或管理员可以查看
        User user = userService.findById(userId);
        boolean isAdmin = user != null && "admin".equalsIgnoreCase(user.getRole());
        if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId()) && !isAdmin) {
            return Result.error("无权查看此订单");
        }

        order.setStatusDesc(getStatusDesc(order.getStatus()));
        order.setStatusColor(getStatusColor(order.getStatus()));
        return Result.success(order);
    }

    @GetMapping("/{orderId}/account-info")
    public Result getAccountInfo(@PathVariable Integer orderId, @RequestHeader("Authorization") String token) {
        Integer userId = JwtUtil.getUserId(token.substring(7));
        Order order = orderService.getById(orderId);

        if (order == null) {
            return Result.error("订单不存在");
        }

        // 权限验证：买家本人或管理员可以查看
        User user = userService.findById(userId);
        boolean isAdmin = user != null && "admin".equalsIgnoreCase(user.getRole());
        
        if (!userId.equals(order.getBuyerId()) && !isAdmin) {
            return Result.error("无权查看此订单的账号信息");
        }

        if (!Integer.valueOf(3).equals(order.getStatus())) {
            return Result.error("订单尚未发货，无法查看账号信息");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("accountInfo", order.getAccountInfo());

        return Result.success(result);
    }

    private boolean isSelfOrAdmin(Integer userId, HttpServletRequest request) {
        return userId != null && (userId.equals(CurrentUser.getUserId(request)) || CurrentUser.isAdmin(request));
    }
}
