package com.trade.virtual_trade.dto;

import com.trade.virtual_trade.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 管理员管理用户时使用的资料，不包含密码或密码哈希。 */
@Data
public class UserAdminResponse {
    private Integer id;
    private String username;
    private String nickname;
    private String phone;
    private String role;
    private String avatar;
    private BigDecimal balance;
    private BigDecimal frozenBalance;
    private LocalDateTime createTime;

    public static UserAdminResponse from(User user) {
        if (user == null) return null;
        UserAdminResponse response = new UserAdminResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setAvatar(user.getAvatar());
        response.setBalance(user.getBalance());
        response.setFrozenBalance(user.getFrozenBalance());
        response.setCreateTime(user.getCreateTime());
        return response;
    }
}
