package com.tbs.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendBookingConfirmation(String toEmail, String customerName, String showTitle,
                                         String bookingReference, byte[] qrPng) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Your booking is confirmed - " + showTitle);
            helper.setText(
                    "Hi " + customerName + ",\n\n" +
                    "Your booking for \"" + showTitle + "\" is confirmed.\n" +
                    "Booking reference: " + bookingReference + "\n\n" +
                    "Your QR ticket is attached. Show it at entry for scanning.\n\n" +
                    "Thanks for booking with us!"
            );
            helper.addAttachment("ticket-" + bookingReference + ".png",
                    new jakarta.mail.util.ByteArrayDataSource(qrPng, "image/png"));
            mailSender.send(message);
        } catch (Exception e) {
            // Booking must not fail just because the email couldn't be sent -
            // log and let the customer retrieve the ticket from their booking
            // history instead.
            System.err.println("Failed to send booking confirmation email: " + e.getMessage());
        }
    }

    public void sendWaitlistOffer(String toEmail, String customerName, String showTitle,
                                   String offerUrl, int ttlMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false);
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("A seat opened up for " + showTitle);
            helper.setText(
                    "Hi " + customerName + ",\n\n" +
                    "A seat has opened up for \"" + showTitle + "\" and it's being offered to you " +
                    "as the next person on the waitlist.\n\n" +
                    "Complete your booking within " + ttlMinutes + " minutes using this link:\n" +
                    offerUrl + "\n\n" +
                    "If you don't complete the booking in time, the seat will be offered to the next person in line."
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send waitlist offer email: " + e.getMessage());
        }
    }
}
