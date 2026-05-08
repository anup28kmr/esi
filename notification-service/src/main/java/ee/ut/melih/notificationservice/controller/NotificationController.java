package ee.ut.melih.notificationservice.controller;

import ee.ut.melih.notificationservice.dto.MarkAllReadResponse;
import ee.ut.melih.notificationservice.dto.NotificationResponse;
import ee.ut.melih.notificationservice.dto.SendNotificationRequest;
import ee.ut.melih.notificationservice.dto.UnreadCountResponse;
import ee.ut.melih.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "User-facing notification inbox and dispatch endpoints.")
public class NotificationController {

  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "List notifications of the current user")
  public List<NotificationResponse> list(
      @Parameter(description = "Authenticated user id (propagated by the API Gateway)")
          @RequestHeader("X-User-Id")
          UUID userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return service.list(userId, pageable).stream().map(NotificationResponse::from).toList();
  }

  @GetMapping("/unread-count")
  @Operation(summary = "Get the number of unread notifications for the current user")
  public UnreadCountResponse unreadCount(
      @Parameter(description = "Authenticated user id (propagated by the API Gateway)")
          @RequestHeader("X-User-Id")
          UUID userId) {
    return new UnreadCountResponse(service.unreadCount(userId));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a single notification owned by the current user")
  public NotificationResponse get(
      @Parameter(description = "Authenticated user id (propagated by the API Gateway)")
          @RequestHeader("X-User-Id")
          UUID userId,
      @PathVariable UUID id) {
    return NotificationResponse.from(service.get(id, userId));
  }

  @PatchMapping("/read-all")
  @Operation(summary = "Mark all notifications of the current user as read")
  public MarkAllReadResponse markAllRead(
      @Parameter(description = "Authenticated user id (propagated by the API Gateway)")
          @RequestHeader("X-User-Id")
          UUID userId) {
    return new MarkAllReadResponse(service.markAllRead(userId));
  }

  @PatchMapping("/{id}/read")
  @Operation(summary = "Mark a single notification as read")
  public NotificationResponse markRead(
      @Parameter(description = "Authenticated user id (propagated by the API Gateway)")
          @RequestHeader("X-User-Id")
          UUID userId,
      @PathVariable UUID id) {
    return NotificationResponse.from(service.markRead(id, userId));
  }

  @PostMapping("/send")
  @Operation(summary = "Internal send endpoint used for admin actions and tests")
  public ResponseEntity<NotificationResponse> send(@Valid @RequestBody SendNotificationRequest request) {
    NotificationResponse response = NotificationResponse.from(service.send(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
