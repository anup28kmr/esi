package ee.ut.anup.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.ut.anup.orderservice.dto.OrderItemResponse;
import ee.ut.anup.orderservice.dto.OrderResponse;
import ee.ut.anup.orderservice.dto.PlaceOrderRequest;
import ee.ut.anup.orderservice.dto.StatusUpdateRequest;
import ee.ut.anup.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();

        orderResponse = new OrderResponse(1L, 1L, "rest1", "PENDING", new BigDecimal("100.00"), Collections.emptyList(), true, true);
    }

    @Test
    void placeOrder_ShouldReturnCreated() throws Exception {
        PlaceOrderRequest request = new PlaceOrderRequest("rest1", Collections.emptyList());
        when(orderService.placeOrder(eq(1L), any(PlaceOrderRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/orders")
                        .header("X-User-Id", 1L)
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
        // Mockito.doNothing() for void methods
        mockMvc.perform(delete("/orders/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateOrderStatus_ShouldReturnUpdatedOrder() throws Exception {
        StatusUpdateRequest request = new StatusUpdateRequest("ACCEPTED");
        OrderResponse updatedResponse = new OrderResponse(1L, 1L, "rest1", "ACCEPTED", new BigDecimal("100.00"), Collections.emptyList(), true, true);
        when(orderService.updateOrderStatus(eq(1L), any(StatusUpdateRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(patch("/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void getOrdersByCustomer_ShouldReturnList() throws Exception {
        when(orderService.getOrdersByCustomer(1L)).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/orders").param("customerId", "1"))
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
