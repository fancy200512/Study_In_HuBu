package com.cy.service;

import com.cy.entity.DietLog;

import java.time.LocalDate;
import java.util.List;

public interface DietLogService {
	DietLog getDay(LocalDate date);
	List<DietLog> history();
	void recomputeDay(Long userId, LocalDate date);
}

