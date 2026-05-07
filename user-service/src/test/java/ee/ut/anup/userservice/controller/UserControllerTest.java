package ee.ut.anup.userservice.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ee.ut.anup.userservice.dto.AddressDTO;
import ee.ut.anup.userservice.dto.UpdateUserDTO;
import ee.ut.anup.userservice.dto.UserDTO;
import ee.ut.anup.userservice.entity.User;
import ee.ut.anup.userservice.exception.GlobalExceptionHandler;
import ee.ut.anup.userservice.exception.ResourceNotFoundException;
import ee.ut.anup.userservice.exception.UserAlreadyExistsException;
import ee.ut.anup.userservice.service.UserService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---- getDummyRestaurants ----

    @Test
    void getDummyRestaurants_shouldReturnRestaurants_whenServiceAvailable() throws Exception {
        List<Map<String, String>> mockResponse = List.of(
                Map.of("id", "1", "name", "Resto A"),
                Map.of("id", "2", "name", "Resto B")
        );
        when(restTemplate.getForObject("http://restaurant-service/restaurants", Object.class))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/users/dummy-restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Resto A"))
                .andExpect(jsonPath("$[1].name").value("Resto B"));
    }

    @Test
    void getDummyRestaurants_shouldReturnServiceUnavailable_whenServiceFails() throws Exception {
        when(restTemplate.getForObject("http://restaurant-service/restaurants", Object.class))
                .thenThrow(new RuntimeException("Service down"));

        mockMvc.perform(get("/users/dummy-restaurants"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Restaurant service is currently unavailable (Dummy call)"));
    }

    // ---- registerUser ----

    @Test
    void registerUser_shouldReturnCreatedUser() throws Exception {
        UserDTO request = new UserDTO(
                "john@example.com", "secret123", "John Doe", "+3720000000",
                User.Role.CUSTOMER, null, null);
        UserDTO response = new UserDTO(
                "john@example.com", "secret123", "John Doe", "+3720000000",
                User.Role.CUSTOMER, User.Status.ACTIVE, null);
        when(userService.registerUser(eq(request))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).registerUser(eq(request));
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenEmailMissing() throws Exception {
        UserDTO invalidRequest = new UserDTO(
                "", "secret123", "John Doe", "+3720000000",
                User.Role.CUSTOMER, null, null);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenEmailInvalid() throws Exception {
        UserDTO invalidRequest = new UserDTO(
                "not-an-email", "secret123", "John Doe", "+3720000000",
                User.Role.CUSTOMER, null, null);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Invalid email format"));
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenPasswordMissing() throws Exception {
        UserDTO invalidRequest = new UserDTO(
                "john@example.com", "", "John Doe", "+3720000000",
                User.Role.CUSTOMER, null, null);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("Password is required"));
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenEmailAlreadyExists() throws Exception {
        UserDTO request = new UserDTO(
                "john@example.com", "secret123", "John Doe", "+3720000000",
                User.Role.CUSTOMER, null, null);
        when(userService.registerUser(eq(request)))
                .thenThrow(new UserAlreadyExistsException("User already exists with email: john@example.com"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("User already exists with email: john@example.com"));
    }

    // ---- getUserProfile ----

    @Test
    void getUserProfile_shouldReturnUser() throws Exception {
        UserDTO response = new UserDTO(
                "amy@example.com", "pwd", "Amy", "+372111111",
                User.Role.DRIVER, User.Status.ACTIVE, null);
        when(userService.getUserProfile(11L)).thenReturn(response);

        mockMvc.perform(get("/users/{id}", 11L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("amy@example.com"))
                .andExpect(jsonPath("$.fullName").value("Amy"))
                .andExpect(jsonPath("$.role").value("DRIVER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getUserProfile_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        when(userService.getUserProfile(99L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/users/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User not found with id: 99"));
    }

    // ---- updateUserProfile ----

    @Test
    void updateUserProfile_shouldReturnUpdatedUser() throws Exception {
        UpdateUserDTO request = new UpdateUserDTO(
                8L, "mia@example.com", "newpass", "Mia Updated", "+372222222",
                User.Role.CUSTOMER, null, null);
        UserDTO response = new UserDTO(
                "mia@example.com", "newpass", "Mia Updated", "+372222222",
                User.Role.CUSTOMER, User.Status.ACTIVE, null);
        when(userService.updateUserProfile(8L, request)).thenReturn(response);

        mockMvc.perform(put("/users/{id}", 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mia@example.com"))
                .andExpect(jsonPath("$.fullName").value("Mia Updated"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).updateUserProfile(8L, request);
    }

    @Test
    void updateUserProfile_shouldReturnBadRequest_whenEmailInvalid() throws Exception {
        UpdateUserDTO invalidRequest = new UpdateUserDTO(
                8L, "not-an-email", "newpass", "Mia", "+372222222",
                User.Role.CUSTOMER, null, null);

        mockMvc.perform(put("/users/{id}", 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Invalid email format"));
    }

    @Test
    void updateUserProfile_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        UpdateUserDTO request = new UpdateUserDTO(
                99L, "ghost@example.com", "pass", "Ghost", "+000",
                User.Role.CUSTOMER, null, null);
        when(userService.updateUserProfile(99L, request))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(put("/users/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User not found with id: 99"));
    }

    // ---- getUserAddresses ----

    @Test
    void getUserAddresses_shouldReturnAddressList() throws Exception {
        List<AddressDTO> addresses = List.of(
                new AddressDTO("Street 1", "Tartu", "50001", "Home", true),
                new AddressDTO("Street 2", "Tallinn", "10111", "Work", false));
        when(userService.getUserAddresses(5L)).thenReturn(addresses);

        mockMvc.perform(get("/users/{id}/addresses", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].street").value("Street 1"))
                .andExpect(jsonPath("$[0].city").value("Tartu"))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[1].label").value("Work"))
                .andExpect(jsonPath("$[1].isDefault").value(false));
    }

    @Test
    void getUserAddresses_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        when(userService.getUserAddresses(99L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/users/{id}/addresses", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User not found with id: 99"));
    }

    // ---- addUserAddress ----

    @Test
    void addUserAddress_shouldReturnCreatedAddress() throws Exception {
        AddressDTO request = new AddressDTO("Narva mnt 1", "Tartu", "51009", "Dorm", true);
        AddressDTO response = new AddressDTO("Narva mnt 1", "Tartu", "51009", "Dorm", true);
        when(userService.addUserAddress(3L, request)).thenReturn(response);

        mockMvc.perform(post("/users/{id}/addresses", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.street").value("Narva mnt 1"))
                .andExpect(jsonPath("$.city").value("Tartu"))
                .andExpect(jsonPath("$.postalCode").value("51009"))
                .andExpect(jsonPath("$.label").value("Dorm"))
                .andExpect(jsonPath("$.isDefault").value(true));

        verify(userService).addUserAddress(3L, request);
    }

    @Test
    void addUserAddress_shouldReturnBadRequest_whenStreetMissing() throws Exception {
        AddressDTO invalidRequest = new AddressDTO("", "Tartu", "51009", "Dorm", true);

        mockMvc.perform(post("/users/{id}/addresses", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.street").value("Street is required"));
    }

    @Test
    void addUserAddress_shouldReturnBadRequest_whenCityMissing() throws Exception {
        AddressDTO invalidRequest = new AddressDTO("Narva mnt 1", "", "51009", "Dorm", false);

        mockMvc.perform(post("/users/{id}/addresses", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.city").value("City is required"));
    }

    @Test
    void addUserAddress_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        AddressDTO request = new AddressDTO("Narva mnt 1", "Tartu", "51009", "Dorm", false);
        when(userService.addUserAddress(99L, request))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(post("/users/{id}/addresses", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User not found with id: 99"));
    }
}
