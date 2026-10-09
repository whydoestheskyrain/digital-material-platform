package com.trade.virtual_trade.service.impl;

import com.trade.virtual_trade.entity.Order;
import com.trade.virtual_trade.entity.Product;
import com.trade.virtual_trade.entity.User;
import com.trade.virtual_trade.mapper.OrderMapper;
import com.trade.virtual_trade.mapper.ProductMapper;
import com.trade.virtual_trade.mapper.TransactionLogMapper;
import com.trade.virtual_trade.mapper.UserMapper;
import com.trade.virtual_trade.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTests {

    @Mock private OrderMapper orderMapper;
    @Mock private ProductMapper productMapper;
    @Mock private UserMapper userMapper;
    @Mock private TransactionLogMapper transactionLogMapper;
    @Mock private NotificationService notificationService;
    @InjectMocks private OrderServiceImpl orderService;
    @Captor private ArgumentCaptor<Order> orderCaptor;

    @Test
    void createOrderMustUseSellerAndPriceFromDatabaseProduct() {
        Product product = availableProduct(10, 200, "99.90");
        Order clientOrder = new Order();
        clientOrder.setProductId(10);
        clientOrder.setBuyerId(100);
        // 模拟恶意客户端提交的字段，后端必须完全覆盖它们。
        clientOrder.setSellerId(999);
        clientOrder.setPrice(new BigDecimal("0.01"));
        clientOrder.setStatus(4);
        clientOrder.setOrderNo("ORDER-TEST");

        when(userMapper.selectByIdForUpdate(100)).thenReturn(user(100, "0.00", "0.00"));
        when(productMapper.findById(10)).thenReturn(product);
        when(orderMapper.countActiveByBuyerId(100)).thenReturn(0L);
        when(productMapper.reserveIfAvailable(10)).thenReturn(1);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Order.class).setId(1);
            return 1;
        });

        orderService.createOrder(clientOrder);

        verify(orderMapper).insert(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(Integer.valueOf(200), savedOrder.getSellerId());
        assertEquals(new BigDecimal("99.90"), savedOrder.getPrice());
        assertEquals(Integer.valueOf(1), savedOrder.getStatus());
    }

    @Test
    void createOrderMustRejectBuyingOwnProduct() {
        when(userMapper.selectByIdForUpdate(100)).thenReturn(user(100, "0.00", "0.00"));
        when(productMapper.findById(10)).thenReturn(availableProduct(10, 100, "99.90"));
        when(productMapper.reserveIfAvailable(10)).thenReturn(1);
        Order order = new Order();
        order.setProductId(10);
        order.setBuyerId(100);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.createOrder(order));

        assertEquals("不能购买自己发布的商品", exception.getMessage());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void createOrderMustRejectProductAlreadyReservedByAnotherBuyer() {
        Order order = new Order();
        order.setProductId(10);
        order.setBuyerId(100);
        when(userMapper.selectByIdForUpdate(100)).thenReturn(user(100, "0.00", "0.00"));
        when(orderMapper.countActiveByBuyerId(100)).thenReturn(0L);
        when(productMapper.reserveIfAvailable(10)).thenReturn(0);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.createOrder(order));

        assertEquals("商品已被其他用户抢先下单", exception.getMessage());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void balancePaymentMustPersistPaymentMethod() {
        Order order = order(1, 100, 200, "99.90", 1, null);
        User buyer = user(100, "100.00", "0.00");
        User seller = user(200, "0.00", "0.00");
        when(orderMapper.getByIdForUpdate(1)).thenReturn(order);
        when(userMapper.selectByIdForUpdate(100)).thenReturn(buyer);
        when(userMapper.selectById(200)).thenReturn(seller);
        when(userMapper.deductBalance(100, 99.90)).thenReturn(1);

        boolean success = orderService.payByBalance(1, 100);

        assertEquals(true, success);
        verify(orderMapper).updateStatus(orderCaptor.capture());
        assertEquals("balance", orderCaptor.getValue().getPaymentMethod());
        assertEquals(Integer.valueOf(2), orderCaptor.getValue().getStatus());
    }

    @Test
    void cancelUnpaidOrderMustOnlyRestoreProductStatus() {
        Order order = order(1, 100, 200, "99.90", 1, null);
        Product product = availableProduct(10, 200, "99.90");
        product.setStatus(3);
        when(orderMapper.getByIdForUpdate(1)).thenReturn(order);
        when(productMapper.findById(10)).thenReturn(product);

        boolean success = orderService.cancelOrder(1, 100);

        assertEquals(true, success);
        verify(productMapper).updateById(10, 1);
        verify(userMapper, never()).addBalance(any(Integer.class), any(Double.class));
        verify(userMapper, never()).deductFrozenBalance(any(Integer.class), any(Double.class));
    }

    @Test
    void cancelPaidOrderMustNotChangeFundsOrStatus() {
        when(orderMapper.getByIdForUpdate(1)).thenReturn(order(1, 100, 200, "99.90", 2, "balance"));

        boolean success = orderService.cancelOrder(1, 100);

        assertEquals(false, success);
        verify(orderMapper, never()).updateStatus(any(Order.class));
        verify(userMapper, never()).addBalance(any(Integer.class), any(Double.class));
        verify(userMapper, never()).deductFrozenBalance(any(Integer.class), any(Double.class));
    }

    @Test
    void alipayPaymentMustNotDeductPlatformBalanceAgain() {
        Order order = order(1, 100, 200, "99.90", 1, null);
        when(orderMapper.selectByOrderNoForUpdate("ORDER-TEST")).thenReturn(order);
        when(userMapper.selectById(200)).thenReturn(user(200, "0.00", "0.00"));

        orderService.paySuccess("ORDER-TEST", "ALIPAY-TRADE-1");

        verify(orderMapper).updateStatus(orderCaptor.capture());
        assertEquals("alipay", orderCaptor.getValue().getPaymentMethod());
        assertEquals("ALIPAY-TRADE-1", orderCaptor.getValue().getTradeNo());
        verify(userMapper, never()).deductBalance(100, 99.90);
    }

    private Product availableProduct(Integer id, Integer sellerId, String price) {
        Product product = new Product();
        product.setId(id);
        product.setSellerId(sellerId);
        product.setPrice(new BigDecimal(price));
        product.setStatus(1);
        return product;
    }

    private Order order(Integer id, Integer buyerId, Integer sellerId, String price, Integer status, String paymentMethod) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo("ORDER-TEST");
        order.setProductId(10);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setPrice(new BigDecimal(price));
        order.setStatus(status);
        order.setPaymentMethod(paymentMethod);
        return order;
    }

    private User user(Integer id, String balance, String frozenBalance) {
        User user = new User();
        user.setId(id);
        user.setBalance(new BigDecimal(balance));
        user.setFrozenBalance(new BigDecimal(frozenBalance));
        return user;
    }
}
