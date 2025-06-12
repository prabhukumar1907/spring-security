package com.demo.controller;

import com.demo.config.JwtUtil;
import com.demo.entity.User;
import com.demo.exception.InvalidCredentialsException;
import com.demo.model.AuthRequest;
import com.demo.model.AuthResponse;
import com.demo.model.GoogleLoginRequest;
import com.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {



    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthRequest request) {
        return new ResponseEntity<>(userService.login(request), HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        return new ResponseEntity<>(userService.creatUser(user), HttpStatus.OK);
    }
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@RequestBody Map<String, String> request) {
        String idTokenString = request.get("idToken");
        if (idTokenString == null) {
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .success(false)
                            .message("idToken is missing")
                            .build()
            );
        }
        AuthResponse response = userService.loginWithGoogle(idTokenString);
        return ResponseEntity.ok(response);
    }
}
