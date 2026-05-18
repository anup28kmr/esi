package ee.ut.quickbite.userservice.config;

import ee.ut.quickbite.userservice.entity.User;
import ee.ut.quickbite.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        log.info("Loading user for authentication, email={}", username);

        Optional<User> credential = repository.findByEmail(username);
        if (credential.isEmpty()) {
            log.info("User not found for email={}", username);
            throw new UsernameNotFoundException("User not found with username :" + username);
        }

        log.info("User found for email={}, role={}", username, credential.get().getRole());
        return new CustomUserDetails(credential.get());
    }
}