package com.cy.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cy.entity.UserBodyProfile;
import com.cy.entity.UserProfile;
import com.cy.mapper.UserBodyProfileMapper;
import com.cy.mapper.UserProfileMapper;
import com.cy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

	@Autowired
	private UserProfileMapper userProfileMapper;

	@Autowired
	private UserBodyProfileMapper userBodyProfileMapper;

	@Override
	public UserProfile getUserProfile(Long userId) {
		if (userId == null) return null;
		QueryWrapper<UserProfile> qw = new QueryWrapper<>();
		qw.eq("user_id", userId).last("limit 1");
		return userProfileMapper.selectOne(qw);
	}

	@Override
	public void updateUserProfile(UserProfile userProfile) {
		if (userProfile == null || userProfile.getUserId() == null) return;
		// 先按 user_id 查询是否存在
		UserProfile existing = getUserProfile(userProfile.getUserId());
		if (existing == null) {
			userProfileMapper.insert(userProfile);
		} else {
			// 局部更新：仅合并非空字段，避免把未传字段覆盖为 null
			UserProfile merged = mergeUserProfile(existing, userProfile);
			userProfileMapper.updateById(merged);
		}
	}

	@Override
	public UserBodyProfile getUserBodyProfile(Long userId) {
		if (userId == null) return null;
		QueryWrapper<UserBodyProfile> qw = new QueryWrapper<>();
		qw.eq("user_id", userId).last("limit 1");
		return userBodyProfileMapper.selectOne(qw);
	}

	@Override
	public void updateUserBodyProfile(UserBodyProfile userBodyProfile) {
		if (userBodyProfile == null || userBodyProfile.getUserId() == null) return;
		UserBodyProfile existing = getUserBodyProfile(userBodyProfile.getUserId());
		if (existing == null) {
			userBodyProfileMapper.insert(userBodyProfile);
		} else {
			UserBodyProfile merged = mergeUserBodyProfile(existing, userBodyProfile);
			userBodyProfileMapper.updateById(merged);
		}
	}

	private UserProfile mergeUserProfile(UserProfile base, UserProfile patch) {
		// id 与 userId 保持基准对象
		UserProfile out = new UserProfile();
		out.setId(base.getId());
		out.setUserId(base.getUserId());

		// 仅当 patch 字段非 null 时覆盖
		out.setNickname(patch.getNickname() != null ? patch.getNickname() : base.getNickname());
		out.setAvatar(patch.getAvatar() != null ? patch.getAvatar() : base.getAvatar());
		out.setGender(patch.getGender() != null ? patch.getGender() : base.getGender());
		return out;
	}

	private UserBodyProfile mergeUserBodyProfile(UserBodyProfile base, UserBodyProfile patch) {
		UserBodyProfile out = new UserBodyProfile();
		out.setId(base.getId());
		out.setUserId(base.getUserId());

		out.setHeight(patch.getHeight() != null ? patch.getHeight() : base.getHeight());
		out.setWeight(patch.getWeight() != null ? patch.getWeight() : base.getWeight());
		out.setBodyFat(patch.getBodyFat() != null ? patch.getBodyFat() : base.getBodyFat());
		out.setAge(patch.getAge() != null ? patch.getAge() : base.getAge());
		out.setBmi(patch.getBmi() != null ? patch.getBmi() : base.getBmi());
		out.setBmr(patch.getBmr() != null ? patch.getBmr() : base.getBmr());
		out.setTargetCalories(patch.getTargetCalories() != null ? patch.getTargetCalories() : base.getTargetCalories());
		out.setUpdateTime(patch.getUpdateTime() != null ? patch.getUpdateTime() : base.getUpdateTime());
		return out;
	}
}
