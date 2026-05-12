package ee.ut.ege.deliveryservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtProperties {

    private final String secret;
    private final String issuer;

    public JwtProperties(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.issuer}") String issuer
    ) {
        this.secret = secret;
        this.issuer = issuer;
    }

    public String secret() {
        return secret;
    }

    public String issuer() {
        return issuer;
    }
}
