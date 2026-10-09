CREATE TABLE IF NOT EXISTS `notification` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
  `user_id` INT COMMENT '接收用户ID（NULL表示广播）',
  `type` VARCHAR(20) NOT NULL COMMENT '消息类型：order/feedback/broadcast/system',
  `title` VARCHAR(100) NOT NULL COMMENT '消息标题',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `order_id` INT COMMENT '关联订单ID',
  `status` INT DEFAULT 0 COMMENT '状态：0未读 1已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`order_id`) REFERENCES `order`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

CREATE TABLE IF NOT EXISTS `notification_read` (
  `user_id` INT NOT NULL COMMENT '用户ID',
  `notification_id` INT NOT NULL COMMENT '消息ID（含广播）',
  `read_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  PRIMARY KEY (`user_id`, `notification_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`notification_id`) REFERENCES `notification`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息已读记录表（按用户记录，解决广播共享行一人已读全员已读）';

CREATE TABLE IF NOT EXISTS `feedback` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '反馈ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(100) NOT NULL COMMENT '反馈标题',
  `content` TEXT NOT NULL COMMENT '反馈内容',
  `status` INT DEFAULT 0 COMMENT '状态：0待处理 1已回复 2已关闭',
  `reply` TEXT COMMENT '管理员回复',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `reply_time` DATETIME COMMENT '回复时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

CREATE TABLE IF NOT EXISTS `pet_skin` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '皮肤ID',
  `skin_name` VARCHAR(50) NOT NULL COMMENT '皮肤名称',
  `idle_ani` VARCHAR(255) NOT NULL COMMENT '待机动态webp地址',
  `click_ani` VARCHAR(255) COMMENT '点击弹跳webp动图',
  `main_img` VARCHAR(255) NOT NULL COMMENT '皮肤缩略预览图',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认',
  `status` TINYINT DEFAULT 1 COMMENT '1启用 0下架',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桌宠皮肤表';

CREATE TABLE IF NOT EXISTS `user_pet` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `skin_id` INT NOT NULL COMMENT '皮肤ID',
  `pos_x` INT DEFAULT 1300 COMMENT '上次拖拽X坐标',
  `pos_y` INT DEFAULT 650 COMMENT '上次拖拽Y坐标',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_uid` (`user_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`skin_id`) REFERENCES `pet_skin`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户桌宠配置表';

-- ====================== 旧库升级：game_type 增加图标/排序/启用状态（幂等，可重复执行） ======================
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'game_type' AND COLUMN_NAME = 'icon');
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `game_type` ADD COLUMN `icon` VARCHAR(20) DEFAULT NULL COMMENT ''分类图标(emoji)''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'game_type' AND COLUMN_NAME = 'sort_order');
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `game_type` ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT ''排序，数字越小越靠前''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'game_type' AND COLUMN_NAME = 'status');
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `game_type` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT ''状态：1启用 0禁用''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
