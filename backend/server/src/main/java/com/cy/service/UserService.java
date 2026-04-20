package com.cy.service;

import com.cy.entity.UserBodyProfile;
import com.cy.entity.UserProfile;

public interface UserService {

	/**
	 * 用户信息查询与更新
	 */
	UserProfile getUserProfile(Long userId);

	void updateUserProfile(UserProfile userProfile);

	/**
	 * 用户身体信息查询与更新
	 */
	UserBodyProfile getUserBodyProfile(Long userId);

	void updateUserBodyProfile(UserBodyProfile userBodyProfile);
}
