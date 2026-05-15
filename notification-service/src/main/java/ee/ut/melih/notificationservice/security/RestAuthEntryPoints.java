package ee.ut.melih.notificationservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAuthEntryPoints {

  private static final Logger log = LoggerFactory.getLogger(RestAuthEntryPoints.class);

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
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", OffsetDateTime.now().toString());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    body.put("path", request.getRequestURI());
    objectMapper.writeValue(response.getWriter(), body);
  }
}
