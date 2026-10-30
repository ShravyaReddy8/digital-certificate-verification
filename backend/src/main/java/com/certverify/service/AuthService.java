package com.certverify.service;

import com.certverify.dto.AuthResponse;
import com.certverify.dto.LoginRequest;
import com.certverify.dto.RegisterRequest;
import com.certverify.dto.RegisterResponse;
import com.certverify.dto.UserDto;
import com.certverify.entity.Role;
import com.certverify.entity.User;
import com.certverify.entity.UserStatus;
import com.certverify.exception.BadRequestException;
import com.certverify.exception.ResourceNotFoundException;
import com.certverify.repository.UserRepository;
import com.certverify.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse registerIssuer(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("An account with this email already exists.");
        }

        User issuer = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ISSUER)
                .status(UserStatus.PENDING)
                .institutionName(request.institutionName().trim())
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(issuer);
        return new RegisterResponse(
                "Registration successful. Your institution account is awaiting administrator approval.",
                UserDto.from(saved));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        if (user.getRole() == Role.ISSUER && user.getStatus() != UserStatus.APPROVED) {
            if (user.getStatus() == UserStatus.REJECTED) {
                throw new DisabledException("Your issuer registration was rejected. Please contact the administrator.");
            }
            throw new DisabledException("Your issuer account is pending administrator approval.");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.of(token, UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public UserDto currentUser(User principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        return UserDto.from(user);
    }
}
