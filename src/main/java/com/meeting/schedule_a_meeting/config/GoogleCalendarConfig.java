package com.meeting.schedule_a_meeting.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.CalendarScopes;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@Getter
public class GoogleCalendarConfig {

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(
            CalendarScopes.CALENDAR,
            "openid",
            "email"
    );

    @Bean
    public NetHttpTransport netHttpTransport() {
        return new NetHttpTransport();
    }

    @Bean
    public JsonFactory jsonFactory() {
        return JSON_FACTORY;
    }

    @Bean
    public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow(
            NetHttpTransport httpTransport, JsonFactory jsonFactory) throws IOException {

        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(clientId);
        details.setClientSecret(clientSecret);

        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setInstalled(details);

        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, clientSecrets, SCOPES)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
    }
}
//git config --global user.name "yrcoksuk"
//git config --global user.email "quangdoleminh@gmail.com"