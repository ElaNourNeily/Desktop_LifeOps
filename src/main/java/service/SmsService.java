package service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmsService {
    // Load credentials from environment variables for security
    public static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    public static final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
    public static final String TWILIO_PHONE_NUMBER = System.getenv("TWILIO_PHONE_NUMBER");

    static {
        // Initialize Twilio once (only if real credentials are provided)
        if (ACCOUNT_SID != null && ACCOUNT_SID.startsWith("AC")) {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        }
    }

    public void sendSms(String to, String messageBody) {
        if (ACCOUNT_SID != null && ACCOUNT_SID.startsWith("AC")) {
            try {
                Message message = Message.creator(
                        new PhoneNumber(to),
                        new PhoneNumber(TWILIO_PHONE_NUMBER),
                        messageBody
                ).create();
                System.out.println("SMS sent successfully: " + message.getSid());
            } catch (Exception e) {
                System.err.println("Failed to send SMS: " + e.getMessage());
            }
        } else {
            System.out.println("[SIMULATED SMS] To: " + to + " | Body: " + messageBody);
            System.out.println("[CONFIG NEEDED] Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_PHONE_NUMBER env vars.");
        }
    }
}
