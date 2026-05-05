package ee.ut.anup.userservice.dto;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

public record ErrorResponseDTO(
    String apiPath, HttpStatus errorCode, String errorMessage, LocalDateTime errorTime) {}
