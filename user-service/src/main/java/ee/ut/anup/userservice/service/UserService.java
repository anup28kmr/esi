package ee.ut.anup.userservice.service;

import ee.ut.anup.userservice.dto.*;
import java.util.List;

public interface UserService {
    UserDTO registerUser(UserDTO userDTO);
    LoginResponse login(LoginRequest loginRequest);
    UserDTO getUserProfile(Long userId);
    UserDTO updateUserProfile(Long userId, UserDTO userDTO);
    List<AddressDTO> getUserAddresses(Long userId);
    AddressDTO addUserAddress(Long userId, AddressDTO addressDTO);
    List<DriverProfileDTO> getAvailableDrivers();
}
