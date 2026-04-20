package com.cy.controller;


import com.cy.constant.JwtClaimsConstant;
import com.cy.dto.UserLoginDTO;
import com.cy.entity.User;
import com.cy.properties.JwtProperties;
import com.cy.result.Result;
import com.cy.service.AuthService;
import com.cy.utils.JwtUtil;
import com.cy.vo.UserLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/auth")
@Api(tags = "认证相关接口")
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("登录:{}",userLoginDTO.getAccount());

        User user =  authService.login(userLoginDTO);

        //为用户生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID,user.getId());

        String token = JwtUtil.createJWT(jwtProperties.getSecretKey(), jwtProperties.getTtl(), claims);
        log.info("当前token:{}",token);

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .token(token)
                .build();
        return Result.success(userLoginVO);
    }

    /**
     * 退出
     * @return
     */
    @PostMapping("/logout")
    public Result logout() {
        return Result.success();
    }
}
