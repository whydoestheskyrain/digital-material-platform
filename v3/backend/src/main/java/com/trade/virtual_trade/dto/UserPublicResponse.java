package com.trade.virtual_trade.dto;

import com.trade.virtual_trade.entity.User;
import lombok.Data;

/** 任何访客都可获取的卖家公开资料。 */
@Data
public class UserPublicResponse {
    private Integer id;
    private String username;
    private String nickname;
    private String avatar;

    public static UserPublicResponse from(User user) {
        if (user == null) return null;
        UserPublicResponse response = new UserPublicResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        return response;
    }
}
