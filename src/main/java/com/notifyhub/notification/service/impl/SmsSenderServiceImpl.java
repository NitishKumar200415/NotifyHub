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

        // trim() is kept to prevent accidental spaces in the environment variable.
        // Example issue encountered during testing:
        // TWILIO_PHONE_NUMBER="+17372508034 "
        this.fromPhoneNumber = fromPhoneNumber.trim();
    }

    @Override
    public boolean sendSms(String to, String body) {

        try {
            Twilio.init(accountSid, authToken);

            log.info(
                    "Twilio SMS config - FROM: [{}], TO: [{}]",
                    fromPhoneNumber,
                    to
            );

            /*
             * ============================================================
             * TEMPORARY TWILIO TRIAL WORKAROUND
             * ============================================================
             *
             * NotifyHub normally sends the actual notification content
             * using the 'body' parameter.
             *
             * Normal production implementation:
             *
             * Message message = Message.creator(
             *         new PhoneNumber(to),
             *         new PhoneNumber(fromPhoneNumber),
             *         body
             * ).create();
             *
             * However, during development/testing, the current Twilio
             * trial account rejects arbitrary SMS content and returns:
             *
             * "Invalid template name. Trial accounts can only use
             * predefined SMS templates."
             *
             * The Twilio "Try out SMS" page provides predefined templates.
             * The following template identifier was successfully tested:
             *
             * sms_appointment_reminders
             *
             * Therefore, this value is temporarily used instead of 'body'
             * so that the complete NotifyHub SMS pipeline can be tested:
             *
             * NotifyHub API
             *      -> RabbitMQ
             *      -> SmsNotificationConsumer
             *      -> SmsSenderServiceImpl
             *      -> Twilio
             *      -> Notification status = SENT
             *
             * IMPORTANT:
             * This is a TEMPORARY TESTING WORKAROUND.
             *
             * When using a Twilio account/configuration that allows normal
             * arbitrary SMS content, remove 'twilioTemplate' and replace
             * it with the original 'body' implementation shown below.
             * ============================================================
             */

            String twilioTemplate = "sms_appointment_reminders";

            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromPhoneNumber),
                    twilioTemplate
            ).create();

            /*
             * ============================================================
             * RESTORE THIS FOR NORMAL/PRODUCTION SMS SENDING
             * ============================================================
             *
             * Replace the Message.creator() block above with:
             *
             * Message message = Message.creator(
             *         new PhoneNumber(to),
             *         new PhoneNumber(fromPhoneNumber),
             *         body
             * ).create();
             *
             * Also remove:
             *
             * String twilioTemplate = "sms_appointment_reminders";
             *
             * This will make NotifyHub send the actual notification payload
             * instead of the temporary Twilio predefined test template.
             * ============================================================
             */

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