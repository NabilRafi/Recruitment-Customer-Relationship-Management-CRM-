package com.recruitcrm.notification;

/**
 * Decides which EmailSender the application uses, based on environment
 * variables.
 *
 * Credentials are read from the environment and never stored in a file,
 * because this project lives in a public GitHub repository. A committed
 * mail password is found and abused by automated scanners very quickly.
 *
 * Required to send real mail:
 *   MAIL_USERNAME   your full Gmail address
 *   MAIL_PASSWORD   a Google App Password (NOT your normal password)
 *
 * Optional:
 *   MAIL_FROM_NAME  display name on the email (default "Fieldnote Recruitment")
 *   MAIL_HOST       default smtp.gmail.com
 *   MAIL_PORT       default 465
 *
 * If username or password is missing, this falls back to
 * ConsoleEmailSender so the application still runs normally.
 */
public final class EmailConfig {

    private EmailConfig() {}

    public static EmailSender buildSender() {
        String username = System.getenv("MAIL_USERNAME");
        String password = System.getenv("MAIL_PASSWORD");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            System.out.println("[MAIL] No MAIL_USERNAME / MAIL_PASSWORD set — printing emails to the console instead.");
            return new ConsoleEmailSender();
        }

        String host = System.getenv().getOrDefault("MAIL_HOST", "smtp.gmail.com");
        int port = Integer.parseInt(System.getenv().getOrDefault("MAIL_PORT", "465"));
        String fromName = System.getenv().getOrDefault("MAIL_FROM_NAME", "Fieldnote Recruitment");

        System.out.println("[MAIL] Sending real email as " + username + " via " + host + ":" + port);
        return new SmtpEmailAdapter(new SmtpClient(host, port, username, password), username, fromName);
    }
}
