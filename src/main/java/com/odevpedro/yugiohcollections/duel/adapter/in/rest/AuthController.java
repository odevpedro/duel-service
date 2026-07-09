package com.odevpedro.yugiohcollections.duel.adapter.in.rest;

import com.odevpedro.yugiohcollections.duel.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");

        if (username.isBlank() || password.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "username and password are required"));

        if (!password.equals("test123"))
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));

        String token = tokenProvider.createToken(username, username, "PLAYER");
        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", username,
                "username", username,
                "role", "PLAYER"
        ));
    }
}
