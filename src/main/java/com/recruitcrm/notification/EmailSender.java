package com.recruitcrm.notification;


public interface EmailSender {
    void send(String toAddress, String subject, String body);
}
