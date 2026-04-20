package com.cy.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.context.BaseContext;
import com.cy.dto.SessionQueryDTO;
import com.cy.entity.Session;
import com.cy.mapper.SessionMapper;
import com.cy.result.PageResult;
import com.cy.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class SessionServiceImpl implements SessionService {

	@Autowired
	private SessionMapper sessionMapper;


	@Override
	public PageResult pageQuery(SessionQueryDTO query) {

		Long userId = BaseContext.getCurrentId();
		if (userId == null) {
			return new PageResult(0, java.util.Collections.emptyList());
		}

		int pageNum = Math.max(1, query.getPage());
		int pageSize = Math.max(1, query.getPageSize());

		LocalDateTime begin = null, end = null;
		if (query.getBeginDate() != null) begin = query.getBeginDate().atStartOfDay();
		if (query.getEndDate() != null) end = query.getEndDate().atTime(LocalTime.MAX);

		// ⭐ 核心：构造分页对象
		Page<Session> page = new Page<>(pageNum, pageSize);

		// ⭐ 调用 mapper
		IPage<Session> result = sessionMapper.pageList(page, userId, begin, end);

		return new PageResult(result.getTotal(), result.getRecords());
	}

	@Override
	public Session getById(Long id) {
		Long userId = BaseContext.getCurrentId();
		Session s = sessionMapper.selectById(id);
		if (s == null || (userId != null && !userId.equals(s.getUserId()))) {
			return null;
		}
		return s;
	}

	@Override
	public void deleteById(Long id) {
		Long userId = BaseContext.getCurrentId();
		Session s = sessionMapper.selectById(id);
		if (s != null && (userId == null || userId.equals(s.getUserId()))) {
			sessionMapper.deleteById(id);
		}
	}

	@Override
	public void updateMealType(Long id, String mealType) {
		Long userId = BaseContext.getCurrentId();
		Session s = sessionMapper.selectById(id);
		if (s == null) return;
		if (userId != null && !userId.equals(s.getUserId())) return;
		s.setMealType(mealType);
		sessionMapper.updateById(s);
	}
}
