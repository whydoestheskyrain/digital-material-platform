package com.trade.virtual_trade.service;

import com.trade.virtual_trade.entity.Notification;
import com.trade.virtual_trade.entity.NotificationThread;

import java.util.List;

public interface NotificationService {
    void sendOrderNotification(Integer userId, String title, String content, Integer orderId);
    void sendBroadcast(String title, String content);
    void sendFeedbackNotification(Integer userId, String title, String content);
    List<Notification> getNotifications(Integer userId);
    List<Notification> getUnreadNotifications(Integer userId);
    void markAsRead(Integer id);
    void markAllAsRead(Integer userId);
    int countUnread(Integer userId);
    void delete(Integer id);

    /** 消息会话列表：订单按 orderId 各一个会话，广播/反馈/系统各自聚合 */
    List<NotificationThread> getThreads(Integer userId);

    /** 某个会话内的消息（时间正序，用于时间轴详情） */
    List<Notification> getThreadMessages(Integer userId, String threadKey);

    /** 整个会话标记已读 */
    void readThread(Integer userId, String threadKey);
}
