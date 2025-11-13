package com.meeting.schedule_a_meeting.controllers.users;

import com.meeting.schedule_a_meeting.config.JwtTokenUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/auth")
public class AuthGoogleController {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    @Qualifier("googleJwtDecoder")
    private JwtDecoder googleJwtDecoder;

    @GetMapping("/login/google")
    public void loginGoogleAuth(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/loginSuccess")
    public ResponseEntity<?> loginSuccess(OAuth2AuthenticationToken authToken) {
        Map<String, Object> userInfo = authToken.getPrincipal().getAttributes();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "email", userInfo.get("email"),
                "name", userInfo.get("name"),
                "picture", userInfo.get("picture")
        ));
    }

    @PostMapping("/exchange-token")
    public ResponseEntity<?> exchangeGoogleTokenForJwt(@RequestBody Map<String, String> request) {
        try {
            String googleToken = request.get("googleToken");
            if (googleToken == null || googleToken.trim().isEmpty()) {
                return ResponseEntity.status(400).body("Google token is required");
            }

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + googleToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> googleResponse = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (!googleResponse.getStatusCode().is2xxSuccessful()) {
                throw new Exception("Failed to fetch user info from Google: " + googleResponse.getStatusCode());
            }

            Map<String, Object> userInfo = googleResponse.getBody();
            String email = (String) userInfo.get("email");

            String localJwtToken = jwtTokenUtil.generateToken(email);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", localJwtToken);
            response.put("userEmail", email);
            response.put("authProvider", "GOOGLE");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid Google token: " + e.getMessage());
        }
    }
}