package com.mandiwatch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Prints the SMS to the console instead of sending it. Real SMS in India needs
 * a paid gateway and DLT template registration, which is overkill for a demo.
 */
@Component
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public boolean send(String phone, String message) {
        log.info("SMS -> +91 {} : {}", phone, message);
        return true;
    }

    @Override
    public String channelName() {
        return "SMS (console)";
    }
}
