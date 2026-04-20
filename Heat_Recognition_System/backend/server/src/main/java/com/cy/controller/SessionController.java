package com.cy.controller;

import com.cy.dto.SessionQueryDTO;
import com.cy.dto.UpdateMealTypeRequest;
import com.cy.entity.Session;
import com.cy.result.PageResult;
import com.cy.result.Result;
import com.cy.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/session")
public class SessionController {

	@Autowired
	private SessionService sessionService;

	/**
	 * 会话列表（分页，按日期过滤）
	 */
	@GetMapping("/list")
	public Result<PageResult> list(@RequestBody SessionQueryDTO sessionQueryDTO) {
		PageResult pageResult = sessionService.pageQuery(sessionQueryDTO);
		return Result.success(pageResult);
	}

	/**
	 * 会话详情
	 */
	@GetMapping("/{id}")
	public Result<Session> detail(@PathVariable Long id) {
		return Result.success(sessionService.getById(id));
	}

	/**
	 * 删除会话
	 */
	@DeleteMapping("/{id}")
	public Result<Void> delete(@PathVariable Long id) {
		sessionService.deleteById(id);
		return Result.success();
	}

	/**
	 * 修改餐别
	 */
	@PutMapping("/{id}/mealType")
	public Result<Void> updateMealType(@PathVariable Long id, @RequestBody UpdateMealTypeRequest request) {
		sessionService.updateMealType(id, request.getMealType());
		return Result.success();
	}
}
