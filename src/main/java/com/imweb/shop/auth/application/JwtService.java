package com.imweb.shop.auth.application;

import com.imweb.shop.global.config.AppProperties;
import com.imweb.shop.global.config.JwtKeyConfig;
import com.imweb.shop.auth.domain.Role;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtKeyConfig keyConfig;
    private final AppProperties appProperties;

    public String generateAccessToken(String username, Collection<Role> roles) throws JOSEException {
        String jti = UUID.randomUUID().toString();
        AppProperties.Jwt jwtProps = appProperties.getJwt();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(keyConfig.getRsaKey().getKeyID())
                .build();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer(jwtProps.getIssuer())
                .audience(jwtProps.getAudience())
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(jwtProps.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES)))
                .jwtID(jti)
                .claim("scope", roles.stream()
                        .map(r -> "ROLE_" + r.name())
                        .collect(Collectors.joining(" ")))
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);
        signedJWT.sign(new RSASSASigner(keyConfig.getRsaKey().toRSAPrivateKey()));
        return signedJWT.serialize();
    }

    public JWTClaimsSet validateAndGetClaims(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);

        if (!signedJWT.verify(new RSASSAVerifier(keyConfig.getRsaKey().toRSAPublicKey()))) {
            throw new JOSEException("Invalid JWT signature");
        }

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
            throw new JOSEException("JWT has expired");
        }

        return claims;
    }

    public JWKSet getPublicJwkSet() {
        return new JWKSet(keyConfig.getRsaKey().toPublicJWK());
    }
}
