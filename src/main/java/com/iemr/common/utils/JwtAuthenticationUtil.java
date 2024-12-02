package com.iemr.common.utils;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.iemr.common.data.users.User;
import com.iemr.common.service.users.IEMRAdminUserServiceImpl;
import com.iemr.common.utils.exception.IEMRException;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtAuthenticationUtil {

    @Autowired
    private final CookieUtil cookieUtil;
    @Autowired
    private final JwtUtil jwtUtil;
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Autowired
    private IEMRAdminUserServiceImpl iEMRAdminUserServiceImpl;

    public JwtAuthenticationUtil(CookieUtil cookieUtil, JwtUtil jwtUtil) {
        this.cookieUtil = cookieUtil;
        this.jwtUtil = jwtUtil;
    }

    public ResponseEntity<String> validateJwtToken(HttpServletRequest request) {
        Optional<String> jwtTokenOpt = cookieUtil.getCookieValue(request, "Jwttoken");

        if (jwtTokenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Error 401: Unauthorized - JWT Token is not set!");
        }

        String jwtToken = jwtTokenOpt.get();

        // Validate the token
        Claims claims = jwtUtil.validateToken(jwtToken);
        if (claims == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error 401: Unauthorized - Invalid JWT Token!");
        }

        // Extract username from token
        String usernameFromToken = claims.getSubject();
        if (usernameFromToken == null || usernameFromToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error 401: Unauthorized - Username is missing!");
        }

        // Return the username if valid
        return ResponseEntity.ok(usernameFromToken);
    }

    public boolean validateUserIdAndJwtToken(String jwtToken) throws IEMRException {
        try {
            // Validate JWT token and extract claims
            Claims claims = jwtUtil.validateToken(jwtToken);

            if (claims == null) {
                throw new IEMRException("Invalid JWT token.");
            }

            String userId = claims.get("userId", String.class);
            String tokenUsername = jwtUtil.extractUsername(jwtToken);

            // Fetch user based on userId from the database or cache
            User user = iEMRAdminUserServiceImpl.getUserById(Long.parseLong(userId));
            if (user == null) {
                throw new IEMRException("Invalid User ID.");
            }

            // Check if the token's username matches the user retrieved by userId
            if (!user.getUserName().equalsIgnoreCase(tokenUsername)) {
                throw new IEMRException("JWT token and User ID mismatch.");
            }

            return true; // Valid userId and JWT token
        } catch (Exception e) {
            logger.error("Validation failed: " + e.getMessage(), e);
            throw new IEMRException("Validation error: " + e.getMessage(), e);
        }
    }
}