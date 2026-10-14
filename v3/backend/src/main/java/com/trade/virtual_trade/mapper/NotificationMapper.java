package com.trade.virtual_trade.mapper;

import com.trade.virtual_trade.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {
    void insert(Notification notification);

    /**
     * 用户可见的全部消息（本人消息 + 全员广播），按时间倒序。
     * 返回的 status 表示“该用户是否已读”（联 notification_read 表计算），
     * 并附带联查得到的 orderNo。
     */
    List<Notification> selectVisibleByUserId(@Param("userId") Integer userId);

    /** 按用户统计未读数（广播未读不再共享） */
    int countUnread(@Param("userId") Integer userId);

    /** 写入单条已读记录 */
    void insertRead(@Param("userId") Integer userId, @Param("notificationId") Integer notificationId);

    /**
     * 整组写入已读：
     * orderId != null 时按 type='order' + order_id；
     * 否则按 type（broadcast/feedback），type='system' 时为其余全部类型。
     */
    void insertReadByThread(@Param("userId") Integer userId,
                            @Param("type") String type,
                            @Param("orderId") Integer orderId);

    /** 当前用户可见的全部消息置为已读 */
    void insertReadAllVisible(@Param("userId") Integer userId);

    void updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    void deleteById(@Param("id") Integer id);

    List<Notification> selectBroadcast();
}
