// order/src/main/java/com/demo/order/service/impl/OrderServiceImpl.java
package com.yzx.crazycodingbyteorder.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.yzx.crazycodingbytecommon.entity.BusinessException;
import com.yzx.crazycodingbytecommon.entity.Result;
import com.yzx.crazycodingbyteorder.constant.OrderConstant;
import com.yzx.crazycodingbyteorder.dto.CreateOrderRequest;
import com.yzx.crazycodingbyteorder.dto.InventoryLockDTO;
import com.yzx.crazycodingbyteorder.entity.LocalMessage;
import com.yzx.crazycodingbyteorder.entity.Order;
import com.yzx.crazycodingbyteorder.entity.OrderDetail;
import com.yzx.crazycodingbyteorder.entity.OrderOperationLog;
import com.yzx.crazycodingbyteorder.enums.OrderOperationTypeEnum;
import com.yzx.crazycodingbyteorder.enums.OrderStatusEnum;
import com.yzx.crazycodingbyteorder.mapper.LocalMessageMapper;
import com.yzx.crazycodingbyteorder.mapper.OrderDetailMapper;
import com.yzx.crazycodingbyteorder.mapper.OrderMapper;
import com.yzx.crazycodingbyteorder.mapper.OrderOperationLogMapper;
import com.yzx.crazycodingbyteorder.service.OrderService;
import com.yzx.crazycodingbyteorder.vo.OrderVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final LocalMessageMapper localMessageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderVO> createOrder(CreateOrderRequest request) {
        log.info("创建订单开始，请求参数：{}", request);
        try {
            // 1. 生成订单号
            String orderNo = generateOrderNo();
            //创建订单之前要先发送锁库存消息 只有锁成功才创建订单
            // 2. 计算订单总金额
            BigDecimal totalAmount = request.getProductPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            // 3. 创建订单记录
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(request.getUserId());
            order.setProductId(request.getProductId());
            order.setQuantity(request.getQuantity());
            order.setTotalAmount(totalAmount);
            order.setStatus(OrderStatusEnum.LOCKING.getCode());

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
            saveOperationLog(orderNo, OrderOperationTypeEnum.CREATE, null, OrderStatusEnum.WAIT_PAY.getCode(), "用户创建订单", request.getUserId(), "用户");
            // 6. 发送库存锁定消息（事务消息）
            InventoryLockDTO lockDTO = new InventoryLockDTO();
            lockDTO.setOrderNo(orderNo);
            lockDTO.setProductId(request.getProductId());
            lockDTO.setQuantity(request.getQuantity());
            lockDTO.setUserId(request.getUserId());
            // 发送半消息到库存服务
            Message<InventoryLockDTO> message = MessageBuilder.withPayload(lockDTO).setHeader("ORDER_NO", orderNo).build();

            TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(OrderConstant.TOPIC_INVENTORY_LOCK, message, orderNo  // 事务参数，会传递给事务监听器
            );
            // 校验半消息发送状态
            if (result.getSendStatus() != SendStatus.SEND_OK) {
                throw new BusinessException("库存锁定请求发送失败");
            }
            log.info("发送库存锁定事务消息成功，订单号：{}，消息ID：{}", orderNo, result.getMsgId());
            // 7. 返回订单信息
            OrderVO orderVO = convertToVO(order);
            //这里应该还要发送一个延迟消息 判断用户30分钟之类是否支付如果支付了就修改状态
            // 发送半消息到库存服务
            OrderDelayCheckDTO delayDTO = new OrderDelayCheckDTO();
            delayDTO.setOrderNo(orderNo);
            delayDTO.setUserId(request.getUserId());
            Message<InventoryLockDTO> orderMessage = MessageBuilder.withPayload(lockDTO).setHeader("ORDER_NO", JSON.toJSON(delayDTO)).build();
            rocketMQTemplate.syncSend("order_topic", orderMessage, 10000, 4);
            return Result.success(orderVO);
        } catch (Exception e) {
            log.error("创建订单失败", e);
            sendUnlockStockRequest(request);
            throw new BusinessException("创建订单失败：" + e.getMessage());
        }
    }

    @Data
    public class OrderDelayCheckDTO {
        private String orderNo;
        private Long userId;
    }

    private void sendUnlockStockRequest(CreateOrderRequest request) {
        // 生成临时订单号（或如果已生成正式订单号，直接用）
        String orderNo = generateOrderNo();
        InventoryLockDTO unlockDTO = new InventoryLockDTO();
        unlockDTO.setOrderNo(orderNo); // 关键：补全订单号，库存服务靠这个做幂等
        unlockDTO.setProductId(request.getProductId());
        unlockDTO.setQuantity(request.getQuantity());
        unlockDTO.setUserId(request.getUserId());

        // 发送解锁请求（同步发送，确保送达）
        rocketMQTemplate.syncSend(OrderConstant.TOPIC_INVENTORY_UNLOCK, MessageBuilder.withPayload(unlockDTO).build());
        log.info("发送解锁库存请求成功，订单号：{}，商品ID：{}", orderNo, request.getProductId());
    }

    /**
     * ★★★ 核心方法：保存本地消息
     * 和订单保存在同一个事务中，要么都成功，要么都失败
     */
    private void saveLocalMessage(String orderNo, CreateOrderRequest request) {
        LocalMessage localMessage = new LocalMessage();
        localMessage.setMessageId(IdUtil.simpleUUID());
        localMessage.setBusinessType("ORDER_CREATE");
        localMessage.setBusinessId(orderNo);
        localMessage.setStatus(0); // 0=待发送
        localMessage.setRetryCount(0);
        localMessage.setMaxRetry(3);

        // 构建消息内容
        Map<String, Object> messageContent = new HashMap<>();
        messageContent.put("orderNo", orderNo);
        messageContent.put("productId", request.getProductId());
        messageContent.put("quantity", request.getQuantity());
        messageContent.put("userId", request.getUserId());
        messageContent.put("productPrice", request.getProductPrice());
        messageContent.put("totalAmount", request.getProductPrice().multiply(BigDecimal.valueOf(request.getQuantity())));

        localMessage.setPayload(com.alibaba.fastjson2.JSON.toJSONString(messageContent));

        int result = localMessageMapper.insert(localMessage);
        if (result <= 0) {
            throw new BusinessException("保存本地消息失败");
        }

        log.info("本地消息保存成功，消息ID：{}，订单号：{}", localMessage.getMessageId(), orderNo);
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
        saveOperationLog(orderNo, OrderOperationTypeEnum.CANCEL, OrderStatusEnum.WAIT_PAY.getCode(), OrderStatusEnum.CANCELED.getCode(), "用户取消订单", userId, "用户");

        // 发送库存释放消息
        InventoryLockDTO lockDTO = new InventoryLockDTO();
        lockDTO.setOrderNo(orderNo);
        lockDTO.setProductId(order.getProductId());
        lockDTO.setQuantity(order.getQuantity());
        lockDTO.setUserId(userId);

        Message<InventoryLockDTO> message = MessageBuilder.withPayload(lockDTO).setHeader("ORDER_NO", orderNo).build();

        rocketMQTemplate.syncSend(OrderConstant.TOPIC_ORDER_CANCEL, message);

        log.info("订单取消成功，订单号：{}", orderNo);
        return Result.success();
    }

    @Override
    public void handleInventoryLockResult(String orderNo, boolean lockSuccess, String message) {
        log.info("处理库存锁定结果，订单号：{}，锁定结果：{}，消息：{}", orderNo, lockSuccess, message);

        try {
            Order order = orderMapper.selectByOrderNo(orderNo);
            if (order == null) {
                log.error("订单不存在，订单号：{}", orderNo);
                return;
            }

            if (lockSuccess) {
                log.info("库存锁定成功，订单号：{}", orderNo);
                // 这里可以记录库存锁定成功的日志，或者更新订单的库存锁定状态 修改锁定中状态改为待支付状态
                int updateResult = orderMapper.updateOrderStatus(orderNo, OrderStatusEnum.WAIT_PAY.getCode());
                if (updateResult > 0) {
                    log.info("库存锁定成功，订单已更新为待支付，订单号：{}", orderNo);
                    saveOperationLog(orderNo, OrderOperationTypeEnum.UPDATE_STATUS, OrderStatusEnum.LOCKING.getCode(), OrderStatusEnum.WAIT_PAY.getCode(), "库存锁定成功，订单转为待支付", 0L, "系统");
                }
            } else {
                log.error("库存锁定失败，订单号：{}，错误信息：{}", orderNo, message);
                // 库存锁定失败，需要取消订单
                cancelOrderAfterLockFailed(orderNo);
            }

        } catch (Exception e) {
            log.error("处理库存锁定结果异常，订单号：{}", orderNo, e);
            // 抛出异常让RocketMQ重试（确保最终处理成功）
            throw new RuntimeException("处理库存锁定结果失败，触发MQ重试", e);
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
        saveOperationLog(orderNo, OrderOperationTypeEnum.PAY, OrderStatusEnum.WAIT_PAY.getCode(), OrderStatusEnum.PAID.getCode(), StrUtil.format("订单支付成功，支付流水号：{}", payNo), order.getUserId(), "支付系统");


        return Result.success();
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        // 格式：ORD + 时间戳 + 随机数
        return OrderConstant.ORDER_NO_PREFIX + System.currentTimeMillis() + IdUtil.getSnowflakeNextId() % 1000;
    }

    /**
     * 保存操作日志
     */
    private void saveOperationLog(String orderNo, OrderOperationTypeEnum operationType, Integer beforeStatus, Integer afterStatus, String remark, Long operatorId, String operatorName) {
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
            // 如果订单是锁定状态，则取消订单 锁定状态意味着前面并没有讲锁定改为待支付说明锁定失败了
            if (order.getStatus() == OrderStatusEnum.LOCKING.getCode()) {
                orderMapper.updateToCanceled(orderNo, OrderStatusEnum.CANCELED.getCode());

                // 记录操作日志
                saveOperationLog(orderNo, OrderOperationTypeEnum.CANCEL, OrderStatusEnum.LOCKING.getCode(), OrderStatusEnum.CANCELED.getCode(), "库存锁定失败，系统自动取消订单", 0L, "系统");

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