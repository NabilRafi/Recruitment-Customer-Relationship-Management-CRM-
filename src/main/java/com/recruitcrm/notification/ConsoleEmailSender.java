package com.recruitcrm.notification;


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
