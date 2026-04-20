package com.cy.dto;

import lombok.Data;

import java.util.List;

/**
 * 单张图片中建议补充的食物信息项
 */
@Data
public class RecognitionFollowUpItemDTO {

    /** 食物名称，例如：火腿肠、米饭 */
    private String foodName;

    /** 需要补充的信息类型：WEIGHT / COUNT / COOKING_METHOD */
    private String infoType;

    /** 给前端直接渲染为可选方块的选项 */
    private List<String> options;
}
