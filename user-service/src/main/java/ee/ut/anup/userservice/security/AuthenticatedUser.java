package ee.ut.anup.userservice.security;

import java.util.UUID;

/**
 * Identity extracted from a validated JWT. User-service now stores native UUID
 * ids, so the JWT's userId claim maps 1:1 to User.userId -- no synthesis.
 */
public record AuthenticatedUser(UUID userId, String role, String tokenType) {

    public boolean isService() {
        return "SERVICE".equals(tokenType);
    }
}
