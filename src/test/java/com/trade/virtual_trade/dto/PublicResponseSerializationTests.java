package com.trade.virtual_trade.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.virtual_trade.entity.Product;
import com.trade.virtual_trade.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 防止以后有人把实体对象直接作为公开接口响应，重新泄露敏感字段。
 */
class PublicResponseSerializationTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publicProductResponseMustNotSerializeCredentials() throws Exception {
        Product product = new Product();
        product.setId(1);
        product.setTitle("测试商品");
        product.setGameAccount("secret-account");
        product.setGamePassword("secret-password");
        product.setBindInfo("实名绑定资料");

        String json = objectMapper.writeValueAsString(ProductPublicResponse.from(product));

        assertTrue(json.contains("测试商品"));
        assertFalse(json.contains("secret-account"));
        assertFalse(json.contains("secret-password"));
        assertFalse(json.contains("实名绑定资料"));
    }

    @Test
    void publicUserResponseMustNotSerializePrivateData() throws Exception {
        User user = new User();
        user.setId(1);
        user.setUsername("seller");
        user.setPassword("bcrypt-password-hash");
        user.setPhone("13800138000");
        user.setBalance(new BigDecimal("100.00"));
        user.setFrozenBalance(new BigDecimal("50.00"));

        String json = objectMapper.writeValueAsString(UserPublicResponse.from(user));

        assertTrue(json.contains("seller"));
        assertFalse(json.contains("bcrypt-password-hash"));
        assertFalse(json.contains("13800138000"));
        assertFalse(json.contains("100.00"));
        assertFalse(json.contains("50.00"));
    }
}
