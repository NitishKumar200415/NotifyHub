package com.notifyhub.notification.service.impl;

import com.notifyhub.notification.service.PushSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PushSenderServiceImpl implements PushSenderService {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    PushSenderServiceImpl.class
            );

    @Override
    public boolean sendPush(
            String deviceToken,
            String message
    ) {

        logger.info(
                "Sending PUSH notification to device token: {}",
                deviceToken
        );

        logger.info(
                "PUSH message: {}",
                message
        );

        /*
         * Temporary failure simulation.
         *
         * This allows us to test:
         *
         * QUEUED → Retry → Retry → Retry → DLQ
         */
        if ("fail-device-token".equals(deviceToken)) {

            logger.error(
                    "Simulated PUSH notification failure for: {}",
                    deviceToken
            );

            return false;
        }

        return true;
    }
}