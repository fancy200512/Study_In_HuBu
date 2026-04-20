package com.cy.service;

import com.cy.dto.CaloriesTrendPoint;
import com.cy.dto.NutritionRatioDTO;

import java.util.List;

public interface AnalyticsService {
	/**
	 * 热量趋势（默认近7天）
	 */
	List<CaloriesTrendPoint> getCaloriesTrend();

	/**
	 * 营养比例（默认近7天累积后计算比例）
	 */
	NutritionRatioDTO getNutritionRatio();

	/**
	 * 每日建议（简单规则 + 预留AI）
	 */
	List<String> getAdvice();
}

