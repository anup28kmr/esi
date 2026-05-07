package ee.ut.anup.userservice.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ee.ut.anup.userservice.dto.AddressDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
                driverProfileMapper
        );
    }

    @Test
    void registerUser_shouldSaveAddress_whenProvidedInDto() {
        // Given
        AddressDTO addressDto = new AddressDTO("Main St", "City", "12345", "Home", true);
        UserDTO userDto = new UserDTO(
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
            user.getAddresses().get(0).getStreet().equals("Main St") &&
            user.getAddresses().get(0).getUser() == user
        ));
    }
}
