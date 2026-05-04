package ee.ut.anup.userservice.service.impl;

import ee.ut.anup.userservice.dto.AddressDTO;
import ee.ut.anup.userservice.dto.DriverProfileDTO;
import ee.ut.anup.userservice.dto.LoginRequest;
import ee.ut.anup.userservice.dto.LoginResponse;
import ee.ut.anup.userservice.dto.UserDTO;
import ee.ut.anup.userservice.entity.Address;
import ee.ut.anup.userservice.entity.DriverProfile;
import ee.ut.anup.userservice.entity.User;
import ee.ut.anup.userservice.mapper.AddressMapper;
import ee.ut.anup.userservice.mapper.DriverProfileMapper;
import ee.ut.anup.userservice.mapper.UserMapper;
import ee.ut.anup.userservice.repository.AddressRepository;
import ee.ut.anup.userservice.repository.DriverProfileRepository;
import ee.ut.anup.userservice.repository.UserRepository;
import ee.ut.anup.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final DriverProfileMapper driverProfileMapper;

    @Override
    @Transactional
    public UserDTO registerUser(UserDTO userDTO) {
        User user = userMapper.toEntity(userDTO);
        
        User savedUser = userRepository.save(user);
        
        if (user.getRole() == User.Role.DRIVER) {
            DriverProfile profile = new DriverProfile();
            profile.setUser(savedUser);
            profile.setAvailable(true);
            driverProfileRepository.save(profile);
        }
        
        return userMapper.toDto(savedUser);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.email())
                .filter(u -> u.getPasswordHash().equals(loginRequest.password()))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        
        String token = "mock-jwt-token-" + user.getEmail();
        return new LoginResponse(token, userMapper.toDto(user));
    }

    @Override
    public UserDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDTO updateUserProfile(Long userId, UserDTO userDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        userMapper.applyProfileUpdate(userDTO, user);
        
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public List<AddressDTO> getUserAddresses(Long userId) {
        return addressRepository.findByUserUserId(userId).stream()
                .map(addressMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressDTO addUserAddress(Long userId, AddressDTO addressDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Address address = addressMapper.toEntity(addressDTO);
        address.setUser(user);
        
        return addressMapper.toDto(addressRepository.save(address));
    }

    @Override
    public List<DriverProfileDTO> getAvailableDrivers() {
        return driverProfileRepository.findByIsAvailable(true).stream()
                .map(driverProfileMapper::toDto)
                .collect(Collectors.toList());
    }
}
