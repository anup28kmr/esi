package ee.ut.quickbite.userservice.controller;

import ee.ut.quickbite.userservice.dto.AddressDTO;
import ee.ut.quickbite.userservice.dto.ErrorResponseDTO;
import ee.ut.quickbite.userservice.dto.UpdateUserDTO;
import ee.ut.quickbite.userservice.dto.UserDTO;
import ee.ut.quickbite.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "Endpoints for managing users and their addresses")
public class UserController {

  private final UserService userService;
  private final RestTemplate restTemplate;

  @Operation(
      summary = "Get dummy restaurants from restaurant-service",
      description = "Call the future restaurant-service to get dummy data")
  @GetMapping("/dummy-restaurants")
  public ResponseEntity<Object> getDummyRestaurants() {
    String restaurantServiceUrl = "http://restaurant-service/restaurants";
    log.info("Dummy restaurants call started, target={}", restaurantServiceUrl);
    try {
      Object restaurants = restTemplate.getForObject(restaurantServiceUrl, Object.class);
      log.info("Dummy restaurants call succeeded");
      return ResponseEntity.ok(restaurants);
    } catch (Exception e) {
      log.warn("Dummy restaurants call failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body("Restaurant service is currently unavailable (Dummy call)");
    }
  }

  @Operation(
      summary = "Register a new user account",
      description = "Register a new user (customer, driver, restaurant owner, or admin)")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "User created",
        content = @Content(schema = @Schema(implementation = UserDTO.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserDTO registerUser(@Valid @RequestBody UserDTO userDTO) {
    log.info("Register user request received, email={}", userDTO.email());
    UserDTO createdUser = userService.registerUser(userDTO);
    log.info("Register user completed, email={}, role={}", createdUser.email(), createdUser.role());
    return createdUser;
  }

  @Operation(
      summary = "Get a user profile",
      description = "Retrieve profile information for a specific user")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "User profile found",
        content = @Content(schema = @Schema(implementation = UserDTO.class))),
    @ApiResponse(
        responseCode = "404",
        description = "User not found",
        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  @GetMapping("/{id}")
  public UserDTO getUserProfile(@PathVariable UUID id) {
    log.info("Get user profile request received, userId={}", id);
    UserDTO user = userService.getUserProfile(id);
    log.info("Get user profile completed, userId={}", id);
    return user;
  }

  @Operation(
      summary = "Update a user profile",
      description = "Update profile information for a specific user")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "User profile updated",
        content = @Content(schema = @Schema(implementation = UserDTO.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
    @ApiResponse(
        responseCode = "404",
        description = "User not found",
        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  @PutMapping("/{id}")
  public UserDTO updateUserProfile(@PathVariable UUID id, @Valid @RequestBody UpdateUserDTO userDTO) {
    log.info("Update user profile request received, userId={}", id);
    UserDTO updatedUser = userService.updateUserProfile(id, userDTO);
    log.info("Update user profile completed, userId={}", id);
    return updatedUser;
  }

  @Operation(
      summary = "List saved addresses",
      description = "List the saved addresses of a given user")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Saved addresses listed",
        content =
            @Content(array = @ArraySchema(schema = @Schema(implementation = AddressDTO.class)))),
    @ApiResponse(
        responseCode = "404",
        description = "User not found",
        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  @GetMapping("/{id}/addresses")
  public List<AddressDTO> getUserAddresses(@PathVariable UUID id) {
    log.info("Get user addresses request received, userId={}", id);
    List<AddressDTO> addresses = userService.getUserAddresses(id);
    log.info("Get user addresses completed, userId={}, count={}", id, addresses.size());
    return addresses;
  }

  @Operation(
      summary = "Add a new saved address",
      description = "Add a new saved address for a given user")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Address added",
        content = @Content(schema = @Schema(implementation = AddressDTO.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
    @ApiResponse(
        responseCode = "404",
        description = "User not found",
        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  @PostMapping("/{id}/addresses")
  @ResponseStatus(HttpStatus.CREATED)
  public AddressDTO addUserAddress(
      @PathVariable UUID id, @Valid @RequestBody AddressDTO addressDTO) {
    log.info("Add user address request received, userId={}", id);
    AddressDTO savedAddress = userService.addUserAddress(id, addressDTO);
    log.info("Add user address completed, userId={}", id);
    return savedAddress;
  }
}
