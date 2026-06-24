package com.autoshorts.ai.service;

import com.autoshorts.ai.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Sends transactional emails. Disabled by default; when {@code app.mail.enabled=false}
 * it logs the message instead of sending so the rest of the pipeline never breaks.
 * The mail sender is built lazily from {@link AppProperties.Mail} (independent of
 * Spring's spring.mail.* auto-configuration).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final AppProperties appProperties;
    private volatile JavaMailSenderImpl mailSender;

    public EmailService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Async
    public void sendText(String to, String subject, String body) {
        AppProperties.Mail cfg = appProperties.getMail();
        if (!cfg.isEnabled()) {
            log.info("event=email_skipped reason=disabled to={} subject={}", to, subject);
            return;
        }
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(cfg.getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body == null ? "" : body);
            sender(cfg).send(message);
            log.info("event=email_sent to={} subject={}", to, subject);
        } catch (Exception ex) {
            log.warn("event=email_send_failed to={} subject={} message={}", to, subject, ex.getMessage());
        }
    }

    private JavaMailSenderImpl sender(AppProperties.Mail cfg) {
        JavaMailSenderImpl local = mailSender;
        if (local == null) {
            synchronized (this) {
                if (mailSender == null) {
                    JavaMailSenderImpl impl = new JavaMailSenderImpl();
                    impl.setHost(cfg.getHost());
                    impl.setPort(cfg.getPort());
                    impl.setUsername(cfg.getUsername());
                    impl.setPassword(cfg.getPassword());
                    Properties props = impl.getJavaMailProperties();
                    props.put("mail.transport.protocol", "smtp");
                    props.put("mail.smtp.auth", cfg.getUsername() != null && !cfg.getUsername().isBlank());
                    props.put("mail.smtp.starttls.enable", cfg.isStartTls());
                    mailSender = impl;
                }
                local = mailSender;
            }
        }
        return local;
    }
}
