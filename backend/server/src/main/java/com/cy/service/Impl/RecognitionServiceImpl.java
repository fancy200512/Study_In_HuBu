package com.cy.service.Impl;

import com.cy.constant.AISystemPromptConstant;
import com.cy.context.BaseContext;
import com.cy.dto.ImageNutritionDTO;
import com.cy.dto.RecognitionFollowUpItemDTO;
import com.cy.dto.RecognitionResponseDTO;
import com.cy.dto.RefineRequestDTO;
import com.cy.entity.Image;
import com.cy.entity.NutritionResult;
import com.cy.entity.Session;
import com.cy.exception.BaseException;
import com.cy.mapper.ImageMapper;
import com.cy.mapper.DietLogMapper;
import com.cy.mapper.SessionMapper;
import com.cy.rag.FoodRecognitionRagService;
import com.cy.service.DietLogService;
import com.cy.service.RecognitionService;
import com.cy.utils.AliOssUtil;
import com.cy.utils.ChatClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.net.URL;
import java.net.URLConnection;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.stream.Collectors;


@Slf4j
@Service
public class RecognitionServiceImpl implements RecognitionService {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private ImageMapper imageMapper;

    @Autowired
    private AliOssUtil aliOssUtil;

    @Autowired
    private ChatClientUtil chatClientUtil;

    @Autowired
    private DietLogMapper dietLogMapper;

    @Autowired
    private DietLogService dietLogService;

    @Autowired
    private FoodRecognitionRagService foodRecognitionRagService;

    @Override
    public RecognitionResponseDTO recognition(String mealType, String userPrompt, List<MultipartFile> files) {

        Long effectiveUserId = BaseContext.getCurrentId();
        String effectiveMealType = (mealType == null || mealType.isBlank()) ? "LUNCH" : mealType.toUpperCase();
        String prompt = foodRecognitionRagService.augmentPrompt(AISystemPromptConstant.DefaultPrompt, effectiveMealType, userPrompt);

        // 1️⃣ 创建 session
        Session session = new Session();
        session.setUserId(effectiveUserId);
        session.setMealType(effectiveMealType);
        session.setStatus("INIT");

        sessionMapper.insert(session);
        Long sessionId = session.getId();

        // 返回数据
        List<ImageNutritionDTO> imageList = new ArrayList<>();

        double totalCalories = 0;
        double totalProtein = 0;
        double totalFat = 0;
        double totalCarbs = 0;
        Exception lastRecognitionException = null;

        // 2️⃣ 遍历图片
        for (MultipartFile file : files) {

            try {
                // 1. 调用AI
                String output = chatClientUtil.multiModalityChat(prompt, List.of(file));
                log.info("AI识别结果: {}", output);

                output = removeJsonMarks(output);

                // 2. 反序列化
                NutritionResult nutrition = deserializeNutrition(output);
                if (nutrition == null) {
                    log.error("营养反序列化失败");
                    continue;
                }

                List<RecognitionFollowUpItemDTO> followUpItems = normalizeFollowUpItems(nutrition.getFollowUpItems());
                String needMoreInfo = resolveNeedMoreInfo(nutrition.getNeedMoreInfo(), followUpItems);

                // 3. 上传OSS
                String imageUrl = uploadImageToOSS(file, sessionId.toString());

                // 4. 保存 image 表
                Image image = new Image();
                image.setSessionId(sessionId);
                image.setImageUrl(imageUrl);

                image.setTotalCalories(nutrition.getTotalCalories());
                image.setProtein(nutrition.getProtein());
                image.setFat(nutrition.getFat());
                image.setCarbs(nutrition.getCarbs());

                image.setAiRawResult(output);
                image.setAiPromptHint(buildPromptHint(needMoreInfo, followUpItems));

                imageMapper.insert(image);

                // 5. 聚合 session
                totalCalories += safe(nutrition.getTotalCalories());
                totalProtein += safe(nutrition.getProtein());
                totalFat += safe(nutrition.getFat());
                totalCarbs += safe(nutrition.getCarbs());

                // 6. 返回DTO
                ImageNutritionDTO dto = new ImageNutritionDTO();
                dto.setImageId(image.getId());
                dto.setImageUrl(imageUrl);
                dto.setTotalCalories(nutrition.getTotalCalories());
                dto.setProtein(nutrition.getProtein());
                dto.setFat(nutrition.getFat());
                dto.setCarbs(nutrition.getCarbs());
                dto.setNeedMoreInfo(needMoreInfo);
                dto.setFollowUpItems(followUpItems);

                imageList.add(dto);

            } catch (Exception e) {
                lastRecognitionException = e;
                log.error("识别失败", e);
            }
        }

        if (imageList.isEmpty()) {
            sessionMapper.deleteById(sessionId);
            throw new BaseException(resolveRecognitionFailureMessage(lastRecognitionException));
        }

        // 7️⃣ 更新 session
        session.setTotalCalories(totalCalories);
        session.setProtein(totalProtein);
        session.setFat(totalFat);
        session.setCarbs(totalCarbs);
        session.setStatus("DONE");

        sessionMapper.updateById(session);

        // 8️⃣ 更新每日饮食日志
        dietLogService.recomputeDay(effectiveUserId, LocalDate.now());

        // 9️⃣ 返回结果
        RecognitionResponseDTO response = new RecognitionResponseDTO();
        response.setSessionId(sessionId);
        response.setImages(imageList);
        response.setTotalCalories(totalCalories);
        response.setProtein(totalProtein);
        response.setFat(totalFat);
        response.setCarbs(totalCarbs);

        return response;
    }

