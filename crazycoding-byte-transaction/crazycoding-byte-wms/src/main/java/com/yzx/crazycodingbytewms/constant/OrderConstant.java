// order/src/main/java/com/demo/order/constant/OrderConstant.java
package com.yzx.crazycodingbytewms.constant;

public class OrderConstant {
    // 订单号前缀
    public static final String ORDER_NO_PREFIX = "ORD";
    
    // 订单锁定库存过期时间（30分钟）
    public static final long ORDER_LOCK_EXPIRE_MINUTES = 30;
    
    // 订单支付超时时间（30分钟）
    public static final long ORDER_PAY_TIMEOUT_MINUTES = 30;
    
    // 最大重试次数
    public static final int MAX_RETRY_COUNT = 3;
    
    // RocketMQ Topic
    public static final String TOPIC_ORDER_CREATE = "ORDER_CREATE_TOPIC";
    public static final String TOPIC_ORDER_PAY = "ORDER_PAY_TOPIC";
    public static final String TOPIC_ORDER_CANCEL = "ORDER_CANCEL_TOPIC";
    public static final String TOPIC_INVENTORY_LOCK = "INVENTORY_LOCK_TOPIC";
    public static final String TOPIC_INVENTORY_UNLOCK = "INVENTORY_UNLOCK_TOPIC";
    // RocketMQ Tag
    public static final String TAG_ORDER_CREATED = "ORDER_CREATED";
    public static final String TAG_INVENTORY_LOCKED = "INVENTORY_LOCKED";
    public static final String TAG_INVENTORY_LOCK_FAILED = "INVENTORY_LOCK_FAILED";
}