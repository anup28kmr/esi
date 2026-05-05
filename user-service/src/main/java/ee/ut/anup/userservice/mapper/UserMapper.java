package ee.ut.anup.userservice.mapper;

import ee.ut.anup.userservice.dto.UserDTO;
import ee.ut.anup.userservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

  private final AddressMapper addressMapper;

  public UserDTO toDto(User user) {
    return new UserDTO(
        user.getUserId(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getFullName(),
        user.getPhoneNumber(),
        user.getRole(),
        user.getStatus(),
        (user.getAddresses() != null && !user.getAddresses().isEmpty())
            ? addressMapper.toDto(
                user.getAddresses().stream()
                    .filter(ee.ut.anup.userservice.entity.Address::isDefault)
                    .findFirst()
                    .orElse(user.getAddresses().get(0)))
            : null);
  }

  public User toEntity(UserDTO userDTO) {
    User user = new User();
    user.setEmail(userDTO.email());
    user.setPasswordHash(userDTO.password());
    user.setFullName(userDTO.fullName());
    user.setPhoneNumber(userDTO.phoneNumber());
    user.setRole(userDTO.role());
    user.setStatus(userDTO.status());

    if (userDTO.address() != null) {
      ee.ut.anup.userservice.entity.Address address = addressMapper.toEntity(userDTO.address());
      address.setUser(user);
      address.setDefault(true);
      user.getAddresses().add(address);
    }

    return user;
  }

  public void applyProfileUpdate(UserDTO userDTO, User user) {
    user.setFullName(userDTO.fullName());
    user.setPhoneNumber(userDTO.phoneNumber());
  }
}
