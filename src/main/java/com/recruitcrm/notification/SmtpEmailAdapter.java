package com.recruitcrm.notification;

import java.io.IOException;

/**
 * ADAPTER PATTERN — the "Adapter".
 *
 * Implements the Target interface (EmailSender) that the application
 * wants, and fulfils it by delegating to the Adaptee (SmtpClient) whose
 * interface is completely different.
 *
 *     Client            Target             Adapter              Adaptee
 *     ------            ------             -------              -------
 *     RealEmail    ->   EmailSender   ->   SmtpEmailAdapter ->  SmtpClient
 *     Observer          .send(...)         .send(...)           .deliver(...)
 *                                                               EHLO/AUTH/DATA...
 *
 * This is object adapter form: the adapter HOLDS an SmtpClient rather
 * than inheriting from it. Our lecture slides showed both forms; the
 * object version is used here because it lets the same adapter be
 * pointed at a different SmtpClient (a different mail host) without
 * changing any class.
 *
 * The adapter also absorbs the difference in error handling: SmtpClient
 * throws a checked IOException, but EmailSender.send() declares no
 * checked exception, because a failed notification must never abort a
 * recruiter's status update. Translating that mismatch is part of the
 * adapter's job.
 */
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
