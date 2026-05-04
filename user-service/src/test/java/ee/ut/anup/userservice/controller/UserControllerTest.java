package ee.ut.anup.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ee.ut.anup.userservice.dto.AddressDTO;
import ee.ut.anup.userservice.dto.UserDTO;
import ee.ut.anup.userservice.entity.User;
import ee.ut.anup.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    void registerUser_shouldReturnCreatedUser() throws Exception {
        UserDTO request = new UserDTO(
                null,
                "john@example.com",
                "secret123",
                "John Doe",
                "+3720000000",
                User.Role.CUSTOMER,
                null,
                null);
        UserDTO response = new UserDTO(
                1L,
                "john@example.com",
                "secret123",
                "John Doe",
                "+3720000000",
                User.Role.CUSTOMER,
                User.Status.ACTIVE,
                LocalDateTime.of(2026, 5, 4, 10, 0));
        when(userService.registerUser(eq(request))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).registerUser(eq(request));
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenEmailMissing() throws Exception {
        UserDTO invalidRequest = new UserDTO(
                null,
                "",
                "secret123",
                "John Doe",
                "+3720000000",
                User.Role.CUSTOMER,
                null,
                null);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserProfile_shouldReturnUser() throws Exception {
        UserDTO response = new UserDTO(
                11L,
                "amy@example.com",
                "pwd",
                "Amy",
                "+372111111",
                User.Role.DRIVER,
                User.Status.ACTIVE,
                LocalDateTime.of(2026, 5, 4, 11, 0));
        when(userService.getUserProfile(11L)).thenReturn(response);

        mockMvc.perform(get("/users/{id}", 11L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(11))
                .andExpect(jsonPath("$.email").value("amy@example.com"))
                .andExpect(jsonPath("$.role").value("DRIVER"));
    }

    @Test
    void updateUserProfile_shouldReturnUpdatedUser() throws Exception {
        UserDTO request = new UserDTO(
                null,
                "mia@example.com",
                "newpass",
                "Mia Updated",
                "+372222222",
                User.Role.CUSTOMER,
                null,
                null);
        UserDTO response = new UserDTO(
                8L,
                "mia@example.com",
                "newpass",
                "Mia Updated",
                "+372222222",
                User.Role.CUSTOMER,
                User.Status.ACTIVE,
                LocalDateTime.of(2026, 5, 4, 12, 0));
        when(userService.updateUserProfile(8L, request)).thenReturn(response);

        mockMvc.perform(put("/users/{id}", 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(8))
                .andExpect(jsonPath("$.fullName").value("Mia Updated"));

        verify(userService).updateUserProfile(8L, request);
    }

    @Test
    void getUserAddresses_shouldReturnAddressList() throws Exception {
        List<AddressDTO> addresses = List.of(
                new AddressDTO(100L, "Street 1", "Tartu", "50001", "Home", true),
                new AddressDTO(101L, "Street 2", "Tallinn", "10111", "Work", false));
        when(userService.getUserAddresses(5L)).thenReturn(addresses);

        mockMvc.perform(get("/users/{id}/addresses", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].addressId").value(100))
                .andExpect(jsonPath("$[0].city").value("Tartu"))
                .andExpect(jsonPath("$[1].label").value("Work"));
    }

    @Test
    void addUserAddress_shouldReturnCreatedAddress() throws Exception {
        AddressDTO request = new AddressDTO(null, "Narva mnt 1", "Tartu", "51009", "Dorm", true);
        AddressDTO response = new AddressDTO(201L, "Narva mnt 1", "Tartu", "51009", "Dorm", true);
        when(userService.addUserAddress(3L, request)).thenReturn(response);

        mockMvc.perform(post("/users/{id}/addresses", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addressId").value(201))
                .andExpect(jsonPath("$.street").value("Narva mnt 1"))
                .andExpect(jsonPath("$.isDefault").value(true));

        verify(userService).addUserAddress(3L, request);
    }
}
