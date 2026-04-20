package com.cy.service;

import com.cy.dto.SessionListDTO;
import com.cy.dto.SessionQueryDTO;
import com.cy.result.PageResult;
import com.cy.entity.Session;

import java.time.LocalDate;

public interface SessionService {
	PageResult pageQuery(SessionQueryDTO query);
	Session getById(Long id);
	void deleteById(Long id);
	void updateMealType(Long id, String mealType);
}

