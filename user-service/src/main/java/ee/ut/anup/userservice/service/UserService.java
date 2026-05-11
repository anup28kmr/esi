package ee.ut.anup.userservice.service;

import ee.ut.anup.userservice.dto.*;
import java.util.List;

public interface UserService {
    UserDTO registerUser(UserDTO userDTO);
    LoginResponseDTO login(AuthRequest authRequest);
    UserDTO getUserProfile(Long userId);
    UserDTO getUserByEmail(String email);
    UserDTO updateUserProfile(Long userId, UpdateUserDTO userDTO);
    List<AddressDTO> getUserAddresses(Long userId);
    AddressDTO addUserAddress(Long userId, AddressDTO addressDTO);
    List<DriverProfileDTO> getAvailableDrivers();
}
