package com.cy.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 大模型接口调用工具类
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class ChatClientUtil {

    private static final long MAX_MEDIA_BYTES = 9L * 1024 * 1024;
    private static final int[] MAX_IMAGE_DIMENSIONS = {2048, 1600, 1280, 1024, 768};
    private static final float[] JPEG_QUALITIES = {0.9f, 0.82f, 0.74f, 0.66f, 0.58f, 0.5f};

    // 注入ChatClient
    private final ChatClient chatClient;

    /**
     * 多模态调用
     *
     * @param prompt
     * @return
     */
    public String multiModalityChat(String prompt, List<MultipartFile> files) {
        if (files.isEmpty()) {
            // 文本聊天
            return textChat(prompt);
        } else {
            // 多模态聊天
            return multiModalChat(prompt, files);
        }
    }

    private String textChat(String prompt) {
        return chatClient.prompt()
                .user(p -> p.text(prompt))
                .call()
                .content();
    }

    private String multiModalChat(String prompt, List<MultipartFile> files) {
        List<Media> medias = files.stream()
                .map(this::buildMedia)
                .toList();

        return chatClient.prompt()
                .user(p -> p.text(prompt).media(medias.toArray(Media[]::new)))
                .call()
                .content();
    }

    private Media buildMedia(MultipartFile file) {
        try {
            return buildPreparedMedia(file);
        } catch (Exception e) {
            log.warn("准备多模态图片失败，回退到原图: {}", file.getOriginalFilename(), e);
            return buildOriginalMedia(file);
        }
    }

    private Media buildPreparedMedia(MultipartFile file) throws IOException {
        if (!isImage(file) || file.getSize() <= MAX_MEDIA_BYTES) {
            return buildOriginalMedia(file);
        }

        BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        if (sourceImage == null) {
            log.warn("图片无法解析，跳过压缩处理: {}", file.getOriginalFilename());
            return buildOriginalMedia(file);
        }

        byte[] bestCandidate = null;
        for (int maxDimension : MAX_IMAGE_DIMENSIONS) {
            BufferedImage scaledImage = scaleToFit(sourceImage, maxDimension);
            for (float quality : JPEG_QUALITIES) {
                byte[] candidate = encodeJpeg(scaledImage, quality);
                if (candidate == null || candidate.length == 0) {
                    continue;
                }
                if (bestCandidate == null || candidate.length < bestCandidate.length) {
                    bestCandidate = candidate;
                }
                if (candidate.length <= MAX_MEDIA_BYTES) {
                    log.info("图片已压缩后再发送给大模型: {} -> {} bytes", file.getOriginalFilename(), candidate.length);
                    return new Media(MimeType.valueOf("image/jpeg"), namedResource(candidate, buildJpegFileName(file.getOriginalFilename())));
                }
            }
        }

        if (bestCandidate != null) {
            log.warn("图片压缩后仍偏大，将使用最小可得版本继续尝试: {} -> {} bytes", file.getOriginalFilename(), bestCandidate.length);
            return new Media(MimeType.valueOf("image/jpeg"), namedResource(bestCandidate, buildJpegFileName(file.getOriginalFilename())));
        }

        return buildOriginalMedia(file);
    }

    private Media buildOriginalMedia(MultipartFile file) {
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";
        return new Media(MimeType.valueOf(contentType), file.getResource());
    }

    private boolean isImage(MultipartFile file) {
        return file.getContentType() != null && file.getContentType().toLowerCase(Locale.ROOT).startsWith("image/");
    }

    private BufferedImage scaleToFit(BufferedImage source, int maxDimension) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int largestSide = Math.max(sourceWidth, sourceHeight);
        double scale = largestSide > maxDimension ? (double) maxDimension / largestSide : 1.0d;

        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));

        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").hasNext()
                ? ImageIO.getImageWritersByFormatName("jpg").next()
                : null;
        if (writer == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream imageOutputStream = new MemoryCacheImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
            return outputStream.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private Resource namedResource(byte[] bytes, String filename) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private String buildJpegFileName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "compressed-upload.jpg";
        }
        return originalFilename.substring(0, originalFilename.lastIndexOf('.')) + ".jpg";
    }

}
