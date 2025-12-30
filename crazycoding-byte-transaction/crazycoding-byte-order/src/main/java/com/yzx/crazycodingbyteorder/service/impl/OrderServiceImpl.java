// order/src/main/java/com/demo/order/service/impl/OrderServiceImpl.java
package com.yzx.crazycodingbyteorder.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.yzx.crazycodingbytecommon.entity.BusinessException;
import com.yzx.crazycodingbytecommon.entity.Result;
import com.yzx.crazycodingbyteorder.constant.OrderConstant;
import com.yzx.crazycodingbyteorder.dto.CreateOrderRequest;
import com.yzx.crazycodingbyteorder.dto.InventoryLockDTO;
import com.yzx.crazycodingbyteorder.entity.Order;
import com.yzx.crazycodingbyteorder.entity.OrderDetail;
import com.yzx.crazycodingbyteorder.entity.OrderOperationLog;
import com.yzx.crazycodingbyteorder.enums.OrderOperationTypeEnum;
import com.yzx.crazycodingbyteorder.enums.OrderStatusEnum;
import com.yzx.crazycodingbyteorder.mapper.OrderDetailMapper;
import com.yzx.crazycodingbyteorder.mapper.OrderMapper;
import com.yzx.crazycodingbyteorder.mapper.OrderOperationLogMapper;
import com.yzx.crazycodingbyteorder.service.OrderService;
import com.yzx.crazycodingbyteorder.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;
    private final RocketMQTemplate rocketMQTemplate;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderVO> createOrder(CreateOrderRequest request) {
        log.info("创建订单开始，请求参数：{}", request);
        
        try {
            // 1. 生成订单号
            String orderNo = generateOrderNo();
            
            // 2. 计算订单总金额
            BigDecimal totalAmount = request.getProductPrice()
                    .multiply(BigDecimal.valueOf(request.getQuantity()));
            
            // 3. 创建订单记录
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(request.getUserId());
            order.setProductId(request.getProductId());
            order.setQuantity(request.getQuantity());
            order.setTotalAmount(totalAmount);
            order.setStatus(OrderStatusEnum.WAIT_PAY.getCode());
            
            int orderInsertResult = orderMapper.insert(order);
            if (orderInsertResult <= 0) {
                throw new BusinessException("创建订单失败");
            }
            
            // 4. 创建订单详情
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderNo(orderNo);
            orderDetail.setProductId(request.getProductId());
            orderDetail.setProductName(request.getProductName());
            orderDetail.setProductPrice(request.getProductPrice());
            orderDetail.setQuantity(request.getQuantity());
            orderDetail.setTotalPrice(totalAmount);
            orderDetail.setProductImage(request.getProductImage());
            orderDetail.setProductSpec(request.getProductSpec());
            
            int detailInsertResult = orderDetailMapper.insert(orderDetail);
            if (detailInsertResult <= 0) {
                throw new BusinessException("创建订单详情失败");
            }
            
            // 5. 记录操作日志
            saveOperationLog(orderNo, OrderOperationTypeEnum.CREATE,
                    null, OrderStatusEnum.WAIT_PAY.getCode(), 
                    "用户创建订单", request.getUserId(), "用户");
            
            // 6. 发送库存锁定消息（事务消息）
            InventoryLockDTO lockDTO = new InventoryLockDTO();
            lockDTO.setOrderNo(orderNo);
            lockDTO.setProductId(request.getProductId());
            lockDTO.setQuantity(request.getQuantity());
            lockDTO.setUserId(request.getUserId());
            
            Message<InventoryLockDTO> message = MessageBuilder.withPayload(lockDTO)
                    .setHeader("ORDER_NO", orderNo)
                    .build();
            
            // 发送半消息到库存服务
            rocketMQTemplate.sendMessageInTransaction(
                    OrderConstant.TOPIC_INVENTORY_LOCK,
                    OrderConstant.TAG_INVENTORY_LOCKED,
                    message,
                    orderNo
            );
            
            log.info("订单创建成功，订单号：{}，已发送库存锁定消息", orderNo);
            
            // 7. 返回订单信息
            OrderVO orderVO = convertToVO(order);
            return Result.success(orderVO);
            
        } catch (Exception e) {
            log.error("创建订单失败", e);
            throw new BusinessException("创建订单失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<OrderVO> getOrderByNo(String orderNo) {
        log.info("查询订单，订单号：{}", orderNo);
        
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }
        
        OrderVO orderVO = convertToVO(order);
        return Result.success(orderVO);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelOrder(String orderNo, Long userId) {
        log.info("取消订单，订单号：{}，用户ID：{}", orderNo, userId);
        
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }
        
        // 校验订单是否属于当前用户
        if (!order.getUserId().equals(userId)) {
            return Result.error("无权操作该订单");
        }
        
        // 只能取消待支付的订单
        if (order.getStatus() != OrderStatusEnum.WAIT_PAY.getCode()) {
            return Result.error("当前订单状态不允许取消");
        }
        
        // 更新订单状态
        int updateResult = orderMapper.updateToCanceled(orderNo, OrderStatusEnum.CANCELED.getCode());
        if (updateResult <= 0) {
            return Result.error("取消订单失败");
        }
        
        // 记录操作日志
        saveOperationLog(orderNo, OrderOperationTypeEnum.CANCEL, 
                OrderStatusEnum.WAIT_PAY.getCode(), OrderStatusEnum.CANCELED.getCode(), 
                "用户取消订单", userId, "用户");
        
        // 发送库存释放消息
        InventoryLockDTO lockDTO = new InventoryLockDTO();
        lockDTO.setOrderNo(orderNo);
        lockDTO.setProductId(order.getProductId());
        lockDTO.setQuantity(order.getQuantity());
        lockDTO.setUserId(userId);
        
        Message<InventoryLockDTO> message = MessageBuilder.withPayload(lockDTO)
                .setHeader("ORDER_NO", orderNo)
                .build();
        
        rocketMQTemplate.syncSend(OrderConstant.TOPIC_ORDER_CANCEL, message);
        
        log.info("订单取消成功，订单号：{}", orderNo);
        return Result.success();
    }
    
    @Override
    public void handleInventoryLockResult(String orderNo, boolean lockSuccess, String message) {
        log.info("处理库存锁定结果，订单号：{}，锁定结果：{}，消息：{}", 
                orderNo, lockSuccess, message);
        
        try {
            Order order = orderMapper.selectByOrderNo(orderNo);
            if (order == null) {
                log.error("订单不存在，订单号：{}", orderNo);
                return;
            }
            
            if (lockSuccess) {
                log.info("库存锁定成功，订单号：{}", orderNo);
                // 这里可以记录库存锁定成功的日志，或者更新订单的库存锁定状态
                // 如果订单有额外的库存锁定状态字段，可以在这里更新
            } else {
                log.error("库存锁定失败，订单号：{}，错误信息：{}", orderNo, message);
                // 库存锁定失败，需要取消订单
                cancelOrderAfterLockFailed(orderNo);
            }
            
        } catch (Exception e) {
            log.error("处理库存锁定结果异常，订单号：{}", orderNo, e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> handlePayCallback(String orderNo, String payNo, BigDecimal amount) {
        log.info("处理支付回调，订单号：{}，支付流水号：{}，金额：{}", orderNo, payNo, amount);
        
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }
        
        // 校验金额
        if (order.getTotalAmount().compareTo(amount) != 0) {
            log.error("支付金额不匹配，订单金额：{}，支付金额：{}", order.getTotalAmount(), amount);
            return Result.error("支付金额不匹配");
        }
        
        // 幂等性检查：如果订单已经是已支付状态，直接返回成功
        if (order.getStatus() == OrderStatusEnum.PAID.getCode()) {
            log.info("订单已支付，幂等处理，订单号：{}", orderNo);
            return Result.success();
        }
        
        // 只能支付待支付的订单
        if (order.getStatus() != OrderStatusEnum.WAIT_PAY.getCode()) {
            return Result.error("订单状态不允许支付");
        }
        
        // 更新订单状态
        int updateResult = orderMapper.updateToPaid(orderNo, OrderStatusEnum.PAID.getCode());
        if (updateResult <= 0) {
            return Result.error("更新订单状态失败");
        }
        
        // 记录操作日志
        saveOperationLog(orderNo, OrderOperationTypeEnum.PAY, 
                OrderStatusEnum.WAIT_PAY.getCode(), OrderStatusEnum.PAID.getCode(), 
                StrUtil.format("订单支付成功，支付流水号：{}", payNo), 
                order.getUserId(), "支付系统");
        
        // 发送支付成功消息，通知库存服务扣减库存
        InventoryLockDTO lockDTO = new InventoryLockDTO();
        lockDTO.setOrderNo(orderNo);
        lockDTO.setProductId(order.getProductId());
        lockDTO.setQuantity(order.getQuantity());
        lockDTO.setUserId(order.getUserId());
        
        Message<InventoryLockDTO> message = MessageBuilder.withPayload(lockDTO)
                .setHeader("ORDER_NO", orderNo)
                .setHeader("PAY_NO", payNo)
                .build();
        
        rocketMQTemplate.syncSend(OrderConstant.TOPIC_ORDER_PAY, message);
        
        log.info("支付回调处理成功，订单号：{}", orderNo);
        return Result.success();
    }
    
    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        // 格式：ORD + 时间戳 + 随机数
        return OrderConstant.ORDER_NO_PREFIX + 
                System.currentTimeMillis() + 
                IdUtil.getSnowflakeNextId() % 1000;
    }
    
    /**
     * 保存操作日志
     */
    private void saveOperationLog(String orderNo, OrderOperationTypeEnum operationType,
                                 Integer beforeStatus, Integer afterStatus,
                                 String remark, Long operatorId, String operatorName) {
        OrderOperationLog log = new OrderOperationLog();
        log.setOrderNo(orderNo);
        log.setOperationType(operationType.getCode());
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setRemark(remark);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        
        orderOperationLogMapper.insert(log);
    }
    
    /**
     * 库存锁定失败后取消订单
     */
    private void cancelOrderAfterLockFailed(String orderNo) {
        try {
            Order order = orderMapper.selectByOrderNo(orderNo);
            if (order == null) {
                return;
            }
            
            // 如果订单是待支付状态，则取消订单
            if (order.getStatus() == OrderStatusEnum.WAIT_PAY.getCode()) {
                orderMapper.updateToCanceled(orderNo, OrderStatusEnum.CANCELED.getCode());
                
                // 记录操作日志
                saveOperationLog(orderNo, OrderOperationTypeEnum.CANCEL, 
                        OrderStatusEnum.WAIT_PAY.getCode(), OrderStatusEnum.CANCELED.getCode(), 
                        "库存锁定失败，系统自动取消订单", 
                        0L, "系统");
                
                log.info("库存锁定失败，已自动取消订单，订单号：{}", orderNo);
            }
        } catch (Exception e) {
            log.error("取消订单失败，订单号：{}", orderNo, e);
        }
    }
    
    /**
     * 转换为VO对象
     */
    private OrderVO convertToVO(Order order) {
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        
        // 设置状态描述
        OrderStatusEnum statusEnum = OrderStatusEnum.getByCode(order.getStatus());
        if (statusEnum != null) {
            orderVO.setStatusDesc(statusEnum.getDesc());
        }
        
        // 查询订单详情
        List<OrderDetail> details = orderDetailMapper.selectByOrderNo(order.getOrderNo());
        if (details != null && !details.isEmpty()) {
            List<OrderVO.OrderDetailVO> detailVOS = details.stream().map(detail -> {
                OrderVO.OrderDetailVO detailVO = new OrderVO.OrderDetailVO();
                BeanUtils.copyProperties(detail, detailVO);
                return detailVO;
            }).collect(Collectors.toList());
            orderVO.setOrderDetails(detailVOS);
        }
        
        return orderVO;
    }
}