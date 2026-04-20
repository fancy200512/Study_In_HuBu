package com.cy.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "cy.jwt")
@Data
public class JwtProperties {

    /**
     * jwt令牌相关配置
     */
    private String secretKey;
    private long ttl;
    private String tokenName;
    private List<String> ignorePaths;


}
