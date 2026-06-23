package com.splitit.infrastructure.email;

import com.splitit.domain.group.port.out.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * EmailSender adapter using Spring Mail + Thymeleaf. Renders HTML templates from
 * resources/templates/email/. Mailhog in dev, Gmail SMTP in prod (env-driven).
 */
@Component
public class SpringMailEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SpringMailEmailSender.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromAddress;

    public SpringMailEmailSender(JavaMailSender mailSender,
                                 TemplateEngine templateEngine,
                                 @Value("${app.mail.from:no-reply@split-it.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendGroupInvitation(String toEmail, String groupName, String registrationLink) {
        Context ctx = new Context();
        ctx.setVariable("groupName", groupName);
        ctx.setVariable("registrationLink", registrationLink);
        send(toEmail, "You're invited to join \"" + groupName + "\" on Split-it",
                "email/invitation", ctx);
    }

    @Override
    public void sendAddedToGroup(String toEmail, String groupName) {
        Context ctx = new Context();
        ctx.setVariable("groupName", groupName);
        send(toEmail, "You were added to \"" + groupName + "\" on Split-it",
                "email/added-to-group", ctx);
    }

    private void send(String toEmail, String subject, String template, Context ctx) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(templateEngine.process(template, ctx), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            // Email delivery is best-effort; the membership change has already been persisted.
            log.error("Failed to send '{}' email to {}", template, toEmail, e);
        }
    }
}
