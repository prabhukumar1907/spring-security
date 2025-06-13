package com.demo.service;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${twilio.phone.number}")
    private String fromNumber;

    @Value("${twilio.account.sid}")
    private String accountSid;

    public void sendSms(String to, String message) {
        Message.creator(
                new PhoneNumber(to),        // To number
                new PhoneNumber(fromNumber), // From number
                message
        ).create();
    }
    public boolean checkVerificationCode(String phoneNumber, String code) {
        VerificationCheck verificationCheck = VerificationCheck.creator(accountSid)
                .setTo(phoneNumber)
                .setCode(code)
                .create();

        return "approved".equals(verificationCheck.getStatus());
    }
}