    @Override
    public ImageNutritionDTO refine(RefineRequestDTO request) {
        // 1. 读取原 image
        Image image = imageMapper.selectById(request.getImageId());
        if (image == null) {
            throw new IllegalArgumentException("image not found: " + request.getImageId());
        }
        Long sessionId = image.getSessionId();
        if (request.getSessionId() != null && !request.getSessionId().equals(sessionId)) {
            // sessionId 不一致时以DB为准
            sessionId = image.getSessionId();
        }

        // 2. 下载图片 -> MultipartFile
        MultipartFile file = downloadAsMultipart(image.getImageUrl());

        // 3. 构建 refine prompt（附加额外信息）
        String effectiveMealType = resolveSessionMealType(sessionId);
        String prompt = foodRecognitionRagService.augmentPrompt(AISystemPromptConstant.DefaultPrompt, effectiveMealType, request.getExtraInfo());

        // 4. 调用 AI
        String output;
        try {
            output = chatClientUtil.multiModalityChat(prompt, List.of(file));
        } catch (Exception e) {
            throw new BaseException(resolveRecognitionFailureMessage(e));
        }
        output = removeJsonMarks(output);
        NutritionResult nutrition = deserializeNutrition(output);
        if (nutrition == null) {
            throw new RuntimeException("refine nutrition parse failed");
        }

        List<RecognitionFollowUpItemDTO> followUpItems = normalizeFollowUpItems(nutrition.getFollowUpItems());
        String needMoreInfo = resolveNeedMoreInfo(nutrition.getNeedMoreInfo(), followUpItems);

        // 5. 更新 image
        image.setTotalCalories(nutrition.getTotalCalories());
        image.setProtein(nutrition.getProtein());
        image.setFat(nutrition.getFat());
        image.setCarbs(nutrition.getCarbs());
        image.setExtraInfo(request.getExtraInfo());
        image.setAiPromptHint(buildPromptHint(needMoreInfo, followUpItems));
        image.setAiRefinedResult(output);
        image.setIsRefined(1);
        imageMapper.updateById(image);

        // 6. 重新聚合 session
        aggregateAndUpdateSession(sessionId);

        dietLogService.recomputeDay(BaseContext.getCurrentId(), LocalDate.now());

        // 7. 返回 DTO
        ImageNutritionDTO dto = new ImageNutritionDTO();
        dto.setImageId(image.getId());
        dto.setImageUrl(image.getImageUrl());
        dto.setTotalCalories(nutrition.getTotalCalories());
        dto.setProtein(nutrition.getProtein());
        dto.setFat(nutrition.getFat());
        dto.setCarbs(nutrition.getCarbs());
        dto.setNeedMoreInfo(needMoreInfo);
        dto.setFollowUpItems(followUpItems);
        return dto;
    }

    private double safe(Double val) {
        return val == null ? 0 : val;
    }

