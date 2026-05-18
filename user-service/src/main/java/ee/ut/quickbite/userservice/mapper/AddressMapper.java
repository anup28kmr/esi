package ee.ut.quickbite.userservice.mapper;

import ee.ut.quickbite.userservice.dto.AddressDTO;
import ee.ut.quickbite.userservice.entity.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressDTO toDto(Address address) {
        return new AddressDTO(
                address.getStreet(),
                address.getCity(),
                address.getPostalCode(),
                address.getLabel(),
                address.isDefault());
    }

    public Address toEntity(AddressDTO addressDTO) {
        Address address = new Address();
        address.setStreet(addressDTO.street());
        address.setCity(addressDTO.city());
        address.setPostalCode(addressDTO.postalCode());
        address.setLabel(addressDTO.label());
        address.setDefault(addressDTO.isDefault());
        return address;
    }
}
