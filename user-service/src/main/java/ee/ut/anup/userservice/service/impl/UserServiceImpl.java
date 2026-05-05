package ee.ut.anup.userservice.service.impl;

import ee.ut.anup.userservice.dto.AddressDTO;
import ee.ut.anup.userservice.dto.DriverProfileDTO;
import ee.ut.anup.userservice.dto.LoginRequestDTO;
import ee.ut.anup.userservice.dto.LoginResponseDTO;
import ee.ut.anup.userservice.dto.UserDTO;
import ee.ut.anup.userservice.entity.Address;
import ee.ut.anup.userservice.entity.DriverProfile;
import ee.ut.anup.userservice.entity.User;
import ee.ut.anup.userservice.exception.ResourceNotFoundException;
import ee.ut.anup.userservice.exception.UserAlreadyExistsException;
import ee.ut.anup.userservice.mapper.AddressMapper;
import ee.ut.anup.userservice.mapper.DriverProfileMapper;
import ee.ut.anup.userservice.mapper.UserMapper;
import ee.ut.anup.userservice.repository.AddressRepository;
import ee.ut.anup.userservice.repository.DriverProfileRepository;
import ee.ut.anup.userservice.repository.UserRepository;
import ee.ut.anup.userservice.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    Optional<User> userExists = userRepository.findByEmail(userDTO.email());
    if (userExists.isPresent()) {
      throw new UserAlreadyExistsException("Email already in use");
    }

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
  public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
    User user =
        userRepository
            .findByEmail(loginRequestDTO.email())
            .filter(u -> u.getPasswordHash().equals(loginRequestDTO.password()))
            .orElseThrow(
                () ->
                    new ee.ut.anup.userservice.exception.InvalidCredentialsException(
                        "Invalid credentials"));

    String token = "mock-jwt-token-" + user.getEmail();
    return new LoginResponseDTO(token, userMapper.toDto(user));
  }

  @Override
  public UserDTO getUserProfile(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return userMapper.toDto(user);
  }

  @Override
  @Transactional
  public UserDTO updateUserProfile(Long userId, UserDTO userDTO) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
