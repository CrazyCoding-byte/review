// order/src/main/java/com/demo/order/controller/OrderController.java
package com.yzx.crazycodingbyteorder.controller;

import com.yzx.crazycodingbytecommon.entity.Result;
import com.yzx.crazycodingbyteorder.dto.CreateOrderRequest;
import com.yzx.crazycodingbyteorder.service.OrderService;
import com.yzx.crazycodingbyteorder.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单相关接口")
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping("/create")
    @Operation(summary = "创建订单")
    public Result<OrderVO> createOrder(@Validated @RequestBody CreateOrderRequest request) {
        log.info("创建订单请求，用户ID：{}，商品ID：{}，数量：{}",
                request.getUserId(), request.getProductId(), request.getQuantity());
        
        return orderService.createOrder(request);
    }
    
    @GetMapping("/{orderNo}")
    @Operation(summary = "查询订单详情")
    public Result<OrderVO> getOrder(@PathVariable String orderNo) {
        log.info("查询订单详情，订单号：{}", orderNo);
        return orderService.getOrderByNo(orderNo);
    }
    
    @PostMapping("/{orderNo}/cancel")
    @Operation(summary = "取消订单")
    public Result<Void> cancelOrder(@PathVariable String orderNo, 
                                   @RequestParam Long userId) {
        log.info("取消订单请求，订单号：{}，用户ID：{}", orderNo, userId);
        return orderService.cancelOrder(orderNo, userId);
    }
    
    @PostMapping("/{orderNo}/pay/callback")
    @Operation(summary = "支付回调（模拟）")
    public Result<Void> payCallback(@PathVariable String orderNo,
                                   @RequestParam String payNo,
                                   @RequestParam String amount) {
        log.info("支付回调请求，订单号：{}，支付流水号：{}，金额：{}", orderNo, payNo, amount);
        return orderService.handlePayCallback(orderNo, payNo, new java.math.BigDecimal(amount));
    }
}