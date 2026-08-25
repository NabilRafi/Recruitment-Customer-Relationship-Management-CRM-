package com.recruitcrm.notification;

import com.recruitcrm.domain.Application;


public interface EmailTemplate {
    String subject(Application application);
    String body(Application application);
}
