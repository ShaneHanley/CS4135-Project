package com.example.demo.service;

import com.example.demo.config.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sending SMS notifications via Twilio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioSmsService {

    private final TwilioConfig twilioConfig;

    /**
     * Sends an SMS message via the Twilio API.
     *
     * @param to   the recipient phone number (E.164 format, e.g. "+15551234567")
     * @param body the message body
     */
    public void sendSms(String to, String body) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(twilioConfig.getPhoneNumber()),
                    body
            ).create();

            log.info("SMS sent successfully to {}. SID: {}", to, message.getSid());
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("SMS delivery failed", e);
        }
    }
}
