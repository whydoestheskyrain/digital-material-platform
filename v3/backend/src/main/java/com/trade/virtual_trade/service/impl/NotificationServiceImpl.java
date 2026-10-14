package com.trade.virtual_trade.service.impl;

import com.trade.virtual_trade.entity.Notification;
import com.trade.virtual_trade.entity.NotificationThread;
import com.trade.virtual_trade.mapper.NotificationMapper;
import com.trade.virtual_trade.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    public void sendOrderNotification(Integer userId, String title, String content, Integer orderId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType("order");
        notification.setTitle(title);
        notification.setContent(content);
        notification.setOrderId(orderId);
        notification.setStatus(0);
        notificationMapper.insert(notification);
    }

    @Override
    public void sendBroadcast(String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(null);
        notification.setType("broadcast");
        notification.setTitle(title);
        notification.setContent(content);
        notification.setStatus(0);
        notificationMapper.insert(notification);
    }

    @Override
    public void sendFeedbackNotification(Integer userId, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType("feedback");
        notification.setTitle(title);
        notification.setContent(content);
        notification.setStatus(0);
        notificationMapper.insert(notification);
    }

    @Override
    public List<Notification> getNotifications(Integer userId) {
        return notificationMapper.selectVisibleByUserId(userId);
    }

    @Override
    public List<Notification> getUnreadNotifications(Integer userId) {
        List<Notification> all = notificationMapper.selectVisibleByUserId(userId);
        List<Notification> unread = new ArrayList<>();
        for (Notification n : all) {
            if (n.getStatus() != null && n.getStatus() == 0) {
                unread.add(n);
            }
        }
        return unread;
    }

    @Override
    public void markAsRead(Integer id) {
        notificationMapper.updateStatus(id, 1);
    }

    @Override
    public boolean markAsReadForUser(Integer userId, Integer notificationId) {
        for (Notification notification : notificationMapper.selectVisibleByUserId(userId)) {
            if (notificationId.equals(notification.getId())) {
                // 广播与个人消息都写入当前用户的已读回执，避免影响其他用户。
                notificationMapper.insertRead(userId, notificationId);
                return true;
            }
        }
        return false;
    }

    @Override
    public void markAllAsRead(Integer userId) {
        notificationMapper.insertReadAllVisible(userId);
    }

    @Override
    public int countUnread(Integer userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    public void delete(Integer id) {
        notificationMapper.deleteById(id);
    }

    // ====================== 消息会话聚合 ======================

    @Override
    public List<NotificationThread> getThreads(Integer userId) {
        List<Notification> all = notificationMapper.selectVisibleByUserId(userId);
        // SQL 已按时间倒序，每条消息第一次出现时所在会话的最新消息就是它
        Map<String, NotificationThread> threadMap = new LinkedHashMap<>();
        for (Notification n : all) {
            String key = buildThreadKey(n);
            NotificationThread thread = threadMap.get(key);
            if (thread == null) {
                thread = new NotificationThread();
                thread.setThreadKey(key);
                thread.setType(buildThreadType(n));
                thread.setTitle(buildThreadTitle(n));
                thread.setOrderId(n.getOrderId());
                thread.setOrderNo(n.getOrderNo());
                thread.setLastContent(n.getContent());
                thread.setLastTime(n.getCreateTime());
                thread.setUnreadCount(0);
                thread.setTotalCount(0);
                threadMap.put(key, thread);
            }
            thread.setTotalCount(thread.getTotalCount() + 1);
            if (n.getStatus() != null && n.getStatus() == 0) {
                thread.setUnreadCount(thread.getUnreadCount() + 1);
            }
        }
        return new ArrayList<>(threadMap.values());
    }

    @Override
    public List<Notification> getThreadMessages(Integer userId, String threadKey) {
        List<Notification> all = notificationMapper.selectVisibleByUserId(userId);
        List<Notification> messages = new ArrayList<>();
        for (Notification n : all) {
            if (threadKey != null && threadKey.equals(buildThreadKey(n))) {
                messages.add(n);
            }
        }
        // 时间轴按时间正序展示
        messages.sort(Comparator.comparing(Notification::getCreateTime));
        return messages;
    }

    @Override
    public void readThread(Integer userId, String threadKey) {
        Integer orderId = null;
        String type = threadKey;
        if (threadKey != null && threadKey.startsWith("order_")) {
            type = "order";
            try {
                orderId = Integer.parseInt(threadKey.substring("order_".length()));
            } catch (NumberFormatException e) {
                orderId = null;
            }
        }
        notificationMapper.insertReadByThread(userId, type, orderId);
    }

    /** 会话 key：订单消息按 order_id 分组，广播/反馈/系统各一个会话 */
    private String buildThreadKey(Notification n) {
        if ("order".equals(n.getType()) && n.getOrderId() != null) {
            return "order_" + n.getOrderId();
        }
        if ("broadcast".equals(n.getType())) {
            return "broadcast";
        }
        if ("feedback".equals(n.getType())) {
            return "feedback";
        }
        return "system";
    }

    private String buildThreadType(Notification n) {
        String type = n.getType();
        if ("order".equals(type) || "broadcast".equals(type) || "feedback".equals(type)) {
            return type;
        }
        return "system";
    }

    private String buildThreadTitle(Notification n) {
        if ("order".equals(n.getType()) && n.getOrderId() != null) {
            String orderNo = n.getOrderNo();
            if (orderNo != null && !orderNo.isEmpty()) {
                return "订单 " + orderNo;
            }
            return "订单 #" + n.getOrderId();
        }
        if ("broadcast".equals(n.getType())) {
            return "平台公告";
        }
        if ("feedback".equals(n.getType())) {
            return "我的反馈";
        }
        return "系统通知";
    }
}
