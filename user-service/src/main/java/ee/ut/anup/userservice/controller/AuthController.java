package ee.ut.anup.userservice.controller;

import ee.ut.anup.userservice.dto.AuthRequest;
import ee.ut.anup.userservice.dto.ErrorResponseDTO;
import ee.ut.anup.userservice.dto.LoginResponseDTO;
import ee.ut.anup.userservice.dto.UserDTO;
import ee.ut.anup.userservice.service.AuthService;
import ee.ut.anup.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user authentication")
public class AuthController {

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
            String token = authService.generateToken(authRequest.email());
            log.info("Token generated for email={}", authRequest.email());
            UserDTO user = userService.getUserByEmail(authRequest.email());
            return new LoginResponseDTO(token, user);
        } else {
            log.info("Login denied for email={}", authRequest.email());
            throw new RuntimeException("invalid access");
        }
    }

    @GetMapping("/validateToken")
    public String validateToken(@RequestParam("token") String token) {
         authService.validateToken(token);
        return "Token is valid";
    }
}
