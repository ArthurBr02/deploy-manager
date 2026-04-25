package fr.arthurbr02.deploymanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final AppConfigService configService;

    @Async
    public void sendEmail(String to, String subject, String text) {
        JavaMailSender mailSender = createMailSender();
        String from = configService.get("smtp_from", "noreply@deploymanager.com");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
            log.info("Email envoyé à {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'e-mail à {}: {}", to, e.getMessage());
        }
    }

    private JavaMailSender createMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(configService.get("smtp_host", "localhost"));
        mailSender.setPort(Integer.parseInt(configService.get("smtp_port", "587")));
        mailSender.setUsername(configService.get("smtp_username", ""));
        mailSender.setPassword(configService.get("smtp_password", ""));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }
}
