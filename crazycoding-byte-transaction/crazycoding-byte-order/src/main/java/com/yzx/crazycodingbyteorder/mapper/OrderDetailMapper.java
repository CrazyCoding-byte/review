// order/src/main/java/com/demo/order/mapper/OrderDetailMapper.java
package com.yzx.crazycodingbyteorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yzx.crazycodingbyteorder.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {
    
    /**
     * 根据订单号查询订单详情
     */
    @Select("SELECT * FROM order_detail WHERE order_no = #{orderNo}")
    List<OrderDetail> selectByOrderNo(@Param("orderNo") String orderNo);
}