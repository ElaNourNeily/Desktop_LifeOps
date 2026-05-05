package service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmsService {
    // These should ideally be in a config file or environment variables
    public static final String ACCOUNT_SID = "YOUR_ACCOUNT_SID_HERE";
    public static final String AUTH_TOKEN = "YOUR_AUTH_TOKEN_HERE";
    public static final String TWILIO_PHONE_NUMBER = "YOUR_TWILIO_NUMBER_HERE";

    static {
        // Initialize Twilio once
        if (ACCOUNT_SID.startsWith("AC") && !ACCOUNT_SID.contains("x")) {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        }
    }

    public void sendSms(String to, String messageBody) {
        if (ACCOUNT_SID.startsWith("AC") && !ACCOUNT_SID.contains("x")) {
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
            System.out.println("[CONFIG NEEDED] Please update service.SmsService with real Twilio credentials.");
        }
    }
}
