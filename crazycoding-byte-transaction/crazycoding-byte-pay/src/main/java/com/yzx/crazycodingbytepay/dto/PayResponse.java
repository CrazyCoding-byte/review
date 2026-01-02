// pay/src/main/java/com/yzx/crazycodingbytepay/dto/PayResponse.java
package com.yzx.crazycodingbytepay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayResponse {
    private boolean success;
    private String payNo;
    private String orderNo;
    private String message;

    public static PayResponse success(String payNo, String orderNo, String message) {
        return new PayResponse(true, payNo, orderNo, message);
    }

    public static PayResponse failure(String payNo, String orderNo, String message) {
        return new PayResponse(false, payNo, orderNo, message);
    }
}