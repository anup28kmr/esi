package ee.ut.anup.userservice.service.impl;

import ee.ut.anup.userservice.dto.AddressDTO;
import ee.ut.anup.userservice.dto.AuthRequest;
import ee.ut.anup.userservice.dto.DriverProfileDTO;
import ee.ut.anup.userservice.dto.LoginResponseDTO;
import ee.ut.anup.userservice.dto.UpdateUserDTO;
import ee.ut.anup.userservice.dto.UserDTO;
import ee.ut.anup.userservice.entity.Address;
import ee.ut.anup.userservice.entity.DriverProfile;
import ee.ut.anup.userservice.entity.User;
import ee.ut.anup.userservice.exception.InvalidCredentialsException;
import ee.ut.anup.userservice.exception.ResourceNotFoundException;
import ee.ut.anup.userservice.exception.UserAlreadyExistsException;
import ee.ut.anup.userservice.mapper.AddressMapper;
import ee.ut.anup.userservice.mapper.DriverProfileMapper;
import ee.ut.anup.userservice.mapper.UserMapper;
import ee.ut.anup.userservice.repository.AddressRepository;
import ee.ut.anup.userservice.repository.DriverProfileRepository;
import ee.ut.anup.userservice.repository.UserRepository;
import ee.ut.anup.userservice.service.AuthService;
import ee.ut.anup.userservice.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final AddressRepository addressRepository;
  private final DriverProfileRepository driverProfileRepository;
  private final UserMapper userMapper;
  private final AddressMapper addressMapper;
  private final DriverProfileMapper driverProfileMapper;
  private final PasswordEncoder passwordEncoder;
  private final AuthService authService;

  @Override
  @Transactional
  public UserDTO registerUser(UserDTO userDTO) {
    log.info("Registering user, email={}, role={}", userDTO.email(), userDTO.role());

    Optional<User> userExists = userRepository.findByEmail(userDTO.email());
    if (userExists.isPresent()) {
      log.warn("Registration rejected, email already in use={}", userDTO.email());
      throw new UserAlreadyExistsException("Email already in use");
    }

    User user = userMapper.toEntity(userDTO);
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    User savedUser = userRepository.save(user);
    log.info("User persisted successfully, userId={}, email={}", savedUser.getUserId(), savedUser.getEmail());

    if (user.getRole() == User.Role.DRIVER) {
      DriverProfile profile = new DriverProfile();
      profile.setUser(savedUser);
      profile.setAvailable(true);
      driverProfileRepository.save(profile);
      log.info("Default driver profile created, userId={}", savedUser.getUserId());
    }

    return userMapper.toDto(savedUser);
  }

  @Override
  public LoginResponseDTO login(AuthRequest authRequest) {
    log.info("Login requested for email={}", authRequest.email());

    User user =
        userRepository
            .findByEmail(authRequest.email())
            .filter(u -> passwordEncoder.matches(authRequest.password(), u.getPassword()))
            .orElseThrow(
                () -> {
                  log.warn("Login failed due to invalid credentials, email={}", authRequest.email());
                  return new InvalidCredentialsException("Invalid credentials");
                });

    String token = authService.generateToken(user.getUserId(), user.getEmail(), user.getRole());
    log.info("Login succeeded for userId={}, email={}", user.getUserId(), user.getEmail());
    return new LoginResponseDTO(token, userMapper.toDto(user));
  }

  @Override
  public UserDTO getUserProfile(UUID userId) {
    log.info("Fetching user profile, userId={}", userId);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> {
                  log.warn("User profile not found, userId={}", userId);
                  return new ResourceNotFoundException("User not found");
                });
    log.info("User profile fetched, userId={}", userId);
    return userMapper.toDto(user);
  }

  @Override
  public UserDTO getUserByEmail(String email) {
    log.info("Fetching user by email, email={}", email);
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () -> {
                  log.warn("User not found, email={}", email);
                  return new ResourceNotFoundException("User not found");
                });
    log.info("User fetched by email, email={}", email);
    return userMapper.toDto(user);
  }

  @Override
  @Transactional
  public UserDTO updateUserProfile(UUID userId, UpdateUserDTO userDTO) {
    log.info("Updating user profile, userId={}", userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> {
                  log.warn("Update rejected, user not found, userId={}", userId);
                  return new ResourceNotFoundException("User not found");
                });

    userMapper.applyProfileUpdate(userDTO, user);
    if (userDTO.password() != null && !userDTO.password().isBlank()) {
      user.setPassword(passwordEncoder.encode(userDTO.password()));
    }
    User savedUser = userRepository.save(user);
    log.info("User profile updated, userId={}", userId);
    return userMapper.toDto(savedUser);
  }

  @Override
  public List<AddressDTO> getUserAddresses(UUID userId) {
    log.info("Fetching addresses for userId={}", userId);
    List<AddressDTO> addresses =
        addressRepository.findByUserUserId(userId).stream()
            .map(addressMapper::toDto)
            .collect(Collectors.toList());
    log.info("Addresses fetched for userId={}, count={}", userId, addresses.size());
    return addresses;
  }

  @Override
  @Transactional
  public AddressDTO addUserAddress(UUID userId, AddressDTO addressDTO) {
    log.info("Adding address for userId={}", userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> {
                  log.warn("Add address rejected, user not found, userId={}", userId);
                  return new ResourceNotFoundException("User not found");
                });

    Address address = addressMapper.toEntity(addressDTO);
    address.setUser(user);
    Address savedAddress = addressRepository.save(address);
    log.info("Address added for userId={}", userId);

    return addressMapper.toDto(savedAddress);
  }

  @Override
  public List<DriverProfileDTO> getAvailableDrivers() {
    log.info("Fetching available driver profiles");
    List<DriverProfileDTO> drivers =
        driverProfileRepository.findByIsAvailable(true).stream()
            .map(driverProfileMapper::toDto)
            .collect(Collectors.toList());
    log.info("Available driver profiles fetched, count={}", drivers.size());
    return drivers;
  }
}
