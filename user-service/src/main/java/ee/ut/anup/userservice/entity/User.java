package ee.ut.anup.userservice.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private DriverProfile driverProfile;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = Status.ACTIVE;
        }
    }

    public enum Role {
        CUSTOMER, DRIVER, RESTAURANT_OWNER, ADMIN;

        @JsonCreator
        public static Role fromValue(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }

            String normalized = value.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase();

            if ("RESTAURANTOWNER".equals(normalized)) {
                return RESTAURANT_OWNER;
            }

            return switch (normalized) {
                case "CUSTOMER" -> CUSTOMER;
                case "DRIVER" -> DRIVER;
                case "RESTAURANT_OWNER" -> RESTAURANT_OWNER;
                case "ADMIN" -> ADMIN;
                default -> throw new IllegalArgumentException("Unknown role: " + value);
            };
        }
    }

    public enum Status {
        ACTIVE, SUSPENDED
    }
}
