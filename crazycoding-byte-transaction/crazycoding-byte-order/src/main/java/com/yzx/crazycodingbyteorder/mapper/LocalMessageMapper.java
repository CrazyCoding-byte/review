package com.yzx.crazycodingbyteorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yzx.crazycodingbyteorder.entity.LocalMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface LocalMessageMapper extends BaseMapper<LocalMessage> {
    
    /**
     * 查询待发送的消息（status=0）
     */
    @Select("SELECT * FROM local_message WHERE status = 0 AND retry_count < max_retry ORDER BY create_time ASC LIMIT #{limit}")
    List<LocalMessage> selectPendingMessages(@Param("limit") int limit);
    
    /**
     * 更新消息状态
     */
    @Update("UPDATE local_message SET status = #{status}, retry_count = retry_count + 1, error_msg = #{errorMsg}, update_time = NOW() WHERE message_id = #{messageId}")
    int updateMessageStatus(@Param("messageId") String messageId,
                           @Param("status") Integer status,
                           @Param("errorMsg") String errorMsg);
    
    /**
     * 标记为发送成功
     */
    @Update("UPDATE local_message SET status = 1, update_time = NOW() WHERE message_id = #{messageId}")
    int markAsSent(@Param("messageId") String messageId);
}