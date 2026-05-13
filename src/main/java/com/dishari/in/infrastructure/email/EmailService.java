package com.dishari.in.infrastructure.email;

import com.dishari.in.utils.CSVUtils;
import com.dishari.in.web.dto.response.BulkErrorDetail;
import com.dishari.in.web.dto.response.BulkSuccessDetail;
import com.dishari.in.web.dto.response.BulkUrlResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final CSVUtils csvUtils ;

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
    public void sendBulkUrlReport(String email , String userName , List<BulkUrlResponse> successUrls , List<BulkErrorDetail> failedUrls) {


        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            // Set 'true' to indicate multipart message (needed for attachments)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setFrom(fromEmail);
            helper.setSubject("Dishari Ops: Batch Deployment Summary");

            int success = successUrls.size() ;
            int failure = failedUrls.size() ;
            int total = success + failure ;
            // 1. Prepare HTML Content
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("userEmail", email);
            context.setVariable("totalCount", total);
            context.setVariable("successCount", success);
            context.setVariable("failureCount", failure);

            // Process the HTML template (ensure the file is in src/main/resources/templates)
            String htmlContent = templateEngine.process("bulk-url-report", context);
            helper.setText(htmlContent, true);

            // 2. Generate and Attach CSV
            byte[] csvBytes = csvUtils.generateBulkCsv(successUrls, failedUrls);
            if (csvBytes.length > 0) {
                String dateAndTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
                String fileName = "Dishari_Bulk_Report" + dateAndTime + ".csv" ;
                helper.addAttachment(fileName, new ByteArrayResource(csvBytes));
            }

            javaMailSender.send(message);
            log.info("Bulk report with CSV attachment sent to {}", email);

        } catch (MessagingException e) {
            log.error("Failed to send bulk report with attachment", e);
        }
    }

    @Async
    public void sendInvitationMail(String email, String username, String token, String workspaceName, String invitedBy, String role) {
        // 1. Construct the absolute link
        String workspaceLink = frontendBaseUrl + "/workspace/join?token=" + token;

        // 2. Format date (Use a standard professional format)
        String invitedAt = DateTimeFormatter.ofPattern("MMM dd, yyyy").format(LocalDateTime.now());

        Context context = new Context(Locale.ENGLISH);

        // Match these exactly to the th:text="${...}" in your HTML
        context.setVariable("userName", username);          // Was memberName
        context.setVariable("invitedByName", invitedBy);     // Was ownerName
        context.setVariable("workspaceName", workspaceName);
        context.setVariable("role", role);
        context.setVariable("invitedAt", invitedAt);         // Was joinedAt
        context.setVariable("invitationLink", workspaceLink); // Was workspaceLink
        context.setVariable("userEmail", email);            // Was memberEmail

        // SDE-3 Tip: Add the logo URL here as well
//        context.setVariable("logoUrl", "https://your-cdn.com/logo.png");

        String htmlContent = templateEngine.process("workspace-invitation", context);
        sendHtmlMail(email, "Join " + workspaceName + " on SnapURL", htmlContent);

        log.info("Workspace invitation email dispatched for workspace={} to={}", workspaceName, email);
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
