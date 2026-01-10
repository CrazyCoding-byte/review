package com.yzx.crazycodingbyteorder.listen;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yzx.crazycodingbytecommon.entity.Idempotent;
import com.yzx.crazycodingbyteorder.dto.InventoryLockDTO;
import com.yzx.crazycodingbyteorder.entity.Order;
import com.yzx.crazycodingbyteorder.enums.OrderStatusEnum;
import com.yzx.crazycodingbyteorder.service.impl.OrderServiceImpl;
import com.yzx.crazycodingbyteorder.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.yzx.crazycodingbyteorder.constant.OrderConstant.TOPIC_INVENTORY_UNLOCK;

/**
 * @className: OrderDelaryListener
 * @author: yzx
 * @date: 2026/1/8 6:50
 * @Version: 1.0
 * @description:
 */
@Component
@RocketMQMessageListener(topic = "order-delay-topic",
        consumerGroup = "order-delay-group",
        consumeMode = ConsumeMode.ORDERLY
)
@Slf4j
@RequiredArgsConstructor

public class OrderDelayListener implements RocketMQListener<String> {
    private final OrderServiceImpl orderService;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    @Idempotent(key = "'ORDER_DELAY_'+T(com.alibaba.fastjson.JSON).parseObject(#s, T(com.yzx.crazycodingbyteorder.service.impl.OrderServiceImpl.OrderDelayCheckDTO)).orderNo",
            message = "延迟消息已处理，请勿重复消费",
            expireTime = 86400 // 幂等键有效期1天
    )
    public void onMessage(String s) {
        OrderServiceImpl.OrderDelayCheckDTO orderDelayCheckDTO = JSON.parseObject(s, OrderServiceImpl.OrderDelayCheckDTO.class);
        String orderNo = orderDelayCheckDTO.getOrderNo();
        //判断当前订单是否存在
        Order one = orderService.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (Objects.isNull(one)) {
            log.info("order-delay-topic订单是空{}", one);
            return;
        }
        Integer status = one.getStatus();
        //已经是取消的就不用管了
        if (status == OrderStatusEnum.CANCELED.getCode()) {
            log.info("order-delay-topic订单已取消{}", one);
            return;
        }
        //如果30分钟还是待支付的话就改为取消订单
        if (status == OrderStatusEnum.WAIT_PAY.getCode()) {
            one.setStatus(OrderStatusEnum.CANCELED.getCode());
        }
        InventoryLockDTO inventoryLockDTO = new InventoryLockDTO();
        inventoryLockDTO.setOrderNo(orderNo);
        inventoryLockDTO.setProductId(one.getProductId());
        inventoryLockDTO.setQuantity(one.getQuantity());
        inventoryLockDTO.setUserId(one.getUserId());
        //让库存服务去解锁库存
        rocketMQTemplate.syncSend(TOPIC_INVENTORY_UNLOCK, inventoryLockDTO);
    }
}
