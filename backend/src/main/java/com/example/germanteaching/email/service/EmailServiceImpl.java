package com.example.germanteaching.email.service;


import com.example.germanteaching.email.dto.PasswordResetEmailData;
import com.example.germanteaching.email.dto.WelcomeEmailData;
import com.example.germanteaching.auth.entity.User;
import com.example.germanteaching.auth.entity.PasswordResetToken;
import com.example.germanteaching.email.config.EmailProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.management.RuntimeErrorException;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailServiceImpl implements EmailService{
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Autowired
    private EmailProperties emailProperties;
    
    @Override
    public void sendPasswordResetEmail(String toEmail, PasswordResetEmailData data){
        if (!emailProperties.isEnabled()) {
            logger.info("Email service is disabled. Would send password reset email to: {}", toEmail);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try{
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(emailProperties.getFromAddress(), emailProperties.getFromName());
                helper.setTo(toEmail);
                helper.setSubject("Password Reset Request - " + data.getAppName());

                // Create Thymeleaf context with data
                Context context = new Context();
                context.setVariable("username", data.getUsername());
                context.setVariable("resetUrl", data.getResetUrl());
                context.setVariable("expirationHours", data.getExpirationHours());
                context.setVariable("appName", data.getAppName());

                // Process HTML template
                String htmlContent = templateEngine.process("email/password-reset", context);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                logger.info("Password reset email sent successfully to: {}", toEmail);
            } catch (Exception e) {
                logger.error("Failed to send password reset email to : {}", toEmail, e);
                throw new RuntimeException("Failed to send password reset email", e);
            }
        });
    }
    
    @Override
    public void sendPasswordResetEmail(User user, PasswordResetToken token) {
        String resetUrl = buildPasswordResetUrl(token.getToken());

        PasswordResetEmailData emailData =  new PasswordResetEmailData(
            user.getUsername(),
            resetUrl,
            emailProperties.getResetTokenExpirationHours(),
            emailProperties.getAppName());

        sendPasswordResetEmail(user.getEmail(), emailData);
    }

    @Override
    public void sendWelcomeEmail(String toEmail, WelcomeEmailData data){
        if (!emailProperties.isEnabled()) {
            logger.info("Email service is disabled. Would send welcome email to : {}", toEmail);
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try{
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setFrom(emailProperties.getFromAddress(), emailProperties.getFromName());
                helper.setTo(toEmail);
                helper.setSubject("Welcome to " + data.getAppName() + "!");

                Context context = new Context();
                context.setVariable(("username"), data.getUsername());
                context.setVariable("loginUrl", data.getLoginUrl());
                context.setVariable("appName", data.getAppName());

                String htmlContent = templateEngine.process("email/welcome", context);
                helper.setText(htmlContent, true);
                
                mailSender.send(message);
                logger.info("Welcome email sent successfully to: {}", toEmail);
            } catch (Exception e){
                logger.error("Failed to send welcome email to: {}", toEmail, e);
            }
        });
    }

    @Override
    public void sendWelcomeEmail(User user) {
        WelcomeEmailData welcomeData = new WelcomeEmailData(
            user.getUsername(),
            buildLoginUrl(),
            emailProperties.getAppName()
            );
        
        sendWelcomeEmail(user.getEmail(), welcomeData);
    }

    @Override
    public void sendPlainTextEmail(String toEmail, String subject, String body){
        if (!emailProperties.isEnabled()) {
            logger.info("Email service is disable. Would send email to: {} with subject: {}", toEmail, subject);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try{
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(emailProperties.getFromAddress());
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(body);
                
                mailSender.send(message);
                logger.info("Plain text email sent successfully to: {}", toEmail);

            } catch (Exception e) {
                logger.error("Failed to send plain text email to: {}", toEmail, e);
                throw new RuntimeException("Failed to send email", e);
            }
        });
    }

    @Override
    public boolean isEmailEnabled() {
        return emailProperties.isEnabled();
    }
    
    @Override
    public String buildPasswordResetUrl(String token) {
        return String.format("%s/reset-password?token=%s",
            emailProperties.getBaseUrl(), token);
    }
    
    @Override
    public String buildLoginUrl() {
        return emailProperties.getBaseUrl() + "/login"; // CHECK THIS 
    };
}
