package ee.ut.anup.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.ut.anup.orderservice.dto.OrderItemResponse;
import ee.ut.anup.orderservice.dto.OrderResponse;
import ee.ut.anup.orderservice.dto.PlaceOrderRequest;
import ee.ut.anup.orderservice.dto.StatusUpdateRequest;
import ee.ut.anup.orderservice.security.AuthenticatedUser;
import ee.ut.anup.orderservice.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();

        AuthenticatedUser principal = new AuthenticatedUser(CUSTOMER_ID, "Customer", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_Customer"))));

        orderResponse = new OrderResponse(1L, CUSTOMER_ID, "rest1", "PENDING",
                new BigDecimal("100.00"), Collections.emptyList(), true, true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void placeOrder_ShouldReturnCreated() throws Exception {
        PlaceOrderRequest request = new PlaceOrderRequest("rest1", Collections.emptyList());
        when(orderService.placeOrder(eq(CUSTOMER_ID), any(PlaceOrderRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1L))
                .andExpect(jsonPath("$.restaurantId").value("rest1"));
    }

    @Test
    void getOrder_ShouldReturnOrder() throws Exception {
        when(orderService.getOrder(1L)).thenReturn(orderResponse);

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L));
    }

    @Test
    void cancelOrder_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/orders/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateOrderStatus_ShouldReturnUpdatedOrder() throws Exception {
        StatusUpdateRequest request = new StatusUpdateRequest("ACCEPTED");
        OrderResponse updatedResponse = new OrderResponse(1L, CUSTOMER_ID, "rest1", "ACCEPTED",
                new BigDecimal("100.00"), Collections.emptyList(), true, true);
        when(orderService.updateOrderStatus(eq(1L), any(StatusUpdateRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(patch("/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void getOrdersByCustomer_ShouldReturnList() throws Exception {
        when(orderService.getOrdersByCustomer(CUSTOMER_ID)).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/orders").param("customerId", CUSTOMER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(1L));
    }

    @Test
    void getOrderItems_ShouldReturnList() throws Exception {
        OrderItemResponse itemResponse = new OrderItemResponse("m1", "Item 1", new BigDecimal("10.00"), 2);
        when(orderService.getOrderItems(1L)).thenReturn(List.of(itemResponse));

        mockMvc.perform(get("/orders/1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].menuItemId").value("m1"));
    }
}
