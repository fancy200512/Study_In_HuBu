package com.cy.dto;

import io.swagger.models.auth.In;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 会话分页查询入参
 */
@Data
public class SessionQueryDTO {
	private Integer page;
	private Integer pageSize;
	/**
	 * 开始日期（含），可为空
	 */
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate beginDate;
	/**
	 * 结束日期（含），可为空
	 */
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate endDate;
}

