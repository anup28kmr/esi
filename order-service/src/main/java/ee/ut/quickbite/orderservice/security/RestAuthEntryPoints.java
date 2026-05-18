package ee.ut.quickbite.orderservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ee.ut.quickbite.orderservice.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@Slf4j
public class RestAuthEntryPoints {

    // Boot 4's slim spring-boot-starter-webmvc does not auto-configure an
    // ObjectMapper bean, so we own one here. The error body is a fixed shape
    // (no app-level Jackson config to inherit), only the LocalDateTime needs
    // the JSR-310 module to serialize as ISO-8601 rather than an array.
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) -> {
            log.warn("security denial 401 method={} path={} reason={}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
            writeError(request, response, HttpStatus.UNAUTHORIZED, "Authentication required");
        };
    }

    public AccessDeniedHandler forbiddenHandler() {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Object principal = auth == null ? null : auth.getPrincipal();
            String actor = principal instanceof AuthenticatedUser au
                ? au.userId() + "/" + au.role()
                : String.valueOf(principal);
            log.warn("security denial 403 method={} path={} actor={} reason={}",
                request.getMethod(), request.getRequestURI(), actor, ex.getMessage());
            writeError(request, response, HttpStatus.FORBIDDEN, "Access denied");
        };
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response,
                            HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponseDto body = new ErrorResponseDto(
            request.getRequestURI(),
            status,
            message,
            LocalDateTime.now()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
