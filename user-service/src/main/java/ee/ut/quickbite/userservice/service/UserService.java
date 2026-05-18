package ee.ut.quickbite.userservice.service;

import ee.ut.quickbite.userservice.dto.AddressDTO;
import ee.ut.quickbite.userservice.dto.AuthRequest;
import ee.ut.quickbite.userservice.dto.DriverProfileDTO;
import ee.ut.quickbite.userservice.dto.LoginResponseDTO;
import ee.ut.quickbite.userservice.dto.UpdateUserDTO;
import ee.ut.quickbite.userservice.dto.UserDTO;

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
