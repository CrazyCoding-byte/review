// order/src/main/java/com/demo/order/service/OrderService.java
package com.yzx.crazycodingbyteorder.service;


import com.yzx.crazycodingbytecommon.entity.Result;
import com.yzx.crazycodingbyteorder.dto.CreateOrderRequest;
import com.yzx.crazycodingbyteorder.vo.OrderVO;

import java.math.BigDecimal;

public interface OrderService {
    
    /**
     * 创建订单
     */
    Result<OrderVO> createOrder(CreateOrderRequest request);
    
    /**
     * 根据订单号获取订单详情
     */
    Result<OrderVO> getOrderByNo(String orderNo);
    
    /**
     * 取消订单
     */
    Result<Void> cancelOrder(String orderNo, Long userId);
    
    /**
     * 处理库存锁定结果
     */
    void handleInventoryLockResult(String orderNo, boolean lockSuccess, String message);
    
    /**
     * 处理支付回调
     */
    Result<Void> handlePayCallback(String orderNo, String payNo, BigDecimal amount);
}