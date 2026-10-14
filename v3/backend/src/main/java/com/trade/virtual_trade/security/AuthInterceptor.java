package com.trade.virtual_trade.security;

import com.trade.virtual_trade.entity.User;
import com.trade.virtual_trade.mapper.UserMapper;
import com.trade.virtual_trade.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 统一认证和管理员授权入口。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS 预检请求不携带登录信息，应直接放行。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
            return false;
        }

        Integer userId = JwtUtil.getUserId(authorization.substring("Bearer ".length()));
        if (userId == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "登录已失效，请重新登录");
            return false;
        }

        // 每次请求确认用户仍存在，并从数据库获取当前角色；JWT 中不保存 role，角色变更会立即生效。
        User user = userMapper.selectById(userId);
        if (user == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "用户不存在或已被删除");
            return false;
        }

        request.setAttribute(CurrentUser.USER_ID_ATTRIBUTE, user.getId());
        request.setAttribute(CurrentUser.ROLE_ATTRIBUTE, user.getRole());

        if (requiresAdmin(handler) && !"admin".equalsIgnoreCase(user.getRole())) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "需要管理员权限");
            return false;
        }

        return true;
    }

    private boolean requiresAdmin(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return false;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        return handlerMethod.hasMethodAnnotation(RequireAdmin.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAdmin.class);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + message + "\"}");
    }
}
