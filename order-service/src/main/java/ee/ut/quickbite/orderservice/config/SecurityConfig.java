package ee.ut.quickbite.orderservice.config;

import ee.ut.quickbite.orderservice.security.JwtAuthFilter;
import ee.ut.quickbite.orderservice.security.RestAuthEntryPoints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthEntryPoints authEntryPoints;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RestAuthEntryPoints authEntryPoints) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authEntryPoints = authEntryPoints;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CORS is handled centrally by the api-gateway. Setting it here would
        // duplicate Access-Control-Allow-* headers on proxied responses, which
        // browsers reject.
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoints.unauthorizedEntryPoint())
                .accessDeniedHandler(authEntryPoints.forbiddenHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health", "/actuator/info",
                    "/v3/api-docs/**", "/v3/api-docs.yaml",
                    "/swagger-ui/**", "/swagger-ui.html"
                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Every /orders/** endpoint requires a valid JWT.
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
