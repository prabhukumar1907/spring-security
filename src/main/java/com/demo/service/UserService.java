package com.demo.service;

import com.demo.Role;
import com.demo.config.JwtUtil;
import com.demo.entity.User;
import com.demo.exception.InvalidCredentialsException;
import com.demo.model.AuthRequest;
import com.demo.model.AuthResponse;
import com.demo.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private String CLIENT_ID="407408718192.apps.googleusercontent.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;


    @Autowired
    private JwtUtil jwtUtil;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<List<User>> getUsers(){
        return Optional.of(userRepository.findAll());
    }


    public User creatUser(User user) {
        String hashedPassword= passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);
        return userRepository.save(user);
    }

    public AuthResponse login(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        final String jwt = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .access_token(jwt)
                .success(true)
                .message("Successfully logged in")
                .build();
    }

    public AuthResponse loginWithGoogle(String idTokenString) {
        try {
            System.out.println("Verifying token: " + idTokenString);

            // Parse token for inspection
            GoogleIdToken parsedToken = GoogleIdToken.parse(GsonFactory.getDefaultInstance(), idTokenString);
            System.out.println("Audience: " + parsedToken.getPayload().getAudience());
            System.out.println("Issuer: " + parsedToken.getPayload().getIssuer());
            System.out.println("Expiration time (epoch seconds): " + parsedToken.getPayload().getExpirationTimeSeconds());

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance()
            ).setAudience(Collections.singletonList(CLIENT_ID)).build();
            System.out.println(verifier.verify(idTokenString));
            GoogleIdToken idToken = verifier.verify(idTokenString);
            System.out.println("idToken: " + idToken);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                System.out.println("Token verified! Email: " + payload.getEmail());

                String email = payload.getEmail();
                String name = (String) payload.get("name");

                User user = userRepository.findByEmail(email)
                        .orElseGet(() -> {
                            User newUser = new User();
                            newUser.setEmail(email);
                            newUser.setName(name);
                            newUser.setCreatedAt(LocalDateTime.now());
                            newUser.setUpdatedAt(LocalDateTime.now());
                            newUser.setRoles(Collections.singletonList(String.valueOf(Role.USER)));
                            return userRepository.save(newUser);
                        });

                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

                String jwt = jwtUtil.generateToken(userDetails);

                return AuthResponse.builder()
                        .access_token(jwt)
                        .success(true)
                        .message("Successfully logged in")
                        .build();
            } else {
                System.out.println("Invalid ID token!");
                throw new RuntimeException("Invalid Google ID token");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Google login failed: " + e.getMessage(), e);
        }
    }

}
