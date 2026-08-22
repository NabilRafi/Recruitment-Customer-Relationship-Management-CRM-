package com.recruitcrm.notification;

/**
 * ADAPTER PATTERN — the "Target" interface.
 *
 * This is the simple interface the rest of the application wants to use:
 * hand over a recipient, a subject and a body, and consider it sent.
 *
 * The application code never learns how mail actually travels. That is
 * the point — SmtpEmailAdapter translates this one method call into the
 * multi-step SMTP conversation a mail server expects, and
 * ConsoleEmailSender ignores the network entirely.
 */
public interface EmailSender {
    void send(String toAddress, String subject, String body);
}
