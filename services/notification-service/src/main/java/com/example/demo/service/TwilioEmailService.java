package com.example.demo.service;

import com.example.demo.config.TwilioConfig;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Service for sending email notifications via Twilio SendGrid.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioEmailService {

    private final TwilioConfig twilioConfig;

    /**
     * Sends an email via the SendGrid API (Twilio's email platform).
     *
     * @param to      the recipient email address
     * @param subject the email subject
     * @param body    the email body (plain text)
     */
    public void sendEmail(String to, String subject, String body) {
        Email from = new Email(twilioConfig.getFromEmail());
        Email toEmail = new Email(to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(twilioConfig.getSendGridApiKey());
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email sent successfully to {}. Status: {}", to, response.getStatusCode());
            } else {
                log.error("Email send failed to {}. Status: {}, Body: {}",
                        to, response.getStatusCode(), response.getBody());
                throw new RuntimeException("Email delivery failed with status " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Email delivery failed", e);
        }
    }
}
