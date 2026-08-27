package com.recruitcrm.notification;

import java.io.IOException;


public class SmtpEmailAdapter implements EmailSender {

    private final SmtpClient smtpClient;
    private final String fromAddress;
    private final String fromName;

    public SmtpEmailAdapter(SmtpClient smtpClient, String fromAddress, String fromName) {
        this.smtpClient = smtpClient;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void send(String toAddress, String subject, String body) {
        try {
            smtpClient.deliver(fromAddress, fromName, toAddress, subject, body);
            System.out.println("[EMAIL SENT] " + toAddress + " — " + subject);
        } catch (IOException e) {
            // Deliberately swallowed after logging. If the mail server is
            // unreachable, the candidate's status change has still happened
            // and must not be rolled back because of a notification failure.
            System.err.println("[EMAIL FAILED] " + toAddress + " — " + e.getMessage());
        }
    }
}
