package com.cy.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cy.context.BaseContext;
import com.cy.entity.DietLog;
import com.cy.entity.Session;
import com.cy.mapper.DietLogMapper;
import com.cy.mapper.SessionMapper;
import com.cy.service.DietLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DietLogServiceImpl implements DietLogService {

	@Autowired
	private DietLogMapper dietLogMapper;
	@Autowired
	private SessionMapper sessionMapper;

	@Override
	public DietLog getDay(LocalDate date) {
		Long userId = BaseContext.getCurrentId();
		if (userId == null || date == null) return null;
		QueryWrapper<DietLog> qw = new QueryWrapper<>();
		qw.eq("user_id", userId).eq("date", date);
		return dietLogMapper.selectOne(qw);
	}

	@Override
	public List<DietLog> history() {
		Long userId = BaseContext.getCurrentId();
		if (userId == null) return java.util.Collections.emptyList();
		QueryWrapper<DietLog> qw = new QueryWrapper<>();
		qw.eq("user_id", userId).orderByDesc("date");
		return dietLogMapper.selectList(qw);
	}

	@Override
	@Transactional
	public void recomputeDay(Long userId, LocalDate date) {

		if (userId == null || date == null) return;

		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.atTime(LocalTime.MAX);

		// SQL聚合（性能最佳）
		Map<String, Object> sum = sessionMapper.sumByUserAndDate(userId, start, end);

		double totalCalories = ((Number) sum.get("totalCalories")).doubleValue();
		double protein = ((Number) sum.get("protein")).doubleValue();
		double fat = ((Number) sum.get("fat")).doubleValue();
		double carbs = ((Number) sum.get("carbs")).doubleValue();

		// UPSERT（防并发）
		dietLogMapper.upsert(userId, date, totalCalories, protein, fat, carbs);
	}
}

