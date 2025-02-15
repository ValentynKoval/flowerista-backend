package ua.flowerista.shop.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import ua.flowerista.shop.dto.OrderDto;
import ua.flowerista.shop.mappers.OrderMapper;
import ua.flowerista.shop.models.Order;
import ua.flowerista.shop.models.OrderStatus;
import ua.flowerista.shop.models.User;
import ua.flowerista.shop.services.OrderService;
import ua.flowerista.shop.services.validators.OrderValidator;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
@CrossOrigin
@Tag(name = "Order controller")
public class OrderController {
    private final OrderService orderService;
    private final OrderValidator orderValidator;
    private final OrderMapper orderMapper;

    @Operation(summary = "Create new order", description = "Returns bad request if order already exist, and accepted if everything fine")
    @ApiResponses(value = {@ApiResponse(responseCode = "409", description = "If order already exist"),
            @ApiResponse(responseCode = "202", description = "Data was accepted")})
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderDto order, Principal connectedUser) {
        List<String> errors = orderValidator.validateOrder(order);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(errors.toString());
        }
        if (orderService.isOrderExists(order.getId())) {
            return ResponseEntity.status(409).body("Order already exists");
        }
        order.setUserId(getPrincipalUser(connectedUser).getId());
        order.setStatus(OrderStatus.PLACED);
        order = orderMapper.toDto(orderService.createOrder(orderMapper.toEntity(order)));
        return ResponseEntity.accepted().body(order);
    }

    @Operation(summary = "Get order by id", description = "Returns order by id")
    @ApiResponses(value = {@ApiResponse(responseCode = "400", description = "If order not found"),
            @ApiResponse(responseCode = "403", description = "If user is not allowed to see this order"),
            @ApiResponse(responseCode = "200", description = "Return order by id")})
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Integer id, Principal connectedUser) {
        Optional<Order> order = orderService.getOrder(id);
        if (order.isEmpty()) {
            return ResponseEntity.badRequest().body("Order not found");
        }
        Integer requestUserId = getPrincipalUser(connectedUser).getId();
        if (!order.get().getUser().getId().equals(requestUserId)) {
            return ResponseEntity.status(403).body("You are not allowed to see this order");
        }
        return ResponseEntity.ok(orderMapper.toDto(order.get()));
    }

    @Operation(summary = "Update order by id", description = "Update order by id")
    @ApiResponses(value = {@ApiResponse(responseCode = "403", description = "If user is not allowed to update this order"),
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "If order is not valid"),
            @ApiResponse(responseCode = "202", description = "Data was accepted and order was created")})
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable Integer id, @RequestBody OrderDto order, Principal connectedUser) {
        List<String> errors = orderValidator.validateOrder(order);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(errors.toString());
        }
        if (!orderService.isOrderExists(id)) {
            order.setUserId(getPrincipalUser(connectedUser).getId());
            order.setStatus(OrderStatus.PLACED);
            return ResponseEntity.accepted().body(orderService.createOrder(orderMapper.toEntity(order)));
        }
        Integer requestUserId = getPrincipalUser(connectedUser).getId();
        Optional<Order> savedOrder = orderService.getOrder(id);
        if (!savedOrder.get().getUser().getId().equals(requestUserId)) {
            return ResponseEntity.status(403).body("You are not allowed to update this order");
        }
        order.setUserId(getPrincipalUser(connectedUser).getId());
        order.setId(id);
        order.setStatus(OrderStatus.PLACED);
        order = orderMapper.toDto(orderService.createOrder(orderMapper.toEntity(order)));
        return ResponseEntity.accepted().body(order);
    }

    @Operation(summary = "Get orders history", description = "The history of all the user's orders is returned.")
    @ApiResponses(value = {@ApiResponse(responseCode = "400", description = "If no order is found"),
            @ApiResponse(responseCode = "200", description = "Return orders history")})
    @GetMapping("/history")
    public ResponseEntity<?> getAllOrdersByUser(Principal connectedUser) {
        Integer requestUserId = getPrincipalUser(connectedUser).getId();
        List<Order> orderList = orderService.getOrdersByUserId(requestUserId);
        if (orderList.isEmpty()) {
            return ResponseEntity.badRequest().body("No orders were found");
        }
        return ResponseEntity.ok(orderList.stream().map(orderMapper::toDto).collect(Collectors.toList()));
    }

    private static User getPrincipalUser(Principal connectedUser) {
        return (User) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();
    }
}
