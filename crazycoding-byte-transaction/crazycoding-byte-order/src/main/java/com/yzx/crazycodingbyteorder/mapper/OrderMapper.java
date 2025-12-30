// order/src/main/java/com/demo/order/mapper/OrderMapper.java
package com.yzx.crazycodingbyteorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yzx.crazycodingbyteorder.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 根据订单号查询订单
     */
    @Select("SELECT * FROM `order` WHERE order_no = #{orderNo}")
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 更新订单状态
     */
    @Update("UPDATE `order` SET status = #{status}, update_time = NOW() " +
            "WHERE order_no = #{orderNo} AND status = #{oldStatus}")
    int updateOrderStatus(@Param("orderNo") String orderNo,
                          @Param("oldStatus") Integer oldStatus,
                          @Param("status") Integer status);

    /**
     * 更新订单支付状态
     */
    @Update("UPDATE `order` SET status = #{status}, pay_time = NOW(), update_time = NOW() " +
            "WHERE order_no = #{orderNo} AND status = 0")
    int updateToPaid(@Param("orderNo") String orderNo, @Param("status") Integer status);

    /**
     * 更新订单取消状态
     */
    @Update("UPDATE `order` SET status = #{status}, cancel_time = NOW(), update_time = NOW() " +
            "WHERE order_no = #{orderNo} AND status = 0")
    int updateToCanceled(@Param("orderNo") String orderNo, @Param("status") Integer status);

    /**
     * 根据用户ID分页查询订单
     */
    @Select("<script>" +
            "SELECT * FROM `order` WHERE user_id = #{userId} " +
            "<if test='status != null'>AND status = #{status}</if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    Order selectByUserId(@Param("userId") Long userId, @Param("status") Integer status);
}