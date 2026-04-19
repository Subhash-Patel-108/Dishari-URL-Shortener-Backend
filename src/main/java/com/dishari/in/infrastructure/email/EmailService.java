package com.dishari.in.infrastructure.email;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class EmailService {
    @Autowired
    private JavaMailSender javaMailSender ;

    @Autowired
    private TemplateEngine templateEngine ;

    @Value("${spring.mail.username}")
    private String fromEmail ;

    @Value("${spring.app.email.support-email}")
    private String supportEmail ;

    @Value("${spring.app.email.base-url}")
    private String frontendBaseUrl ;


    //Method to send verification email
    @Async
    public void sendVerificationEmail(String toEmail, String name, String token) {
        String verificationLink = frontendBaseUrl + "/auth/verify-email?token=" + token;

        Context context = new Context();
        context.setVariable("verificationLink", verificationLink);
        context.setVariable("supportEmail", supportEmail);

        String htmlContent = templateEngine.process("verify-email", context);

        sendHtmlMail(toEmail , "Verify your account - SnapUrl", htmlContent);
        log.info("Verification email sent to={}", toEmail);
    }


    private void sendHtmlMail(String to , String subject , String htmlContent) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);
            helper.setText(htmlContent, true);
            javaMailSender.send(message);
            log.info("Email sent successfully to : {}" , to);
        } catch (Exception e) {
            log.error("Failed to send email to : {}" , to , e);
        }
        return ;
    }
}
