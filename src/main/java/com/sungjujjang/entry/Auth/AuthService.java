package com.sungjujjang.entry.Auth;

import com.sungjujjang.entry.Auth.dto.LoginRequest;
import com.sungjujjang.entry.Auth.dto.LoginResponse;
import com.sungjujjang.entry.Auth.dto.RegisterRequest;
import com.sungjujjang.entry.Auth.dto.RegisterResponse;
import com.sungjujjang.entry.Global.Errors.exception.PHONE_DUPLICATION_ERR;
import com.sungjujjang.entry.Global.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public RegisterResponse register(
            RegisterRequest registerRequest
    ) {
        if (userRepository.findByPhone(registerRequest.phone()).isPresent()) {
            throw PHONE_DUPLICATION_ERR.EXCEPTION;
        }

        User user = User.builder()
                .phone(registerRequest.phone())
                .name(registerRequest.name())
                .password(passwordEncoder.encode(registerRequest.password()))
                .build();

        userRepository.save(user);

        return new RegisterResponse(Boolean.TRUE);
    }

    public LoginResponse login(
            LoginRequest loginRequest
    ) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.phone(), loginRequest.password())
        );

        String token = jwtTokenProvider.createToken(loginRequest.phone());
        return new LoginResponse(token);

    }
}
