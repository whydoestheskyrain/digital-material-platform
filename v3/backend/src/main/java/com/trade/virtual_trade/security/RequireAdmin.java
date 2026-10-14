package com.trade.virtual_trade.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记只能由管理员访问的 Controller 或接口方法。
 *
 * 实际校验由 AuthInterceptor 在调用 Controller 前完成，前端路由限制不能替代此校验。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {
}
