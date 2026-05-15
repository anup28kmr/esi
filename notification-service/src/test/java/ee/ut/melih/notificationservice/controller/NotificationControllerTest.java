package ee.ut.melih.notificationservice.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.ut.melih.notificationservice.config.SecurityConfig;
import ee.ut.melih.notificationservice.domain.Channel;
import ee.ut.melih.notificationservice.domain.Notification;
import ee.ut.melih.notificationservice.domain.NotificationStatus;
import ee.ut.melih.notificationservice.dto.SendNotificationRequest;
import ee.ut.melih.notificationservice.repository.NotificationRepository;
import ee.ut.melih.notificationservice.security.AuthenticatedUser;
import ee.ut.melih.notificationservice.security.JwtAuthFilter;
import ee.ut.melih.notificationservice.security.RestAuthEntryPoints;
import ee.ut.melih.notificationservice.service.MessageDispatcher;
import ee.ut.melih.notificationservice.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@Import({NotificationService.class, SecurityConfig.class, RestAuthEntryPoints.class})
class NotificationControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private NotificationRepository repository;
  @MockBean private MessageDispatcher dispatcher;
  // JwtAuthFilter is bypassed via the .with(authentication(...)) request
  // post-processor below, but it still has to be present in the context
  // so the filter chain wires up cleanly.
  @MockBean private JwtAuthFilter jwtAuthFilter;

  private static Authentication service() {
    var principal = new AuthenticatedUser(UUID.randomUUID(), "SERVICE", "SERVICE");
    return new UsernamePasswordAuthenticationToken(
        principal, null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
  }

  private static Authentication user() {
    var principal = new AuthenticatedUser(UUID.randomUUID(), "CUSTOMER", "USER");
    return new UsernamePasswordAuthenticationToken(
        principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
  }

  @Test
  void send_happyPath_returns201_andMarksSent() throws Exception {
    UUID recipientId = UUID.randomUUID();
    given(repository.save(any(Notification.class)))
        .willAnswer(
            inv -> {
              Notification n = inv.getArgument(0);
              if (n.getId() == null) {
                n.setId(UUID.randomUUID());
              }
              return n;
            });

    SendNotificationRequest body =
        new SendNotificationRequest(recipientId, Channel.EMAIL, "Order placed");

    mvc.perform(
            post("/notifications/send")
                .with(authentication(service()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.recipientId", is(recipientId.toString())))
        .andExpect(jsonPath("$.channel", is("EMAIL")))
        .andExpect(jsonPath("$.message", is("Order placed")))
        .andExpect(jsonPath("$.status", is(NotificationStatus.SENT.name())));

    verify(dispatcher).dispatch(any(Notification.class));
    verify(repository, atLeastOnce()).save(any(Notification.class));
  }

  @Test
  void send_dispatcherFails_returns502_andPersistsFailedStatus() throws Exception {
    UUID recipientId = UUID.randomUUID();
    given(repository.save(any(Notification.class)))
        .willAnswer(
            inv -> {
              Notification n = inv.getArgument(0);
              if (n.getId() == null) {
                n.setId(UUID.randomUUID());
              }
              return n;
            });
    willThrow(new RuntimeException("provider down"))
        .given(dispatcher)
        .dispatch(any(Notification.class));

    SendNotificationRequest body =
        new SendNotificationRequest(recipientId, Channel.SMS, "Order delivered");

    mvc.perform(
            post("/notifications/send")
                .with(authentication(service()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.status", is(502)));
  }

  @Test
  void send_invalidPayload_returns400() throws Exception {
    Map<String, Object> body =
        Map.of("recipientId", UUID.randomUUID().toString(), "channel", "EMAIL", "message", "");

    mvc.perform(
            post("/notifications/send")
                .with(authentication(service()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void send_userTokenWithoutServiceRole_returns403() throws Exception {
    SendNotificationRequest body =
        new SendNotificationRequest(UUID.randomUUID(), Channel.EMAIL, "Order placed");

    mvc.perform(
            post("/notifications/send")
                .with(authentication(user()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isForbidden());
  }

  @Test
  void send_anonymous_returns401() throws Exception {
    SendNotificationRequest body =
        new SendNotificationRequest(UUID.randomUUID(), Channel.EMAIL, "Order placed");

    mvc.perform(
            post("/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isUnauthorized());
  }
}
