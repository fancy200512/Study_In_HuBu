package com.cy.controller;

import com.cy.dto.CaloriesTrendPoint;
import com.cy.dto.NutritionRatioDTO;
import com.cy.result.Result;
import com.cy.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 健康分析接口
 */
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

	@Autowired
	private AnalyticsService analyticsService;

	/**
	 * 热量趋势（默认近7天）
	 */
	@GetMapping("/calories")
	public Result<List<CaloriesTrendPoint>> caloriesTrend() {
		return Result.success(analyticsService.getCaloriesTrend());
	}

	/**
	 * 营养比例（默认近7天累积）
	 */
	@GetMapping("/nutrition")
	public Result<NutritionRatioDTO> nutritionRatio() {
		return Result.success(analyticsService.getNutritionRatio());
	}

	/**
	 * 每日建议（简单规则 + 预留AI）
	 */
	@GetMapping("/advice")
	public Result<List<String>> advice() {
		return Result.success(analyticsService.getAdvice());
	}
}

