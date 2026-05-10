package ee.ut.anup.userservice.controller;

import ee.ut.anup.userservice.dto.DriverProfileDTO;
import ee.ut.anup.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/driver-profiles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Driver Management", description = "Endpoints for managing driver profiles")
public class DriverProfileController {

    private final UserService userService;

    @Operation(summary = "List available driver profiles", description = "List available driver profiles for delivery assignment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Available drivers listed",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DriverProfileDTO.class))))
    })
    @GetMapping
    public List<DriverProfileDTO> getAvailableDrivers(@RequestParam(defaultValue = "true") boolean available) {
        log.info("Get available drivers request received, available={}", available);
        if (available) {
            List<DriverProfileDTO> drivers = userService.getAvailableDrivers();
            log.info("Get available drivers completed, count={}", drivers.size());
            return drivers;
        }
        log.info("Get available drivers skipped because available=false");
        return List.of();
    }
}
