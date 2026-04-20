package com.cy.service;
import com.cy.dto.UserLoginDTO;
import com.cy.entity.User;

public interface AuthService {

    /**
     * 登录
     * @param userLoginDTO
     * @return
     */
    User login(UserLoginDTO userLoginDTO);

}
