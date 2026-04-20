-- 用户表 user
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    password VARCHAR(255) NOT NULL COMMENT '加密密码',
    status TINYINT DEFAULT 1 COMMENT '状态（1正常 0禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_email (email),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';


-- 用户信息表 user_profile
CREATE TABLE user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    gender TINYINT COMMENT '性别（0未知 1男 2女）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';


-- 用户身体信息表 user_body_profile
CREATE TABLE user_body_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    height DOUBLE COMMENT '身高(cm)',
    weight DOUBLE COMMENT '体重(kg)',
    body_fat DOUBLE COMMENT '体脂率(%)',
    age INT COMMENT '年龄',
    bmi DOUBLE COMMENT 'BMI',
    bmr DOUBLE COMMENT '基础代谢',
    target_calories DOUBLE COMMENT '目标每日摄入热量',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户身体信息表';

-- 会话表 session
CREATE TABLE session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    meal_type VARCHAR(20) COMMENT '餐次（BREAKFAST/LUNCH/DINNER/SNACK/CUSTOM）',
    custom_meal_name VARCHAR(50) COMMENT '自定义餐次名称',
    total_calories DOUBLE COMMENT '总热量',
    protein DOUBLE COMMENT '蛋白质',
    fat DOUBLE COMMENT '脂肪',
    carbs DOUBLE COMMENT '碳水',
    status VARCHAR(20) DEFAULT 'INIT' COMMENT '状态（INIT/DONE）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_meal_type (meal_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表';


-- 图片表 image
CREATE TABLE image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL COMMENT '会话ID',
    image_url VARCHAR(255) NOT NULL COMMENT '图片地址',

    -- AI识别结果（聚合）
    total_calories DOUBLE,
    protein DOUBLE,
    fat DOUBLE,
    carbs DOUBLE,

    -- 用户补充信息
    extra_info TEXT COMMENT '用户补充描述',

    -- AI建议补充信息
    ai_prompt_hint TEXT COMMENT 'AI提示补充内容',

    -- AI原始返回（JSON）
    ai_raw_result TEXT COMMENT '首次识别结果JSON',

    -- 二次优化结果
    ai_refined_result TEXT COMMENT '二次识别结果JSON',
    is_refined TINYINT DEFAULT 0 COMMENT '是否经过二次优化',
    source_type VARCHAR(20) DEFAULT 'AI' COMMENT '来源（AI/USER）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片及营养分析表';


-- 饮食日志表 diet_log
CREATE TABLE diet_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    date DATE NOT NULL COMMENT '日期（yyyy-MM-dd）',
    total_calories DOUBLE DEFAULT 0 COMMENT '总热量(kcal)',
    protein DOUBLE DEFAULT 0 COMMENT '蛋白质(g)',
    fat DOUBLE DEFAULT 0 COMMENT '脂肪(g)',
    carbs DOUBLE DEFAULT 0 COMMENT '碳水(g)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_date (user_id, date),
    INDEX idx_user_id (user_id),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日饮食日志（按天聚合）';

-- 建议表 advice
CREATE TABLE advice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    related_date DATE NOT NULL COMMENT '对应日期',
    content TEXT COMMENT '建议内容',
    type VARCHAR(20) COMMENT '类型（CALORIES/NUTRITION）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, related_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮食建议表';







































