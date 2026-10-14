package com.trade.virtual_trade.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GameType {
    private Integer id;
    private String typeName; // 游戏分类名称，如：王者荣耀、原神
    private String icon;     // 分类图标（emoji）
    private Integer sortOrder; // 排序，数字越小越靠前
    private Integer status;  // 状态：1启用 0禁用
    private LocalDateTime createTime;
}
