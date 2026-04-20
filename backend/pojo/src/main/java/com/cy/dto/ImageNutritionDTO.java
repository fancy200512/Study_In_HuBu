package com.cy.dto;

import lombok.Data;

import java.util.List;

/**
 * 单张图片营养结果
 */
@Data
public class ImageNutritionDTO {

    private Long imageId;
    private String imageUrl;

    private Double totalCalories;
    private Double protein;
    private Double fat;
    private Double carbs;

    private String needMoreInfo;

    private List<RecognitionFollowUpItemDTO> followUpItems;
}
