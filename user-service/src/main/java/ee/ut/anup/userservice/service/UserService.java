package ee.ut.anup.userservice.service;

import ee.ut.anup.userservice.dto.*;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserDTO registerUser(UserDTO userDTO);
    LoginResponseDTO login(AuthRequest authRequest);
    UserDTO getUserProfile(UUID userId);
    UserDTO getUserByEmail(String email);
    UserDTO updateUserProfile(UUID userId, UpdateUserDTO userDTO);
    List<AddressDTO> getUserAddresses(UUID userId);
    AddressDTO addUserAddress(UUID userId, AddressDTO addressDTO);
    List<DriverProfileDTO> getAvailableDrivers();
}
