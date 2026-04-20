package com.cy.service.Impl;

import com.cy.constant.MessageConstant;
import com.cy.dto.UserLoginDTO;
import com.cy.entity.User;
import com.cy.exception.LoginFailedException;
import com.cy.mapper.UserMapper;
import com.cy.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public User login(UserLoginDTO userLoginDTO) {
        String account = userLoginDTO.getAccount();
        if (account == null || account.isBlank()) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        String loginType = userLoginDTO.getLoginType();
        if ("PASSWORD".equalsIgnoreCase(loginType)) {
            // 使用邮箱或手机号查询
            QueryWrapper<User> qw = new QueryWrapper<>();
            qw.eq("email", account).or().eq("phone", account);
            User user = userMapper.selectOne(qw);
            if (user == null) {
                throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
            }
            // 最小可运行版：明文比较（如需加密请替换为加密校验）
            if (userLoginDTO.getPassword() == null || !userLoginDTO.getPassword().equals(user.getPassword())) {
                throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
            }
            return user;
        } else if ("CODE".equalsIgnoreCase(loginType)) {
            // 最小可运行版：简单校验验证码为 123456
            if (!"123456".equals(userLoginDTO.getCode())) {
                throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
            }
            QueryWrapper<User> qw = new QueryWrapper<>();
            qw.eq("email", account).or().eq("phone", account);
            User user = userMapper.selectOne(qw);
            if (user == null) {
                // 自动注册（按邮箱/手机号落表）
                user = new User();
                if (account.contains("@")) {
                    user.setEmail(account);
                } else {
                    user.setPhone(account);
                }
                user.setPassword("");
                user.setStatus(1);
                user.setCreateTime(LocalDateTime.now());
                userMapper.insert(user);
            }
            return user;
        } else {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
    }
}
