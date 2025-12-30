// order/src/main/java/com/demo/order/mq/OrderTransactionListener.java
package com.yzx.crazycodingbyteorder.listen;

import com.yzx.crazycodingbyteorder.entity.Order;
import com.yzx.crazycodingbyteorder.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RocketMQTransactionListener
@RequiredArgsConstructor
public class OrderTransactionListener implements RocketMQLocalTransactionListener {
    
    private final OrderMapper orderMapper;
    
    /**
     * 执行本地事务
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        String orderNo = (String) arg;
        log.info("执行本地事务，订单号：{}", orderNo);
        
        try {
            // 检查订单是否存在
            Order order = orderMapper.selectByOrderNo(orderNo);
            if (order == null) {
                log.error("订单不存在，回滚消息，订单号：{}", orderNo);
                return RocketMQLocalTransactionState.ROLLBACK;
            }
            
            // 订单存在，提交消息
            return RocketMQLocalTransactionState.COMMIT;
            
        } catch (Exception e) {
            log.error("执行本地事务异常，订单号：{}", orderNo, e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }
    
    /**
     * 检查本地事务状态
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String orderNo = null;
        try {
            orderNo = msg.getHeaders().get("ORDER_NO", String.class);
            log.info("检查本地事务状态，订单号：{}", orderNo);
            
            if (orderNo == null) {
                // 尝试从消息体中解析
                String body = new String((byte[]) msg.getPayload(), StandardCharsets.UTF_8);
                // 这里可以解析JSON获取orderNo，为了简单，我们直接返回UNKNOWN
                return RocketMQLocalTransactionState.UNKNOWN;
            }
            
            // 检查订单是否存在
            Order order = orderMapper.selectByOrderNo(orderNo);
            if (order == null) {
                log.error("订单不存在，回滚消息，订单号：{}", orderNo);
                return RocketMQLocalTransactionState.ROLLBACK;
            }
            
            return RocketMQLocalTransactionState.COMMIT;
            
        } catch (Exception e) {
            log.error("检查本地事务状态异常，订单号：{}", orderNo, e);
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }
}