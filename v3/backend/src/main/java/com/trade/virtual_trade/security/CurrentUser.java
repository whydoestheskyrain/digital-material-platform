package com.trade.virtual_trade.security;

import javax.servlet.http.HttpServletRequest;

/**
 * 当前请求中已验证的用户信息。
 * 这些属性只由 AuthInterceptor 写入，业务代码不能信任客户端传入的 userId 或 role。
 */
public final class CurrentUser {

    public static final String USER_ID_ATTRIBUTE = "currentUserId";
    public static final String ROLE_ATTRIBUTE = "currentUserRole";

    private CurrentUser() {
    }

    public static Integer getUserId(HttpServletRequest request) {
        Object value = request.getAttribute(USER_ID_ATTRIBUTE);
        if (!(value instanceof Integer)) {
            throw new IllegalStateException("当前接口需要登录用户");
        }
        return (Integer) value;
    }

    public static boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute(ROLE_ATTRIBUTE);
        return role != null && "admin".equalsIgnoreCase(String.valueOf(role));
    }
}
