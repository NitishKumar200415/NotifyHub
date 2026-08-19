package com.notifyhub.notification.service.impl;

import com.notifyhub.notification.service.SmsSenderService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsSenderServiceImpl implements SmsSenderService {

    private final String accountSid;
    private final String authToken;
    private final String fromPhoneNumber;

    public SmsSenderServiceImpl(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.phone-number}") String fromPhoneNumber
    ) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromPhoneNumber = fromPhoneNumber;
    }

    @Override
    public boolean sendSms(String to, String body) {

        try {
            Twilio.init(accountSid, authToken);

            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromPhoneNumber),
                    body
            ).create();

            log.info(
                    "SMS sent successfully to {} with SID {}",
                    to,
                    message.getSid()
            );

            return true;

        } catch (Exception ex) {
            log.error("Failed to send SMS to {}", to, ex);
            return false;
        }
    }
}