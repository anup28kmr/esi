package ee.ut.quickbite.userservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
public class DriverProfile {
    // Shared PK with User -- @MapsId means this column IS users.user_id, so
    // it must be UUID now that User.userId is UUID.
    @Id
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String vehicleType;
    private String licenseNumber;
    private boolean isAvailable;
}
