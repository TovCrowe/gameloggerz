package com.tov.gamelogger.auth;

import com.tov.gamelogger.auth.dto.TokenResponse;
import com.tov.gamelogger.domain.User;
import com.tov.gamelogger.repository.UserRepository;
import com.tov.gamelogger.security.InvalidTokenException;
import com.tov.gamelogger.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException(email);
        }
        userRepository.save(new User(email, passwordEncoder.encode(rawPassword)));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueTokenPair(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        Claims claims = jwtService.parseRefreshToken(refreshToken);
        User user = userRepository.findById(JwtService.extractUserId(claims))
                .orElseThrow(InvalidTokenException::new);
        return issueTokenPair(user);
    }

    private TokenResponse issueTokenPair(User user) {
        return new TokenResponse(
                jwtService.generateAccessToken(user.getId(), user.getEmail()),
                jwtService.generateRefreshToken(user.getId(), user.getEmail()));
    }
}
