package ee.ut.esi.quickbite.restaurant.config;

import ee.ut.esi.quickbite.restaurant.security.JwtAuthFilter;
import ee.ut.esi.quickbite.restaurant.security.RestAuthEntryPoints;
import ee.ut.esi.quickbite.restaurant.security.SecurityRoles;
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
        // CORS is handled centrally by the api-gateway. Browsers never call
        // this service directly under the frontend -> gateway -> service
        // architecture, so setting CORS here would only duplicate the
        // gateway's Access-Control-Allow-* headers on proxied responses,
        // which browsers reject.
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
                // Public browse endpoints per 0010 §6.
                .requestMatchers(HttpMethod.GET, "/restaurants").permitAll()
                .requestMatchers(HttpMethod.GET, "/restaurants/{id:[0-9a-fA-F-]+}").permitAll()
                // Availability: any valid token (customer/owner/driver/admin/service) per 0010 §8.
                .requestMatchers(HttpMethod.GET, "/restaurants/*/availability").authenticated()
                // Mutations: restaurant owner.
                .requestMatchers(HttpMethod.POST,   "/restaurants")
                    .hasRole(SecurityRoles.RESTAURANT_OWNER)
                .requestMatchers(HttpMethod.PUT,    "/restaurants/*")
                    .hasRole(SecurityRoles.RESTAURANT_OWNER)
                .requestMatchers(HttpMethod.PATCH,  "/restaurants/*/status")
                    .hasRole(SecurityRoles.RESTAURANT_OWNER)
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
