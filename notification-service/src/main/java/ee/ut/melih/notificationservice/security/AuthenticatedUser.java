package ee.ut.melih.notificationservice.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String role, String tokenType) {

  public boolean isService() {
    return "SERVICE".equals(tokenType);
  }
}