    private String resolveSessionMealType(Long sessionId) {
        if (sessionId == null) {
            return "LUNCH";
        }

        Session session = sessionMapper.selectById(sessionId);
        if (session == null || session.getMealType() == null || session.getMealType().isBlank()) {
            return "LUNCH";
        }

        return session.getMealType().trim().toUpperCase();
    }

    private String resolveRecognitionFailureMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null) {
            String message = exception.getMessage();
            if (message.contains("Exceeded limit on max bytes per data-uri item")) {
                return "上传图片过大，模型暂时无法处理。现在会先尝试压缩图片；如果仍失败，请把单张图片压缩到 10MB 以内后重试。";
            }
        }
        return "当前图片未能完成识别，请稍后重试。";
    }

    private List<RecognitionFollowUpItemDTO> normalizeFollowUpItems(List<RecognitionFollowUpItemDTO> rawItems) {
        List<RecognitionFollowUpItemDTO> normalized = new ArrayList<>();
        if (rawItems == null || rawItems.isEmpty()) {
            return normalized;
        }

        for (RecognitionFollowUpItemDTO rawItem : rawItems) {
            if (rawItem == null) {
                continue;
            }

            String foodName = trimToNull(rawItem.getFoodName());
            String infoType = normalizeInfoType(rawItem.getInfoType());
            if (foodName == null || infoType == null || "NONE".equals(infoType)) {
                continue;
            }

            LinkedHashSet<String> options = new LinkedHashSet<>();
            if (rawItem.getOptions() != null) {
                for (String option : rawItem.getOptions()) {
                    String normalizedOption = trimToNull(option);
                    if (normalizedOption != null) {
                        options.add(normalizedOption);
                    }
                    if (options.size() >= 4) {
                        break;
                    }
                }
            }

            if (options.isEmpty()) {
                options.add(defaultOption(infoType));
            }
            if (options.stream().noneMatch(this::isOtherOption)) {
                options.add("其他/不确定");
            }

            RecognitionFollowUpItemDTO item = new RecognitionFollowUpItemDTO();
            item.setFoodName(foodName);
            item.setInfoType(infoType);
            item.setOptions(new ArrayList<>(options));
            normalized.add(item);

            if (normalized.size() >= 3) {
                break;
            }
        }

        return normalized;
    }

    private String resolveNeedMoreInfo(String rawNeedMoreInfo, List<RecognitionFollowUpItemDTO> followUpItems) {
        if (followUpItems != null && !followUpItems.isEmpty()) {
            return followUpItems.get(0).getInfoType();
        }

        String normalized = normalizeInfoType(rawNeedMoreInfo);
        return normalized == null ? "NONE" : normalized;
    }

    private String normalizeInfoType(String rawInfoType) {
        if (rawInfoType == null) {
            return null;
        }

        String normalized = rawInfoType.trim().toUpperCase();
        if ("WEIGHT".equals(normalized) || "COUNT".equals(normalized) || "COOKING_METHOD".equals(normalized) || "NONE".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String trimToNull(String rawText) {
        if (rawText == null) {
            return null;
        }
        String trimmed = rawText.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isOtherOption(String option) {
        return option.contains("其他") || option.contains("不确定") || option.equalsIgnoreCase("other");
    }

    private String defaultOption(String infoType) {
        if ("WEIGHT".equals(infoType)) {
            return "约100g";
        }
        if ("COUNT".equals(infoType)) {
            return "1个";
        }
        if ("COOKING_METHOD".equals(infoType)) {
            return "清蒸/水煮";
        }
        return "其他/不确定";
    }

    private String buildPromptHint(String needMoreInfo, List<RecognitionFollowUpItemDTO> followUpItems) {
        if (followUpItems == null || followUpItems.isEmpty()) {
            return needMoreInfo;
        }

        return followUpItems.stream()
                .map(item -> item.getFoodName() + ":" + item.getInfoType())
                .collect(Collectors.joining("; "));
    }

    private void aggregateAndUpdateSession(Long sessionId) {
        // 简化：扫描该 session 的所有图片求和
        List<Image> imgs = imageMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Image>()
                .eq("session_id", sessionId));
        double totalCalories = 0, totalProtein = 0, totalFat = 0, totalCarbs = 0;
        for (Image it : imgs) {
            totalCalories += it.getTotalCalories() == null ? 0 : it.getTotalCalories();
            totalProtein += it.getProtein() == null ? 0 : it.getProtein();
            totalFat += it.getFat() == null ? 0 : it.getFat();
            totalCarbs += it.getCarbs() == null ? 0 : it.getCarbs();
        }
        Session session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setTotalCalories(totalCalories);
            session.setProtein(totalProtein);
            session.setFat(totalFat);
            session.setCarbs(totalCarbs);
            sessionMapper.updateById(session);

            // 同步更新 diet_log（按用户+日期聚合所有 session）
            java.time.LocalDate day = session.getCreateTime() != null ? session.getCreateTime().toLocalDate() : java.time.LocalDate.now();
            recomputeDietLog(session.getUserId(), day);
        }
    }

    private MultipartFile downloadAsMultipart(String url) {
        try {
            URL u = new URL(url);
            URLConnection conn = u.openConnection();
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) {
                    bos.write(buf, 0, r);
                }
                byte[] bytes = bos.toByteArray();
                String fileName = url.substring(url.lastIndexOf('/') + 1);
                String contentType = conn.getContentType();
                return new SimpleBytesMultipartFile(fileName, contentType, bytes);
            }
        } catch (Exception e) {
            throw new RuntimeException("download image failed: " + url, e);
        }
    }

    private void recomputeDietLog(Long userId, java.time.LocalDate date) {
        // 复用 Service 逻辑的简版，避免循环依赖：按用户+日期扫描所有 sessions 求和后 upsert diet_log
        java.time.LocalDateTime start = date.atStartOfDay();
        java.time.LocalDateTime end = date.atTime(java.time.LocalTime.MAX);
        List<Session> sessions = sessionMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Session>()
                .eq("user_id", userId).between("create_time", start, end));
        double totalCalories = 0, protein = 0, fat = 0, carbs = 0;
        for (Session s : sessions) {
            totalCalories += s.getTotalCalories() == null ? 0 : s.getTotalCalories();
            protein += s.getProtein() == null ? 0 : s.getProtein();
            fat += s.getFat() == null ? 0 : s.getFat();
            carbs += s.getCarbs() == null ? 0 : s.getCarbs();
        }
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.cy.entity.DietLog> dq =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        dq.eq("user_id", userId).eq("date", date);
        com.cy.entity.DietLog existing = dietLogMapper.selectOne(dq);
        if (existing == null) {
            com.cy.entity.DietLog dl = com.cy.entity.DietLog.builder()
                    .userId(userId)
                    .date(date)
                    .totalCalories(totalCalories)
                    .protein(protein)
                    .fat(fat)
                    .carbs(carbs)
                    .build();
            dietLogMapper.insert(dl);
        } else {
            existing.setTotalCalories(totalCalories);
            existing.setProtein(protein);
            existing.setFat(fat);
            existing.setCarbs(carbs);
            dietLogMapper.updateById(existing);
        }
    }

    private static class SimpleBytesMultipartFile implements MultipartFile {
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;
        SimpleBytesMultipartFile(String originalFilename, String contentType, byte[] bytes) {
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.bytes = bytes;
        }
        @Override public String getName() { return originalFilename; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes == null || bytes.length == 0; }
        @Override public long getSize() { return bytes == null ? 0 : bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public InputStream getInputStream() { return new java.io.ByteArrayInputStream(bytes); }
        @Override public void transferTo(java.io.File dest) throws java.io.IOException {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(bytes);
            }
        }
    }

    /**
     * 上传图片到OSS并返回URL
     */
    private String uploadImageToOSS(MultipartFile file, String recordNumber) throws Exception {
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String objectName = "recognition-images/" + recordNumber + fileExtension;

        // 上传并返回完整URL
        return aliOssUtil.upload(file.getBytes(), objectName);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 反序列化JSON为Nutrition对象
     */
    private NutritionResult deserializeNutrition(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, NutritionResult.class);
        } catch (JsonProcessingException e) {
            log.error("JSON反序列化失败: {}", json, e);
            return null;
        }
    }


    /**
     * 清理JSON标记
     */
    private String removeJsonMarks(String jsonString) {
        if (jsonString == null) return null;
        return jsonString.replaceAll("^\\s*```json\\s*|\\s*```\\s*$", "");
    }


}



