package com.cy.controller;

import com.cy.entity.DietLog;
import com.cy.result.Result;
import com.cy.service.DietLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/diet-log")
public class DietLogController {

	@Autowired
	private DietLogService dietLogService;

	/**
	 * 获取某天饮食
	 */
	@GetMapping
	public Result<DietLog> getByDate(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
		return Result.success(dietLogService.getDay(date));
	}

	/**
	 * 历史记录（按日期倒序）
	 */
	@GetMapping("/history")
	public Result<List<DietLog>> history() {
		return Result.success(dietLogService.history());
	}
}
