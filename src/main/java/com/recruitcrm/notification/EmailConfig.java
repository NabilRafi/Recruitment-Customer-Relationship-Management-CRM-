package com.recruitcrm.notification;


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
