package com.cy.service;

import com.cy.dto.RecognitionResponseDTO;
import com.cy.dto.RefineRequestDTO;
import com.cy.dto.ImageNutritionDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RecognitionService {

    public RecognitionResponseDTO recognition(String mealType, String userPrompt, List<MultipartFile> files);

    public ImageNutritionDTO refine(RefineRequestDTO request);
}
