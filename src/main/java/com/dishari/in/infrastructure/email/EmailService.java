package com.dishari.in.infrastructure.email;

import com.dishari.in.web.dto.response.BulkErrorDetail;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

        sendHtmlMail(toEmail , "Verify your account - Dishari", htmlContent);
        log.info("Verification email sent to={}", toEmail);
    }

    @Async
    public void sendForgotPasswordEmail(String toEmail, String name , String token) {
        String verificationLink = frontendBaseUrl + "/auth/forgot-password?token=" + token;

        Context context = new Context();
        context.setVariable("resetLink", verificationLink);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("userName", name);

        String htmlContent = templateEngine.process("forgot-password", context);

        sendHtmlMail(toEmail , "Forgot Password - Dishari", htmlContent);
        log.info("Forgot password email sent to={}", toEmail);
    }

    @Async
    public void sendPasswordChangedEmail(String email, String name) {
        String changeTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) ;
        Context context = new Context();
        context.setVariable("userName", name);
        context.setVariable("changeTime", changeTime);

        String htmlContent = templateEngine.process("password-changed", context);

        sendHtmlMail(email , "Password Changed - Dishari", htmlContent);
        log.info("Password changed email sent to={}", email);
    }

    @Async
    public void sendBulkUrlReport(String email , String userName , List<String> successUrls , List<BulkErrorDetail> failedUrls) {
        Context thymeleafContext = new Context();
        thymeleafContext.setVariable("userName", userName);
        thymeleafContext.setVariable("userEmail", email);
        thymeleafContext.setVariable("successUrls", successUrls);
        thymeleafContext.setVariable("failedUrls", failedUrls);

        // Process the HTML template (ensure the file is in src/main/resources/templates)
        String htmlContent = templateEngine.process("bulk-url-report", thymeleafContext);
        sendHtmlMail(email , "Bulk Url - Report" , htmlContent);

        log.info("Bulk deployment report transmitted to {}", email);
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
