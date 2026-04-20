package com.cy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录
 */
@Data
public class UserLoginDTO implements Serializable {

    /**
     * 登录类型：
     * PASSWORD = 密码登录
     * CODE = 验证码登录
     */
    private String loginType;

    /** 邮箱或手机号 */
    private String account;

    /** 密码（密码登录用） */
        private String password;

    /** 验证码（验证码登录用） */
    private String code;

}
