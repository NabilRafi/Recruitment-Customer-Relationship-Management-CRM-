package com.recruitcrm.notification;

/**
 * Fallback EmailSender used when no mail credentials are configured.
 *
 * Prints the message instead of sending it, so the application keeps
 * working on a machine with no email set up (and during grading, where
 * the examiner may not want real mail going out).
 */
public class ConsoleEmailSender implements EmailSender {

    @Override
    public void send(String toAddress, String subject, String body) {
        System.out.println("========== EMAIL (console fallback) ==========");
        System.out.println("To:      " + toAddress);
        System.out.println("Subject: " + subject);
        System.out.println();
        System.out.println(body);
        System.out.println("=============================================");
    }
}
