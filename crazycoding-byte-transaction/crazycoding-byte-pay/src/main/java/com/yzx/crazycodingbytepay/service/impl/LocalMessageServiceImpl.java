// order/src/main/java/com/yzx/crazycodingbyteorder/service/impl/LocalMessageServiceImpl.java
package com.yzx.crazycodingbytepay.service.impl;


import com.yzx.crazycodingbytepay.entity.LocalMessage;
import com.yzx.crazycodingbytepay.mapper.LocalMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocalMessageServiceImpl implements LocalMessageService {

    private final LocalMessageMapper localMessageMapper;

    @Transactional
    @Override
    public void updateMessageStatusForPaymentSuccess(String orderNo) {
        LocalMessage message = localMessageMapper.selectByBusinessId(orderNo);
        if (message != null) {
            // 状态更新为"待发送"（1=待发送，可根据实际业务调整）
            message.setStatus(1);
            localMessageMapper.updateById(message);
        }
    }

    @Transactional
    @Override
    public void updateMessageStatusForPaymentFailed(String orderNo, String errorMsg) {
        LocalMessage message = localMessageMapper.selectByBusinessId(orderNo);
        if (message != null) {
            // 状态更新为"失败"（-1=支付失败）
            message.setStatus(-1);
            localMessageMapper.updateById(message);
        }
    }
}