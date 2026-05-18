package ee.ut.quickbite.orderservice.security;

import java.util.UUID;

/**
 * Identity extracted from the user-service JWT. User-service now stores
 * native UUID ids and emits them directly into the userId claim -- no
 * synthesis. Downstream services consume the UUID as the user identifier.
 */
public record AuthenticatedUser(UUID userId, String role, String tokenType) {

    public boolean isService() {
        return "SERVICE".equals(tokenType);
    }
}
