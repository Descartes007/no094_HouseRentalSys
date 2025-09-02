package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Order;
import org.example.springboot.service.OrderService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Tag(name = "订单管理接口")
@RestController
@RequestMapping("/orders")
public class OrderController {
    @Resource
    private OrderService orderService;
    
    @Operation(summary = "创建订单")
    @PostMapping
    public Result<?> createOrder(@RequestBody Order order) {
        return Result.success(orderService.createOrder(order));
    }
    
    @Operation(summary = "支付订单")
    @PutMapping("/{id}/pay")
    public Result<?> payOrder(@PathVariable Long id, @RequestParam String paymentMethod) {
        return Result.success(orderService.payOrder(id, paymentMethod));
    }
    
    @Operation(summary = "确认订单（房东确认）")
    @PutMapping("/{id}/confirm")
    public Result<?> confirmOrder(@PathVariable Long id) {
        return Result.success(orderService.confirmOrder(id));
    }
    
    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<?> cancelOrder(@PathVariable Long id) {
        return Result.success(orderService.cancelOrder(id));
    }
    
    @Operation(summary = "申请退款")
    @PostMapping("/{id}/refund")
    public Result<?> refundOrder(@PathVariable Long id) {
        return Result.success(orderService.refundOrder(id));
    }
    
    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public Result<?> getOrderDetail(@PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(id));
    }
    
    @Operation(summary = "租客获取自己的订单")
    @GetMapping("/tenant")
    public Result<?> getTenantOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.getTenantOrders(orderNo, status, currentPage, size));
    }
    
    @Operation(summary = "房东获取自己的订单")
    @GetMapping("/landlord")
    public Result<?> getLandlordOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.getLandlordOrders(orderNo, status, currentPage, size));
    }
    
    @Operation(summary = "管理员获取所有订单")
    @GetMapping("/admin")
    public Result<?> getAllOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String tenantUsername,
            @RequestParam(required = false) String landlordUsername,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.getAllOrders(orderNo, status, tenantUsername, landlordUsername, currentPage, size));
    }
} 