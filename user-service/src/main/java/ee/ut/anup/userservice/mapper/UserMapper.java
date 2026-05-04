package ee.ut.anup.userservice.mapper;

import ee.ut.anup.userservice.dto.UserDTO;
import ee.ut.anup.userservice.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserDTO toDto(User user) {
    return new UserDTO(
        user.getEmail(),
        user.getPasswordHash(),
        user.getFullName(),
        user.getPhoneNumber(),
        user.getRole(),
        user.getStatus());
  }

  public User toEntity(UserDTO userDTO) {
    User user = new User();
    user.setEmail(userDTO.email());
    user.setPasswordHash(userDTO.password());
    user.setFullName(userDTO.fullName());
    user.setPhoneNumber(userDTO.phoneNumber());
    user.setRole(userDTO.role());
    user.setStatus(userDTO.status());
    return user;
  }

  public void applyProfileUpdate(UserDTO userDTO, User user) {
    user.setFullName(userDTO.fullName());
    user.setPhoneNumber(userDTO.phoneNumber());
  }
}
