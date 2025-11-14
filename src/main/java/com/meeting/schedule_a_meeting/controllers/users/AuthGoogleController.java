package com.meeting.schedule_a_meeting.controllers.users;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import com.meeting.schedule_a_meeting.config.JwtTokenUtil;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.service.users.AuthenticationGoogleService;
import com.meeting.schedule_a_meeting.service.users.UserService;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/auth")
public class AuthGoogleController {

    final UserService userService;
    final AuthenticationGoogleService authenticationGoogleService;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    @Qualifier("googleJwtDecoder")
    private JwtDecoder googleJwtDecoder;
    private static final Key SECRET_KEY = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512);

    @GetMapping("/login/google")
    public void loginGoogleAuth(HttpServletResponse response) throws IOException {
        log.info("Initiating Google OAuth2 login");
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/loginSuccess")
    public void handleGoogleSuccess(OAuth2AuthenticationToken auth2AuthenticationToken,
                                    HttpServletResponse response,
                                    HttpSession session) throws IOException {

        log.info("=== LOGIN SUCCESS ENDPOINT HIT ===");

        if (auth2AuthenticationToken == null) {
            log.error("OAuth2AuthenticationToken is null");
            response.sendRedirect("http://localhost:5173/?error=auth_failed");
            return;
        }

        try {
            Users user = authenticationGoogleService.loginRegisterByGoogleOAuth2(auth2AuthenticationToken);

            session.setAttribute("userId", user.getId().toString());
            session.setAttribute("email", user.getEmail());
            session.setAttribute("name", user.getName());
            session.setAttribute("avatarUrl", user.getAvatar_url());
            session.setAttribute("accessToken", user.getAccessToken());
            session.setAttribute("refreshToken", user.getRefreshToken() != null ? user.getRefreshToken() : "");
            session.setAttribute("expiresIn", user.getExpiresIn());
            session.setAttribute("created_at", user.getCreatedAt().toString());
            session.setAttribute("backgroundUrl", user.getBackground_url());

            response.sendRedirect("http://localhost:5173/loginSuccess");

        } catch (AppException e) {
            log.error("Google login failed: {}", e.getMessage());
            String errorMsg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect("http://localhost:5173/?error=google_login_failed&message=" + errorMsg);
        } catch (Exception e) {
            log.error("Unexpected error in Google login", e);
            response.sendRedirect("http://localhost:5173/?error=processing_failed");
        }
    }

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(HttpSession session) {
        log.info("=== USER INFO ENDPOINT HIT ===");
        log.info("Session ID: {}", session.getId());

        String userId = (String) session.getAttribute("userId");
        log.info("UserId from session: {}", userId);

        if (userId == null) {
            log.warn("No userId found in session");
            java.util.Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String name = attributeNames.nextElement();
                log.info("Session attribute: {} = {}", name, session.getAttribute(name));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated", "message", "No session found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("email", session.getAttribute("email"));
        response.put("name", session.getAttribute("name"));
        response.put("avatarUrl", session.getAttribute("avatarUrl"));
        response.put("accessToken", session.getAttribute("accessToken"));
        response.put("refreshToken", session.getAttribute("refreshToken"));
        response.put("expiresIn", session.getAttribute("expiresIn"));
        response.put("created_at", session.getAttribute("created_at"));
        response.put("backgroundUrl", session.getAttribute("backgroundUrl"));
        log.info("Returning user info: {}", response);

        // Do not clear session here; let it persist for the frontend
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> requestBody) throws IOException {
        String refreshToken = requestBody.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Missing refresh token in request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing refresh token");
        }

        log.info("Attempting to refresh token");
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://oauth2.googleapis.com/token")
                    .openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String body = "client_id=23955059535-ms7k1vo9hcgjdfhkoup7u3i01pqecq4u.apps.googleusercontent.com"
                    + "&client_secret=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"
                    + "&refresh_token=" + refreshToken
                    + "&grant_type=refresh_token";
            conn.setDoOutput(true);
            conn.getOutputStream().write(body.getBytes());

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                Map<String, Object> responseData = new ObjectMapper().readValue(reader.readLine(), Map.class);
                log.info("Token refreshed successfully");
                return ResponseEntity.ok(responseData);
            } else {
                log.error("Failed to refresh token, status: {}", responseCode);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Failed to refresh token");
            }
        } catch (Exception e) {
            log.error("Error refreshing token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error refreshing token");
        }
    }

    @GetMapping("/debug/session")
    public ResponseEntity<?> debugSession(HttpSession session) {
        log.info("=== DEBUG SESSION ===");
        Map<String, Object> sessionData = new HashMap<>();

        java.util.Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = session.getAttribute(attributeName);
            sessionData.put(attributeName, attributeValue);
            log.info("Session attribute: {} = {}", attributeName, attributeValue);
        }

        sessionData.put("sessionId", session.getId());
        sessionData.put("isNew", session.isNew());
        sessionData.put("creationTime", new java.util.Date(session.getCreationTime()));
        sessionData.put("lastAccessedTime", new java.util.Date(session.getLastAccessedTime()));

        return ResponseEntity.ok(sessionData);
    }

    @PostMapping("/exchange-token")
    public ResponseEntity<?> exchangeGoogleTokenForJwt(@RequestBody Map<String, String> request) {
        System.out.println("Received exchange-token request");

        try {
            String googleToken = request.get("googleToken");
            System.out.println("Google token received: " + (googleToken != null ? "Yes" : "No"));

            if (googleToken == null || googleToken.trim().isEmpty()) {
                return ResponseEntity.status(400).body("Google token is required");
            }

            // Use access token to fetch user info from Google
            System.out.println("Fetching user info from Google...");
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + googleToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> googleResponse = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    entity,
                    Map.class);

            if (!googleResponse.getStatusCode().is2xxSuccessful()) {
                throw new Exception("Failed to fetch user info from Google: " + googleResponse.getStatusCode());
            }

            Map<String, Object> userInfo = googleResponse.getBody();
            String email = (String) userInfo.get("email");
            System.out.println("Fetched email from Google: " + email);

            // Check if user exists in system
            System.out.println("Loading user details for: " + email);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            System.out.println("User found: " + userDetails.getUsername());

            // Generate local JWT token
            System.out.println("Generating local JWT token...");
            String localJwtToken = jwtTokenUtil.generateToken(email);
            System.out.println("Local JWT token generated successfully");

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", localJwtToken);
            response.put("userEmail", email);
            response.put("authProvider", "GOOGLE");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Token exchange error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401).body("Invalid Google token: " + e.getMessage());
        }
    }
}
