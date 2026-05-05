package ee.ut.anup.userservice.controller;

import ee.ut.anup.userservice.dto.AddressDTO;
import ee.ut.anup.userservice.dto.ErrorResponseDTO;
import ee.ut.anup.userservice.dto.UserDTO;
import ee.ut.anup.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
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
    try {
      // This is a dummy call to a service that doesn't exist yet
      Object restaurants = restTemplate.getForObject(restaurantServiceUrl, Object.class);
      return ResponseEntity.ok(restaurants);
    } catch (Exception e) {
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
    return userService.registerUser(userDTO);
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
  public UserDTO getUserProfile(@PathVariable Long id) {
    return userService.getUserProfile(id);
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
  public UserDTO updateUserProfile(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
    return userService.updateUserProfile(id, userDTO);
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
  public List<AddressDTO> getUserAddresses(@PathVariable Long id) {
    return userService.getUserAddresses(id);
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
      @PathVariable Long id, @Valid @RequestBody AddressDTO addressDTO) {
    return userService.addUserAddress(id, addressDTO);
  }
}
