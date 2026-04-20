package com.cy.dto;

import lombok.Data;
import java.util.List;

/**
 * 一次识别返回结果
 */
@Data
public class RecognitionResponseDTO {

    /** 会话ID */
    private Long sessionId;

    /** 每张图片结果 */
    private List<ImageNutritionDTO> images;

    /** 会话总营养 */
    private Double totalCalories;
    private Double protein;
    private Double fat;
    private Double carbs;
}