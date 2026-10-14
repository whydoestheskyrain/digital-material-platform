package com.trade.virtual_trade.controller;

import com.trade.virtual_trade.common.Result;
import com.trade.virtual_trade.service.NotificationService;
import com.trade.virtual_trade.security.CurrentUser;
import com.trade.virtual_trade.security.RequireAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/list/{userId}")
    public Result list(@PathVariable Integer userId, HttpServletRequest request) {
        if (!isSelfOrAdmin(userId, request)) return Result.error("无权查看其他用户的通知");
        return Result.success(notificationService.getNotifications(userId));
    }

    @GetMapping("/unread/{userId}")
    public Result unread(@PathVariable Integer userId, HttpServletRequest request) {
        if (!isSelfOrAdmin(userId, request)) return Result.error("无权查看其他用户的通知");
        return Result.success(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/count/{userId}")
    public Result count(@PathVariable Integer userId, HttpServletRequest request) {
        if (!isSelfOrAdmin(userId, request)) return Result.error("无权查看其他用户的通知");
        return Result.success(notificationService.countUnread(userId));
    }

    @PutMapping("/read/{id}")
    public Result read(@PathVariable Integer id, HttpServletRequest request) {
        if (!notificationService.markAsReadForUser(CurrentUser.getUserId(request), id)) {
            return Result.error("无权操作此通知");
        }
        return Result.success();
    }

    @PutMapping("/readAll/{userId}")
    public Result readAll(@PathVariable Integer userId, HttpServletRequest request) {
        if (!isSelfOrAdmin(userId, request)) return Result.error("无权操作其他用户的通知");
        notificationService.markAllAsRead(userId);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @RequireAdmin
    public Result delete(@PathVariable Integer id) {
        notificationService.delete(id);
        return Result.success();
    }

    // ====================== 消息会话聚合 ======================

    /** 会话列表：订单按订单号各一个会话，广播/反馈/系统各自聚合 */
    @GetMapping("/threads/{userId}")
    public Result threads(@PathVariable Integer userId, HttpServletRequest request) {
        if (!isSelfOrAdmin(userId, request)) return Result.error("无权查看其他用户的通知");
        return Result.success(notificationService.getThreads(userId));
    }

    /** 某个会话内的消息时间轴（时间正序），key 形如 order_123 / broadcast / feedback / system */
    @GetMapping("/thread/{userId}/{threadKey}")
    public Result threadMessages(@PathVariable Integer userId,
                                 @PathVariable String threadKey,
                                 HttpServletRequest request) {
        if (!isSelfOrAdmin(userId, request)) return Result.error("无权查看其他用户的通知");
        return Result.success(notificationService.getThreadMessages(userId, threadKey));
    }

    /** 整个会话标记已读 */
    @PutMapping("/readThread/{userId}/{threadKey}")
    public Result readThread(@PathVariable Integer userId,
                             @PathVariable String threadKey,
                             HttpServletRequest request) {
        if (!isSelfOrAdmin(userId, request)) return Result.error("无权操作其他用户的通知");
        notificationService.readThread(userId, threadKey);
        return Result.success();
    }

    private boolean isSelfOrAdmin(Integer userId, HttpServletRequest request) {
        return userId != null && (userId.equals(CurrentUser.getUserId(request)) || CurrentUser.isAdmin(request));
    }
}
