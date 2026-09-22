package com.springboot.POS.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
public class JwtProvider {

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(JwtConstant.JWT_SECRET.getBytes());
    }

    public String generateToken(Authentication authentication){
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        String roles = populateAuthorities(authorities);
        return Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime()+86400000))
                .claim("email", authentication.getName())
                .claim("authorities", roles)
                .signWith(getKey())
                .compact();
    }

    public String getEmailFromToken(String jwt){
        jwt = jwt.substring(7);
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        return String.valueOf(claims.get("email"));
    }

    /**
     * Parses a token for the /auth/refresh flow: signature must be valid, but the
     * token may already be expired (within a 7 day grace window) so the client can
     * silently obtain a fresh token.
     */
    public String getEmailFromRefreshToken(String jwt){
        if (jwt != null && jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);
        }
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .clockSkewSeconds(7L * 24 * 3600)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        return String.valueOf(claims.get("email"));
    }

    private String populateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<String> auths = new HashSet<>();
        for(GrantedAuthority authority : authorities){
            auths.add(authority.getAuthority());
        }
        return String.join(",", auths);
    }

}
