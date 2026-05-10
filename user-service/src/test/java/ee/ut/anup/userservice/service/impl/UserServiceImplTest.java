package ee.ut.anup.userservice.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ee.ut.anup.userservice.dto.AddressDTO;
import ee.ut.anup.userservice.dto.UpdateUserDTO;
import ee.ut.anup.userservice.dto.UserDTO;
import ee.ut.anup.userservice.entity.Address;
import ee.ut.anup.userservice.entity.User;
import ee.ut.anup.userservice.mapper.AddressMapper;
import ee.ut.anup.userservice.mapper.DriverProfileMapper;
import ee.ut.anup.userservice.mapper.UserMapper;
import ee.ut.anup.userservice.repository.AddressRepository;
import ee.ut.anup.userservice.repository.DriverProfileRepository;
import ee.ut.anup.userservice.repository.UserRepository;
import java.util.Optional;

import ee.ut.anup.userservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;


@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private DriverProfileRepository driverProfileRepository;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private DriverProfileMapper driverProfileMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthService authService;

    private UserMapper userMapper;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper(addressMapper);
        userService = new UserServiceImpl(
                userRepository,
                addressRepository,
                driverProfileRepository,
                userMapper,
                addressMapper,
                driverProfileMapper,
                passwordEncoder,
                authService
        );
    }

    @Test
    void registerUser_shouldSaveAddress_whenProvidedInDto() {
        // Given
        AddressDTO addressDto = new AddressDTO("Main St", "City", "12345", "Home", true);
        UserDTO userDto = new UserDTO(
                null,
                "test@example.com",
                "password",
                "Test User",
                "12345678",
                User.Role.CUSTOMER,
                User.Status.ACTIVE,
                addressDto
        );

        User userEntity = new User();
        userEntity.setEmail(userDto.email());
        
        Address addressEntity = new Address();
        addressEntity.setStreet("Main St");

        when(userRepository.findByEmail(userDto.email())).thenReturn(Optional.empty());
        when(addressMapper.toEntity(addressDto)).thenReturn(addressEntity);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.registerUser(userDto);

        // Then
        verify(userRepository).save(argThat(user -> 
            user.getAddresses().size() == 1 &&
            user.getAddresses().getFirst().getStreet().equals("Main St") &&
            user.getAddresses().getFirst().getUser() == user
        ));
    }

    @Test
    void updateUserProfile_shouldEncodePassword_whenNewPasswordProvided() {
        User existingUser = new User();
        existingUser.setUserId(7L);
        existingUser.setEmail("test@example.com");
        existingUser.setPassword("old-hash");
        existingUser.setFullName("Test User");
        existingUser.setPhoneNumber("12345678");

        UpdateUserDTO request = new UpdateUserDTO(
                7L,
                "test@example.com",
                "new-secret",
                "Updated User",
                "87654321",
                User.Role.CUSTOMER,
                User.Status.ACTIVE,
                null
        );

        when(userRepository.findById(7L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("new-secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUserProfile(7L, request);

        verify(passwordEncoder).encode("new-secret");
        verify(userRepository).save(argThat(user ->
                "encoded-secret".equals(user.getPassword()) &&
                "Updated User".equals(user.getFullName()) &&
                "87654321".equals(user.getPhoneNumber())
        ));
    }

    @Test
    void updateUserProfile_shouldKeepPassword_whenBlankPasswordProvided() {
        User existingUser = new User();
        existingUser.setUserId(8L);
        existingUser.setEmail("test@example.com");
        existingUser.setPassword("old-hash");

        UpdateUserDTO request = new UpdateUserDTO(
                8L,
                "test@example.com",
                "",
                "Updated User",
                "87654321",
                User.Role.CUSTOMER,
                User.Status.ACTIVE,
                null
        );

        when(userRepository.findById(8L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUserProfile(8L, request);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(argThat(user ->
                "old-hash".equals(user.getPassword()) &&
                "Updated User".equals(user.getFullName()) &&
                "87654321".equals(user.getPhoneNumber())
        ));
    }
}
