package ee.ut.quickbite.userservice.controller;

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
import ee.ut.quickbite.userservice.dto.AddressDTO;
import ee.ut.quickbite.userservice.dto.UpdateUserDTO;
import ee.ut.quickbite.userservice.dto.UserDTO;
import ee.ut.quickbite.userservice.entity.User;
import ee.ut.quickbite.userservice.exception.GlobalExceptionHandler;
import ee.ut.quickbite.userservice.exception.ResourceNotFoundException;
import ee.ut.quickbite.userservice.exception.UserAlreadyExistsException;
import ee.ut.quickbite.userservice.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    // Stable UUIDs per persona so test failures are easy to trace.
    private static final UUID CREATED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID AMY_ID          = UUID.fromString("0000000a-0000-0000-0000-000000000011");
    private static final UUID GHOST_ID        = UUID.fromString("00000099-0000-0000-0000-000000000099");
    private static final UUID MIA_ID          = UUID.fromString("00000008-0000-0000-0000-000000000008");
    private static final UUID ADDR_USER_ID    = UUID.fromString("00000005-0000-0000-0000-000000000005");
    private static final UUID ADD_ADDR_USER_ID = UUID.fromString("00000003-0000-0000-0000-000000000003");

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
                null,
                "john@example.com", "secret123", "John Doe", "+3720000000",
                User.Role.CUSTOMER, null, null);
        UserDTO response = new UserDTO(
                CREATED_USER_ID,
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
                null,
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
                null,
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
                null,
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
                null,
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
                AMY_ID,
                "amy@example.com", "pwd", "Amy", "+372111111",
                User.Role.DRIVER, User.Status.ACTIVE, null);
        when(userService.getUserProfile(AMY_ID)).thenReturn(response);

        mockMvc.perform(get("/users/{id}", AMY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("amy@example.com"))
                .andExpect(jsonPath("$.fullName").value("Amy"))
                .andExpect(jsonPath("$.role").value("DRIVER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getUserProfile_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        when(userService.getUserProfile(GHOST_ID))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + GHOST_ID));

        mockMvc.perform(get("/users/{id}", GHOST_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User not found with id: " + GHOST_ID));
    }

    // ---- updateUserProfile ----

    @Test
    void updateUserProfile_shouldReturnUpdatedUser() throws Exception {
        UpdateUserDTO request = new UpdateUserDTO(
                MIA_ID, "mia@example.com", "newpass", "Mia Updated", "+372222222",
                User.Role.CUSTOMER, null, null);
        UserDTO response = new UserDTO(
                MIA_ID,
                "mia@example.com", "newpass", "Mia Updated", "+372222222",
                User.Role.CUSTOMER, User.Status.ACTIVE, null);
        when(userService.updateUserProfile(MIA_ID, request)).thenReturn(response);

        mockMvc.perform(put("/users/{id}", MIA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mia@example.com"))
                .andExpect(jsonPath("$.fullName").value("Mia Updated"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).updateUserProfile(MIA_ID, request);
    }

    @Test
    void updateUserProfile_shouldReturnBadRequest_whenEmailInvalid() throws Exception {
        UpdateUserDTO invalidRequest = new UpdateUserDTO(
                MIA_ID, "not-an-email", "newpass", "Mia", "+372222222",
                User.Role.CUSTOMER, null, null);

        mockMvc.perform(put("/users/{id}", MIA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Invalid email format"));
    }

    @Test
    void updateUserProfile_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        UpdateUserDTO request = new UpdateUserDTO(
                GHOST_ID, "ghost@example.com", "pass", "Ghost", "+000",
                User.Role.CUSTOMER, null, null);
        when(userService.updateUserProfile(GHOST_ID, request))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + GHOST_ID));

        mockMvc.perform(put("/users/{id}", GHOST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User not found with id: " + GHOST_ID));
    }

    // ---- getUserAddresses ----

    @Test
    void getUserAddresses_shouldReturnAddressList() throws Exception {
        List<AddressDTO> addresses = List.of(
                new AddressDTO("Street 1", "Tartu", "50001", "Home", true),
                new AddressDTO("Street 2", "Tallinn", "10111", "Work", false));
        when(userService.getUserAddresses(ADDR_USER_ID)).thenReturn(addresses);

        mockMvc.perform(get("/users/{id}/addresses", ADDR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].street").value("Street 1"))
                .andExpect(jsonPath("$[0].city").value("Tartu"))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[1].label").value("Work"))
                .andExpect(jsonPath("$[1].isDefault").value(false));
    }

    @Test
    void getUserAddresses_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        when(userService.getUserAddresses(GHOST_ID))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + GHOST_ID));

        mockMvc.perform(get("/users/{id}/addresses", GHOST_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User not found with id: " + GHOST_ID));
    }

    // ---- addUserAddress ----

    @Test
    void addUserAddress_shouldReturnCreatedAddress() throws Exception {
        AddressDTO request = new AddressDTO("Narva mnt 1", "Tartu", "51009", "Dorm", true);
        AddressDTO response = new AddressDTO("Narva mnt 1", "Tartu", "51009", "Dorm", true);
        when(userService.addUserAddress(ADD_ADDR_USER_ID, request)).thenReturn(response);

        mockMvc.perform(post("/users/{id}/addresses", ADD_ADDR_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.street").value("Narva mnt 1"))
                .andExpect(jsonPath("$.city").value("Tartu"))
                .andExpect(jsonPath("$.postalCode").value("51009"))
                .andExpect(jsonPath("$.label").value("Dorm"))
                .andExpect(jsonPath("$.isDefault").value(true));

        verify(userService).addUserAddress(ADD_ADDR_USER_ID, request);
    }

    @Test
    void addUserAddress_shouldReturnBadRequest_whenStreetMissing() throws Exception {
        AddressDTO invalidRequest = new AddressDTO("", "Tartu", "51009", "Dorm", true);

        mockMvc.perform(post("/users/{id}/addresses", ADD_ADDR_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.street").value("Street is required"));
    }

    @Test
    void addUserAddress_shouldReturnBadRequest_whenCityMissing() throws Exception {
        AddressDTO invalidRequest = new AddressDTO("Narva mnt 1", "", "51009", "Dorm", false);

        mockMvc.perform(post("/users/{id}/addresses", ADD_ADDR_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.city").value("City is required"));
    }

    @Test
    void addUserAddress_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        AddressDTO request = new AddressDTO("Narva mnt 1", "Tartu", "51009", "Dorm", false);
        when(userService.addUserAddress(GHOST_ID, request))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + GHOST_ID));

        mockMvc.perform(post("/users/{id}/addresses", GHOST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User not found with id: " + GHOST_ID));
    }
}
