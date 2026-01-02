// order/src/main/java/com/yzx/crazycodingbyteorder/mapper/LocalMessageMapper.java
package com.yzx.crazycodingbytepay.mapper;

import com.yzx.crazycodingbytepay.entity.LocalMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LocalMessageMapper {

    // 根据业务ID（订单号）查询消息
    LocalMessage selectByBusinessId(@Param("businessId") String businessId);

    // 新增消息记录（订单创建时初始化）
    int insert(LocalMessage message);

    // 更新消息状态
    int updateById(LocalMessage message);
}