package com.example.demo;

import com.example.demo.model.NotificationMessage;
import com.example.demo.service.NotificationProcessor;
import com.example.demo.service.TwilioEmailService;
import com.example.demo.service.TwilioSmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
		"twilio.account-sid=test-sid",
		"twilio.auth-token=test-token",
		"twilio.phone-number=+10000000000",
		"sendgrid.api-key=test-key",
		"sendgrid.from-email=test@example.com"
})
class DemoApplicationTests {
	@Autowired
	private NotificationProcessor notificationProcessor;
	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private TwilioSmsService smsService;

	@MockBean
	private TwilioEmailService emailService;

	@Test
	void contextLoads() {
	}

	@Test
	void processSmsReceivedRoutesToSmsWithFormattedBody() {
		NotificationMessage message = new NotificationMessage(
				"sms",
				"received",
				"+15551234567",
				null,
				"Your prescription has been logged.",
				1L
		);

		notificationProcessor.process(message);

		verify(smsService).sendSms(
				eq("+15551234567"),
				eq("📬 Your item has been received.\n\nYour prescription has been logged.")
		);
		verify(emailService, never()).sendEmail(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void processEmailCollectUsesDefaultSubjectWhenBlank() {
		NotificationMessage message = new NotificationMessage(
				"email",
				"collect",
				"patient@example.com",
				"  ",
				"Please collect from the pharmacy desk.",
				2L
		);

		notificationProcessor.process(message);

		verify(emailService).sendEmail(
				eq("patient@example.com"),
				eq("Notification: Ready for Collection"),
				eq("✅ Your item is ready for collection!\n\nPlease collect from the pharmacy desk.")
		);
		verify(smsService, never()).sendSms(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void processUnknownChannelDoesNotSendAnything() {
		NotificationMessage message = new NotificationMessage(
				"push",
				"processing",
				"patient@example.com",
				"Subject",
				"Body",
				3L
		);

		notificationProcessor.process(message);

		verify(smsService, never()).sendSms(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
		verify(emailService, never()).sendEmail(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void notificationMessageContract_mapsMsgIdAndIgnoresUnknownFields() throws Exception {
		String json = """
				{
				  "channel":"email",
				  "type":"collect",
				  "recipient":"patient@example.com",
				  "subject":"Ready",
				  "body":"Pickup now",
				  "msg_id": 77,
				  "unexpected":"ignored"
				}
				""";

		NotificationMessage message = objectMapper.readValue(json, NotificationMessage.class);

		org.assertj.core.api.Assertions.assertThat(message.getChannel()).isEqualTo("email");
		org.assertj.core.api.Assertions.assertThat(message.getType()).isEqualTo("collect");
		org.assertj.core.api.Assertions.assertThat(message.getMsgId()).isEqualTo(77L);
	}

}
