package com.notifyhub.auth;

import com.notifyhub.auth.dto.AuthResponse;
import com.notifyhub.auth.dto.LoginRequest;
import com.notifyhub.auth.dto.RegisterRequest;
import com.notifyhub.exception.EmailAlreadyExistsException;
import com.notifyhub.user.AppUser;
import com.notifyhub.user.AppUserRepository;
import com.notifyhub.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public AuthResponse register(RegisterRequest request) {

        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered.");
        }

        AppUser appUser = AppUser.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CLIENT)
                .build();

        appUserRepository.save(appUser);

        String token = jwtService.generateToken(new AppUserDetails(appUser));

        return new AuthResponse(
                token,
                "Bearer",
                expirationMs,
                appUser.getEmail(),
                appUser.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        AppUser appUser = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(new AppUserDetails(appUser));

        return new AuthResponse(
                token,
                "Bearer",
                expirationMs,
                appUser.getEmail(),
                appUser.getRole().name()
        );
    }
}