package ee.ut.esi.quickbite.menu.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProperties jwt;
    private final Key signingKey;

    public JwtAuthFilter(JwtProperties jwt) {
        this.jwt = jwt;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.secret()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
        throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(jwt.issuer())
                .build()
                .parseClaimsJws(token)
                .getBody();

            UUID userId = UUID.fromString(requireClaim(claims, "userId"));
            String role = requireClaim(claims, "role");
            String tokenType = claims.get("tokenType", String.class);
            if (tokenType == null) {
                tokenType = "USER";
            }
            String serviceName = claims.get("serviceName", String.class);

            AuthenticatedUser principal = new AuthenticatedUser(userId, role, tokenType, serviceName);
            var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            // Token is unrecognized (bad signature, missing claims, wrong issuer,
            // user-id not a UUID, etc.). Don't reject outright -- clear any partial
            // context and let the filter chain's authorization rules decide. This
            // way permitAll() endpoints still succeed (anonymously) and the SPA
            // doesn't get logged out when it browses a public page with a token
            // that this service can't interpret. Protected endpoints return 401
            // via RestAuthEntryPoints.unauthorizedEntryPoint().
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }

    private static String requireClaim(Claims claims, String name) {
        String value = claims.get(name, String.class);
        if (value == null || value.isBlank()) {
            throw new JwtException("missing '" + name + "' claim");
        }
        return value;
    }
}
