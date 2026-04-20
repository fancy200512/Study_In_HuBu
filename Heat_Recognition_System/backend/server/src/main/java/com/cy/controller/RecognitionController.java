package com.cy.controller;

import com.cy.dto.ImageNutritionDTO;
import com.cy.dto.RecognitionResponseDTO;
import com.cy.dto.RefineRequestDTO;
import com.cy.result.Result;
import com.cy.service.RecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/recognition")
public class RecognitionController {

    @Autowired
    private RecognitionService recognitionService;

    /**
     * 热量识别(一次识别)
     * @param prompt
     * @param files
     * @return
     */
    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<RecognitionResponseDTO> recognition(
            @RequestParam(value = "mealType", required = false) String mealType,
            @RequestParam(value = "prompt", required = false) String prompt,
            @RequestParam("files") List<MultipartFile> files) {

        RecognitionResponseDTO data = recognitionService.recognition(mealType, prompt, files);
        return Result.success(data);
    }

    /**
     * 二次识别（补充信息）
     */
    @PostMapping("/refine")
    public Result<ImageNutritionDTO> refine(@RequestBody RefineRequestDTO request) {
        return Result.success(recognitionService.refine(request));
    }
}
