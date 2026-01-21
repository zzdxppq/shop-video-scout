package com.shopvideoscout.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SMS service for sending verification codes.
 * Currently a mock implementation - replace with Aliyun SMS SDK in production.
 */
@Slf4j
@Service
public class SmsService {

    /**
     * Send SMS verification code to phone number.
     *
     * @param phone the phone number to send to
     * @param code  the verification code
     * @return true if sent successfully, false otherwise
     */
    public boolean sendVerificationCode(String phone, String code) {
        // TODO: Replace with actual Aliyun SMS SDK integration
        // For now, log the code for development/testing
        log.info("[SMS] Sending verification code {} to phone {}", code, phone);

        // Simulate SMS sending
        // In production, this would call Aliyun SMS API:
        // SendSmsRequest request = new SendSmsRequest();
        // request.setPhoneNumbers(phone);
        // request.setSignName("探店宝");
        // request.setTemplateCode("SMS_XXXXXXXX");
        // request.setTemplateParam("{\"code\":\"" + code + "\"}");
        // SendSmsResponse response = client.sendSms(request);
        // return "OK".equals(response.getCode());

        return true;
    }
}
