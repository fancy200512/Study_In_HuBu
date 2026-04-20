package com.cy.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cy.context.BaseContext;
import com.cy.dto.CaloriesTrendPoint;
import com.cy.dto.NutritionRatioDTO;
import com.cy.entity.DietLog;
import com.cy.mapper.DietLogMapper;
import com.cy.service.AnalyticsService;
import com.cy.utils.ChatClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

	private static final int DAYS = 7;

	@Autowired
	private DietLogMapper dietLogMapper;

	@Autowired
	private ChatClientUtil chatClientUtil;

	@Override
	public List<CaloriesTrendPoint> getCaloriesTrend() {
		Long userId = BaseContext.getCurrentId();
		LocalDate today = LocalDate.now();
		LocalDate start = today.minusDays(DAYS - 1);
		QueryWrapper<DietLog> qw = new QueryWrapper<>();
		qw.eq("user_id", userId).between("date", start, today).orderByAsc("date");
		List<DietLog> logs = dietLogMapper.selectList(qw);
		// 按日期补全
		List<CaloriesTrendPoint> out = new ArrayList<>();
		for (int i = 0; i < DAYS; i++) {
			LocalDate d = start.plusDays(i);
			double cal = 0;
			for (DietLog l : logs) {
				if (d.equals(l.getDate())) {
					cal = l.getTotalCalories() == null ? 0 : l.getTotalCalories();
					break;
				}
			}
			out.add(new CaloriesTrendPoint(d.toString(), cal));
		}
		return out;
	}

	@Override
	public NutritionRatioDTO getNutritionRatio() {
		Long userId = BaseContext.getCurrentId();
		LocalDate today = LocalDate.now();
		LocalDate start = today.minusDays(DAYS - 1);
		QueryWrapper<DietLog> qw = new QueryWrapper<>();
		qw.eq("user_id", userId).between("date", start, today);
		List<DietLog> logs = dietLogMapper.selectList(qw);
		double p = 0, f = 0, c = 0;
		for (DietLog l : logs) {
			p += l.getProtein() == null ? 0 : l.getProtein();
			f += l.getFat() == null ? 0 : l.getFat();
			c += l.getCarbs() == null ? 0 : l.getCarbs();
		}
		double sum = p + f + c;
		if (sum <= 0) return new NutritionRatioDTO(0d, 0d, 0d);
		return new NutritionRatioDTO(
				roundPercent(p / sum * 100),
				roundPercent(f / sum * 100),
				roundPercent(c / sum * 100)
		);
	}

	@Override
	public List<String> getAdvice() {
		// 简单规则：对当天摄入进行判断
		Long userId = BaseContext.getCurrentId();
		LocalDate today = LocalDate.now();
		QueryWrapper<DietLog> qw = new QueryWrapper<>();
		qw.eq("user_id", userId).eq("date", today);
		DietLog d = dietLogMapper.selectOne(qw);
		List<String> adv = new ArrayList<>();
		if (d == null || (safe(d.getTotalCalories()) + safe(d.getProtein()) + safe(d.getFat()) + safe(d.getCarbs())) == 0) {
			adv.add("今天还没有记录饮食，可适量补充高蛋白食物。");
			return adv;
		}
		double p = safe(d.getProtein()), f = safe(d.getFat()), c = safe(d.getCarbs());
		double sum = p + f + c;
		if (sum > 0) {
			double pr = p / sum;
			if (pr < 0.2) adv.add("蛋白质摄入偏低，建议增加瘦肉、鸡蛋、奶制品。");
			if (f / sum > 0.35) adv.add("脂肪摄入偏高，请少油烹饪，控制油炸食品。");
			if (c / sum > 0.6) adv.add("碳水占比偏高，主食建议粗细搭配。");
		}
		// 预留：可拼装 prompt 调用 ChatClientUtil 获取更详尽建议
		// String ai = chatClientUtil.multiModalityChat(prompt, List.of()); // 仅文本
		return adv.isEmpty() ? java.util.List.of("当前饮食较为均衡，请继续保持。") : adv;
	}

	private static double safe(Double v) { return v == null ? 0 : v; }
	private static double roundPercent(double v) { return Math.round(v * 10.0) / 10.0; }
}

