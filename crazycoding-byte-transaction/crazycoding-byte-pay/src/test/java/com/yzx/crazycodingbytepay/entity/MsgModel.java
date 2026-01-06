package com.yzx.crazycodingbytepay.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @className: MsgModel
 * @author: yzx
 * @date: 2026/1/5 4:52
 * @Version: 1.0
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MsgModel {
    private String orderSn;
    private Integer userId;
    private String dsc;

}
