package com.recruitcrm.patterns.observer;

import com.recruitcrm.domain.Application;
import com.recruitcrm.notification.EmailSender;
import com.recruitcrm.notification.EmailTemplate;
import com.recruitcrm.notification.EmailTemplateRegistry;

/**
 * OBSERVER PATTERN — a Concrete Observer that sends real email.
 *
 * Replaces the console-only EmailNotificationObserver. It reacts to the
 * same event through the same interface; the publisher is unchanged.
 *
 * This is the extensibility claim made concrete: adding real email meant
 * writing one new Concrete Observer and registering it. Neither
 * ApplicationStatusPublisher nor RecruitmentFacade was modified.
 *
 * The EmailSender it holds is chosen at startup (see EmailConfig): a real
 * SMTP adapter when credentials are present, otherwise a console fallback.
 * This observer does not know or care which one it received.
 */
public class RealEmailObserver implements ApplicationObserver {

    private final EmailSender sender;

    public RealEmailObserver(EmailSender sender) {
        this.sender = sender;
    }

    @Override
    public void onStatusChanged(Application application) {
        EmailTemplate template = EmailTemplateRegistry.getInstance()
                .getTemplate(application.getStatus());

        if (template == null) {
            return;   // no template registered for this status - nothing to send
        }

        String to = application.getCandidate().getEmail();
        sender.send(to, template.subject(application), template.body(application));
    }
}
