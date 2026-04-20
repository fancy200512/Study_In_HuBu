package com.cy.controller;

import com.cy.entity.UserBodyProfile;
import com.cy.entity.UserProfile;
import com.cy.result.Result;
import com.cy.service.UserService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Api(tags = "用户相关接口")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户信息
     * @return
     */
    @GetMapping("/user/profile/{userId}")
    public Result<UserProfile> getUserProfile(@PathVariable Long userId) {
        UserProfile profile = userService.getUserProfile(userId);
        return Result.success(profile);
    }

    /**
     * 更新用户信息
     * @return
     */
    @PutMapping("/user/profile")
    public Result updateUserProfile(@RequestBody UserProfile userProfile) {
        userService.updateUserProfile(userProfile);
        return Result.success();
    }

    /**
     * 获取用户身体信息
     * @return
     */
    @GetMapping("/user/body/{userId}")
    public Result<UserBodyProfile> getUserBodyProfile(@PathVariable Long userId) {
        UserBodyProfile body = userService.getUserBodyProfile(userId);
        return Result.success(body);
    }

    /**
     * 更新用户身体信息
     * @return
     */
    @PutMapping("/user/body")
    public Result updateUserBody(@RequestBody UserBodyProfile userBodyProfile) {
        userService.updateUserBodyProfile(userBodyProfile);
        return Result.success();
    }



}
