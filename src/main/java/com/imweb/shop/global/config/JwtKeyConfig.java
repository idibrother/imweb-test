package com.imweb.shop.global.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtKeyConfig {

    private final AppProperties appProperties;

    @Getter
    private RSAKey rsaKey;

    @PostConstruct
    public void init() throws Exception {
        String privateKeyBase64 = appProperties.getJwt().getPrivateKeyBase64();
        if (StringUtils.hasText(privateKeyBase64)) {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            RSAPublicKey publicKey = derivePublicKey(privateKey);
            rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .algorithm(JWSAlgorithm.RS256)
                    .keyUse(KeyUse.SIGNATURE)
                    .build();
            log.info("Loaded RSA key from environment variable");
        } else {
            log.warn("JWT_PRIVATE_KEY_BASE64 not set - generating ephemeral RSA key pair (NOT suitable for production)");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .algorithm(JWSAlgorithm.RS256)
                    .keyUse(KeyUse.SIGNATURE)
                    .build();
        }
    }

    private RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) throws Exception {
        RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey) privateKey;
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
    }
}
