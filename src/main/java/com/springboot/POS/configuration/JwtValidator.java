package com.springboot.POS.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

public class JwtValidator extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

                String jwt = request.getHeader(JwtConstant.JWT_HEADER);
                String requestURI = request.getRequestURI();
                
                System.out.println("🔍 JWT DEBUG - Request URI: " + requestURI);
                System.out.println("🔍 JWT DEBUG - JWT Header present: " + (jwt != null));
                
                //Bearer jwt
                if(jwt != null){
                    System.out.println("🔍 JWT DEBUG - JWT Header value: " + jwt.substring(0, Math.min(jwt.length(), 20)) + "...");
                    jwt = jwt.substring(7);
                    try{
                        SecretKey key = Keys.hmacShaKeyFor(JwtConstant.JWT_SECRET.getBytes());
                        Claims claims = Jwts.parser()
                                .verifyWith(key)
                                .build()
                                .parseSignedClaims(jwt)
                                .getPayload();

                        String email = String.valueOf(claims.get("email"));
                        String authorities = String.valueOf(claims.get("authorities"));
                        
                        System.out.println("✅ JWT DEBUG - JWT validation successful for: " + email);
                        System.out.println("🔍 JWT DEBUG - Authorities: " + authorities);

                        List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(
                                authorities
                        );
                        Authentication auth = new UsernamePasswordAuthenticationToken(email,null, auths);
                        SecurityContextHolder.getContext().setAuthentication(
                                auth
                        );

                    }
                    catch(Exception e){
                        System.err.println("❌ JWT DEBUG - JWT validation failed: " + e.getMessage());
                        e.printStackTrace();
                        throw new BadCredentialsException("Invalid JWT....");
                    }
                } else {
                    System.out.println("⚠️ JWT DEBUG - No JWT header found for: " + requestURI);
                }

                filterChain.doFilter(request,response);
    }
}
