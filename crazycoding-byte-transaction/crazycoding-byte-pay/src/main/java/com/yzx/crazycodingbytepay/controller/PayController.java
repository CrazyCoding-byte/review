// pay/src/main/java/com/yzx/crazycodingbytepay/controller/PayController.java
package com.yzx.crazycodingbytepay.controller;

import com.yzx.crazycodingbytepay.dto.PayRequest;
import com.yzx.crazycodingbytepay.dto.PayResponse;
import com.yzx.crazycodingbytepay.service.impl.PayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    @PostMapping("/process")
    public PayResponse processPayment(@RequestBody PayRequest request) {
        return payService.processPayment(request);
    }
}