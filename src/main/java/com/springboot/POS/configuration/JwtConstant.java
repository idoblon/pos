package com.springboot.POS.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtConstant {

    public static String JWT_SECRET;
    public static final String JWT_HEADER = "Authorization";

    @Value("${jwt.secret}")
    public void setJwtSecret(String secret) {
        JWT_SECRET = secret;
    }
}
