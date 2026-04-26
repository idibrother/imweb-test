package com.imweb.shop.global.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private Jwt jwt = new Jwt();
    private Security security = new Security();
    private Cors cors = new Cors();

    @Data
    public static class Jwt {
        private String issuer = "http://localhost:8080";
        private String audience = "imweb-api";
        private int accessTokenTtlMinutes = 15;
        private int refreshTokenTtlDays = 30;
        private String privateKeyBase64 = "";
    }

    @Data
    public static class Security {
        private int maxLoginAttempts = 5;
        private int lockoutDurationMinutes = 15;
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
    }
}
