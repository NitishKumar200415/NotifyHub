package com.notifyhub.auth;

import com.notifyhub.auth.dto.AuthResponse;
import com.notifyhub.auth.dto.LoginRequest;
import com.notifyhub.auth.dto.RegisterRequest;
import com.notifyhub.exception.EmailAlreadyExistsException;
import com.notifyhub.user.AppUser;
import com.notifyhub.user.AppUserRepository;
import com.notifyhub.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_ShouldCreateUserSuccessfully() {

        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(appUserRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any(AppUserDetails.class))).thenReturn("jwt-token");

        ReflectionTestUtils.setField(authService, "expirationMs", 86400000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("test@example.com", response.getEmail());

        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(appUserRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void login_ShouldReturnJwtToken() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AppUser user = AppUser.builder()
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .role(UserRole.CLIENT)
                .build();

        when(appUserRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(any(AppUserDetails.class)))
                .thenReturn("jwt-token");

        ReflectionTestUtils.setField(authService, "expirationMs", 86400000L);

        when(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("CLIENT", response.getRole());
    }

    @Test
    void login_ShouldThrowException_WhenCredentialsAreInvalid() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );
    }
}