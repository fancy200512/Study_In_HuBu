package com.cy.entity;

import com.cy.dto.RecognitionFollowUpItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI返回的营养结构
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NutritionResult {

    private Double totalCalories;
    private Double protein;
    private Double fat;
    private Double carbs;

    private String needMoreInfo; // WEIGHT / COUNT / COOKING_METHOD / NONE

    private List<RecognitionFollowUpItemDTO> followUpItems;
}
