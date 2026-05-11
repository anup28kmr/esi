package ee.ut.anup.userservice.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService {

    private final JwtService jwtService;

    public String generateToken(String username) {
        log.info("Generating JWT for email={}", username);
        String token = jwtService.generateToken(username);
        log.info("JWT generated successfully for email={}", username);
        return token;
    }

    public void validateToken(String token) {
        log.info("Token validation requested");
        jwtService.validateToken(token);
        log.info("Token validation completed successfully");
    }

}