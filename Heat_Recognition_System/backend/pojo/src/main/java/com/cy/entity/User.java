package com.cy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户表（仅存登录认证信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /** 用户ID */
    private Long id;

    /** 邮箱（唯一） */
    private String email;

    /** 手机号（唯一） */
    private String phone;

    /** 加密后的密码 */
    private String password;

    /** 状态：1正常 0禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}