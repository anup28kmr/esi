package ee.ut.melih.notificationservice.controller;

import ee.ut.melih.notificationservice.dto.MarkAllReadResponse;
import ee.ut.melih.notificationservice.dto.NotificationResponse;
import ee.ut.melih.notificationservice.dto.SendNotificationRequest;
import ee.ut.melih.notificationservice.dto.UnreadCountResponse;
import ee.ut.melih.notificationservice.security.AuthenticatedUser;
import ee.ut.melih.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    UUID userId = requireUser(user);
    Pageable pageable = PageRequest.of(page, size);
    return service.list(userId, pageable).stream().map(NotificationResponse::from).toList();
  }

  @GetMapping("/unread-count")
  @Operation(summary = "Get the number of unread notifications for the current user")
  public UnreadCountResponse unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
    return new UnreadCountResponse(service.unreadCount(requireUser(user)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a single notification owned by the current user")
  public NotificationResponse get(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id) {
    return NotificationResponse.from(service.get(id, requireUser(user)));
  }

  @PatchMapping("/read-all")
  @Operation(summary = "Mark all notifications of the current user as read")
  public MarkAllReadResponse markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
    return new MarkAllReadResponse(service.markAllRead(requireUser(user)));
  }

  @PatchMapping("/{id}/read")
  @Operation(summary = "Mark a single notification as read")
  public NotificationResponse markRead(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id) {
    return NotificationResponse.from(service.markRead(id, requireUser(user)));
  }

  @PostMapping("/send")
  @Operation(summary = "Internal send endpoint — only callable with a SERVICE-scoped JWT issued for service-to-service traffic")
  public ResponseEntity<NotificationResponse> send(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody SendNotificationRequest request) {
    // Only service tokens (issued for internal service-to-service calls)
    // are allowed to push notifications on behalf of someone else.
    if (user == null || !user.isService()) {
      throw new AccessDeniedException("Service token required to send notifications");
    }
    NotificationResponse response = NotificationResponse.from(service.send(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  private static UUID requireUser(AuthenticatedUser user) {
    if (user == null || user.userId() == null) {
      throw new AccessDeniedException("Authenticated user required");
    }
    return user.userId();
  }
}
