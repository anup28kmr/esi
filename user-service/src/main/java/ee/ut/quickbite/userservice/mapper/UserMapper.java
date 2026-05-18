package ee.ut.quickbite.userservice.mapper;

import ee.ut.quickbite.userservice.dto.UpdateUserDTO;
import ee.ut.quickbite.userservice.dto.UserDTO;
import ee.ut.quickbite.userservice.entity.User;
import ee.ut.quickbite.userservice.entity.Address;
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
        null,
        user.getFullName(),
        user.getPhoneNumber(),
        user.getRole(),
        user.getStatus(),
        (user.getAddresses() != null && !user.getAddresses().isEmpty())
            ? addressMapper.toDto(
                user.getAddresses().stream()
                    .filter(Address::isDefault)
                    .findFirst()
                    .orElse(user.getAddresses().get(0)))
            : null);
  }

  public User toEntity(UserDTO userDTO) {
    User user = new User();
    user.setEmail(userDTO.email());
    user.setPassword(userDTO.password());
    user.setFullName(userDTO.fullName());
    user.setPhoneNumber(userDTO.phoneNumber());
    user.setRole(userDTO.role());
    user.setStatus(userDTO.status());

    if (userDTO.address() != null) {
      Address address = addressMapper.toEntity(userDTO.address());
      address.setUser(user);
      address.setDefault(true);
      user.getAddresses().add(address);
    }

    return user;
  }

  public void applyProfileUpdate(UpdateUserDTO userDTO, User user) {
    user.setFullName(userDTO.fullName());
    user.setPhoneNumber(userDTO.phoneNumber());
    if(userDTO.address()!= null) {
      Address newAddress = addressMapper.toEntity(userDTO.address());
      newAddress.setUser(user);
      newAddress.setDefault(true);

      var existingAddresses = user.getAddresses();
      if(existingAddresses != null && !existingAddresses.isEmpty()) {
        existingAddresses.clear();
      }
      user.getAddresses().add(newAddress);
    }
  }
}
