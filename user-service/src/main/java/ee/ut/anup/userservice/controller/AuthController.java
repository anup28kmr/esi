package ee.ut.anup.userservice.controller;

import ee.ut.anup.userservice.dto.AuthRequest;
import ee.ut.anup.userservice.dto.ErrorResponseDTO;
import ee.ut.anup.userservice.dto.LoginResponseDTO;
import ee.ut.anup.userservice.dto.UserDTO;
import ee.ut.anup.userservice.entity.User;
import ee.ut.anup.userservice.security.AuthenticatedUser;
import ee.ut.anup.userservice.service.AuthService;
import ee.ut.anup.userservice.service.UserService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user authentication")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @Operation(summary = "Sign in", description = "Sign in and return the authenticated user JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody AuthRequest authRequest) {
        log.info("Login attempt for email={}", authRequest.email());

        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.email(), authRequest.password())
        );

        log.info("Authentication result for email={}, authenticated={}",
                authRequest.email(), authenticate.isAuthenticated());

        if (authenticate.isAuthenticated()) {
            UserDTO user = userService.getUserByEmail(authRequest.email());
            String token = authService.generateToken(user.userId(), user.email(), user.role());
            log.info("Token generated for userId={} email={}", user.userId(), user.email());
            return new LoginResponseDTO(token, scrub(user));
        } else {
            log.info("Login denied for email={}", authRequest.email());
            throw new RuntimeException("invalid access");
        }
    }

    /**
     * Token introspection endpoint. Verifies signature + issuer + expiration,
     * then checks the user still exists and is ACTIVE. Returns the current
     * UserDTO (password stripped) on success, 401 otherwise.
     * <p>
     * Accepts the token either as a `Bearer ...` Authorization header or as
     * a `?token=...` query param (the latter is kept for backwards
     * compatibility with the original validateToken signature). The new
     * shape is the header form -- preferred for service-to-service calls
     * that just forward their incoming Authorization header.
     */
    @Operation(summary = "Validate token (introspection)",
            description = "Verifies the JWT and returns the current user. Returns 401 if the token is invalid, expired, or the user is suspended.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token valid",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token invalid, expired, or user not active",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/validateToken")
    public UserDTO validateToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(value = "token", required = false) String tokenParam
    ) {
        String token = extractToken(authHeader, tokenParam);
        if (token == null || token.isBlank()) {
            throw new io.jsonwebtoken.JwtException("missing token");
        }
        Claims claims = authService.parseAndValidateToken(token);
        UUID userId = UUID.fromString(claims.get("userId", String.class));
        UserDTO user = userService.getUserProfile(userId);
        if (user.status() == User.Status.SUSPENDED) {
            log.warn("introspection rejected: user {} is SUSPENDED", userId);
            throw new io.jsonwebtoken.JwtException("user suspended");
        }
        log.debug("introspection ok userId={} role={}", userId, user.role());
        return scrub(user);
    }

    /**
     * Returns the currently authenticated user, read from the JWT principal
     * populated by JwtAuthFilter. Cheap "who am I" call that doesn't re-hit
     * the database for the user record -- prefer validateToken if you need
     * the fresh user state (e.g. status, role changes).
     */
    @Operation(summary = "Current user", description = "Returns the user identified by the JWT in the Authorization header.")
    @GetMapping("/me")
    public UserDTO me(@AuthenticationPrincipal AuthenticatedUser principal) {
        UserDTO user = userService.getUserProfile(principal.userId());
        return scrub(user);
    }

    private static String extractToken(String authHeader, String tokenParam) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }
        return tokenParam;
    }

    private static UserDTO scrub(UserDTO u) {
        // Never leak the bcrypt hash in introspection responses.
        return new UserDTO(u.userId(), u.email(), null, u.fullName(), u.phoneNumber(),
                u.role(), u.status(), u.address());
    }
}
