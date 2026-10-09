package com.trade.virtual_trade.dto;

import com.trade.virtual_trade.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/** 当前登录用户自己的资料，不包含密码或密码哈希。 */
@Data
public class UserSelfResponse {
    private Integer id;
    private String username;
    private String nickname;
    private String phone;
    private String role;
    private String avatar;
    private LocalDateTime createTime;

    public static UserSelfResponse from(User user) {
        if (user == null) return null;
        UserSelfResponse response = new UserSelfResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setAvatar(user.getAvatar());
        response.setCreateTime(user.getCreateTime());
        return response;
    }
}
