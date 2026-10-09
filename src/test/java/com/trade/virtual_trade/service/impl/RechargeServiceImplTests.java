package com.trade.virtual_trade.service.impl;

import com.trade.virtual_trade.entity.RechargeOrder;
import com.trade.virtual_trade.entity.User;
import com.trade.virtual_trade.mapper.RechargeOrderMapper;
import com.trade.virtual_trade.mapper.UserMapper;
import com.trade.virtual_trade.service.TransactionLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RechargeServiceImplTests {

    @Mock private RechargeOrderMapper rechargeOrderMapper;
    @Mock private UserMapper userMapper;
    @Mock private TransactionLogService transactionLogService;
    @InjectMocks private RechargeServiceImpl rechargeService;

    @Test
    void firstSuccessfulCallbackMustCreditBalanceOnce() {
        RechargeOrder order = rechargeOrder(1L, 100, "50.00");
        User user = new User();
        user.setId(100);
        user.setBalance(new BigDecimal("10.00"));
        when(rechargeOrderMapper.findByOrderNo("RECHARGE-1")).thenReturn(order);
        when(rechargeOrderMapper.markSuccessIfPending("RECHARGE-1", "ALIPAY-1")).thenReturn(1);
        when(userMapper.selectByIdForUpdate(100)).thenReturn(user);
        when(userMapper.addBalance(100, 50.0)).thenReturn(1);

        assertTrue(rechargeService.handleAlipayNotify("RECHARGE-1", "ALIPAY-1"));

        verify(userMapper).addBalance(100, 50.0);
        verify(transactionLogService).recordLog(100, 50.0, "recharge", 1L, 10.0, 60.0,
                "账户充值 RECHARGE-1");
    }

    @Test
    void duplicateSuccessfulCallbackMustNotCreditBalanceAgain() {
        when(rechargeOrderMapper.findByOrderNo("RECHARGE-1"))
                .thenReturn(rechargeOrder(1L, 100, "50.00"));
        when(rechargeOrderMapper.markSuccessIfPending("RECHARGE-1", "ALIPAY-1")).thenReturn(0);

        assertTrue(rechargeService.handleAlipayNotify("RECHARGE-1", "ALIPAY-1"));

        verify(userMapper, never()).addBalance(100, 50.0);
    }

    private RechargeOrder rechargeOrder(Long id, Integer userId, String amount) {
        RechargeOrder order = new RechargeOrder();
        order.setId(id);
        order.setUserId(userId);
        order.setAmount(new BigDecimal(amount));
        order.setStatus(0);
        return order;
    }
}
