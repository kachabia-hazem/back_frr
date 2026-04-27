package com.hazem.worklink.services;

import com.hazem.worklink.models.VerificationCode;
import com.hazem.worklink.repositories.VerificationCodeRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final VerificationCodeRepository verificationCodeRepository;

    @Value("${spring.mail.from}")
    private String fromEmail;

    private static final SecureRandom RANDOM = new SecureRandom();

    public void sendVerificationCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        verificationCodeRepository.deleteByEmail(email);

        VerificationCode vc = new VerificationCode();
        vc.setEmail(email);
        vc.setCode(code);
        vc.setCreatedAt(Instant.now());
        verificationCodeRepository.save(vc);

        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom("WorkLink <" + fromEmail + ">");
                helper.setTo(email);
                helper.setSubject("WorkLink — Confirm your email address");
                helper.setText(buildVerificationEmailHtml(code), true);
                mailSender.send(message);
                log.info("Verification code sent to {}", email);
            } catch (Exception e) {
                log.error("Failed to send verification email to {}: {}", email, e.getMessage());
            }
        });
    }

    private String buildVerificationEmailHtml(String code) {
        String[] digits = code.split("");
        StringBuilder digitBoxes = new StringBuilder();
        for (String digit : digits) {
            digitBoxes.append(
                "<td style='padding:0 5px;'>" +
                "<table cellpadding='0' cellspacing='0' style='border-collapse:collapse;'><tr><td " +
                "align='center' valign='middle' " +
                "style='width:52px; height:60px; background:#eef6fb; border:2px solid #3793B0;" +
                "border-radius:10px; font-size:30px; font-weight:800; color:#3793B0;" +
                "text-align:center; vertical-align:middle; font-family:Arial,sans-serif;" +
                "line-height:60px; padding:0;'>" +
                digit +
                "</td></tr></table></td>"
            );
        }

        return "<!DOCTYPE html>" +
            "<html lang='en'><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>WorkLink - Verification</title></head>" +
            "<body style='margin:0; padding:0; background-color:#f4f4f5; font-family:Arial,sans-serif;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f4f5; padding:40px 20px;'>" +
            "<tr><td align='center'>" +

            // Card
            "<table width='560' cellpadding='0' cellspacing='0' style='background:#ffffff;" +
            "border-radius:16px; overflow:hidden; box-shadow:0 4px 24px rgba(0,0,0,0.08);'>" +

            // Header
            "<tr><td align='center' style='padding:36px 40px 24px;'>" +
            "<span style='font-size:28px; font-weight:900; color:#1a1a2e; letter-spacing:-1px;'>" +
            "Work<span style='color:#3793B0;'>Link</span>" +
            "<span style='color:#3793B0; font-size:32px;'>.</span></span>" +
            "</td></tr>" +

            // Divider
            "<tr><td style='padding:0 40px;'>" +
            "<hr style='border:none; border-top:1px solid #f0f0f0; margin:0;'>" +
            "</td></tr>" +

            // Title
            "<tr><td align='center' style='padding:32px 40px 12px;'>" +
            "<h1 style='margin:0; font-size:22px; font-weight:800; color:#1a1a2e; line-height:1.3;" +
            "text-align:center;'>You're almost there!<br>Confirm your email address.</h1>" +
            "</td></tr>" +

            // Description
            "<tr><td align='center' style='padding:12px 48px 28px;'>" +
            "<p style='margin:0; font-size:15px; color:#6b7280; line-height:1.6; text-align:center;'>" +
            "Congratulations, your WorkLink account has been created.<br>" +
            "Enter this code to activate it." +
            "</p></td></tr>" +

            // Code label
            "<tr><td align='center' style='padding:0 40px 12px;'>" +
            "<p style='margin:0; font-size:14px; color:#9ca3af; text-align:center;'>Your code:</p>" +
            "</td></tr>" +

            // Code digits
            "<tr><td align='center' style='padding:0 40px 36px;'>" +
            "<table cellpadding='0' cellspacing='0' style='margin:0 auto;'><tr>" +
            digitBoxes +
            "</tr></table></td></tr>" +

            // Expiry warning
            "<tr><td align='center' style='padding:0 40px 32px;'>" +
            "<p style='margin:0; font-size:13px; color:#3793B0; text-align:center;'>" +
            "&#9203; This code expires in <strong>5 minutes</strong>.</p>" +
            "</td></tr>" +

            // Sign off
            "<tr><td style='padding:0 40px 32px;'>" +
            "<p style='margin:0; font-size:15px; color:#374151; line-height:1.8;'>" +
            "Thank you,<br><strong>The WorkLink Team</strong></p>" +
            "</td></tr>" +

            // Footer divider
            "<tr><td style='padding:0 40px;'>" +
            "<hr style='border:none; border-top:1px solid #f0f0f0; margin:0;'>" +
            "</td></tr>" +

            // Footer
            "<tr><td align='center' style='padding:20px 40px;'>" +
            "<p style='margin:0; font-size:12px; color:#9ca3af; text-align:center;'>" +
            "If you did not create a WorkLink account, please ignore this email.<br>" +
            "© 2026 WorkLink — Freelance Platform</p>" +
            "</td></tr>" +

            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    public void sendPasswordResetCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        verificationCodeRepository.deleteByEmail(email);

        VerificationCode vc = new VerificationCode();
        vc.setEmail(email);
        vc.setCode(code);
        vc.setCreatedAt(Instant.now());
        verificationCodeRepository.save(vc);

        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom("WorkLink <" + fromEmail + ">");
                helper.setTo(email);
                helper.setSubject("WorkLink — Reset your password");
                helper.setText(buildPasswordResetEmailHtml(code), true);
                mailSender.send(message);
                log.info("Password reset code sent to {}", email);
            } catch (Exception e) {
                log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
            }
        });
    }

    private String buildPasswordResetEmailHtml(String code) {
        String[] digits = code.split("");
        StringBuilder digitBoxes = new StringBuilder();
        for (String digit : digits) {
            digitBoxes.append(
                "<td style='padding:0 5px;'>" +
                "<table cellpadding='0' cellspacing='0' style='border-collapse:collapse;'><tr><td " +
                "align='center' valign='middle' " +
                "style='width:52px; height:60px; background:#eef6fb; border:2px solid #3793B0;" +
                "border-radius:10px; font-size:30px; font-weight:800; color:#3793B0;" +
                "text-align:center; vertical-align:middle; font-family:Arial,sans-serif;" +
                "line-height:60px; padding:0;'>" +
                digit +
                "</td></tr></table></td>"
            );
        }

        return "<!DOCTYPE html>" +
            "<html lang='en'><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>WorkLink - Reset Password</title></head>" +
            "<body style='margin:0; padding:0; background-color:#f4f4f5; font-family:Arial,sans-serif;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f4f5; padding:40px 20px;'>" +
            "<tr><td align='center'>" +

            "<table width='560' cellpadding='0' cellspacing='0' style='background:#ffffff;" +
            "border-radius:16px; overflow:hidden; box-shadow:0 4px 24px rgba(0,0,0,0.08);'>" +

            "<tr><td align='center' style='padding:36px 40px 24px;'>" +
            "<span style='font-size:28px; font-weight:900; color:#1a1a2e; letter-spacing:-1px;'>" +
            "Work<span style='color:#3793B0;'>Link</span>" +
            "<span style='color:#3793B0; font-size:32px;'>.</span></span>" +
            "</td></tr>" +

            "<tr><td style='padding:0 40px;'>" +
            "<hr style='border:none; border-top:1px solid #f0f0f0; margin:0;'>" +
            "</td></tr>" +

            "<tr><td align='center' style='padding:32px 40px 12px;'>" +
            "<h1 style='margin:0; font-size:22px; font-weight:800; color:#1a1a2e; line-height:1.3;" +
            "text-align:center;'>Reset your password</h1>" +
            "</td></tr>" +

            "<tr><td align='center' style='padding:12px 48px 28px;'>" +
            "<p style='margin:0; font-size:15px; color:#6b7280; line-height:1.6; text-align:center;'>" +
            "You requested to reset your WorkLink password.<br>" +
            "Enter the code below to continue." +
            "</p></td></tr>" +

            "<tr><td align='center' style='padding:0 40px 12px;'>" +
            "<p style='margin:0; font-size:14px; color:#9ca3af; text-align:center;'>Your code:</p>" +
            "</td></tr>" +

            "<tr><td align='center' style='padding:0 40px 36px;'>" +
            "<table cellpadding='0' cellspacing='0' style='margin:0 auto;'><tr>" +
            digitBoxes +
            "</tr></table></td></tr>" +

            "<tr><td align='center' style='padding:0 40px 32px;'>" +
            "<p style='margin:0; font-size:13px; color:#3793B0; text-align:center;'>" +
            "&#9203; This code expires in <strong>5 minutes</strong>.</p>" +
            "</td></tr>" +

            "<tr><td align='center' style='padding:0 40px 28px;'>" +
            "<p style='margin:0; font-size:13px; color:#9ca3af; text-align:center;'>" +
            "If you did not request a password reset, please ignore this email.<br>" +
            "Your password will not be changed." +
            "</p></td></tr>" +

            "<tr><td style='padding:0 40px 32px;'>" +
            "<p style='margin:0; font-size:15px; color:#374151; line-height:1.8;'>" +
            "Thank you,<br><strong>The WorkLink Team</strong></p>" +
            "</td></tr>" +

            "<tr><td style='padding:0 40px;'>" +
            "<hr style='border:none; border-top:1px solid #f0f0f0; margin:0;'>" +
            "</td></tr>" +

            "<tr><td align='center' style='padding:20px 40px;'>" +
            "<p style='margin:0; font-size:12px; color:#9ca3af; text-align:center;'>" +
            "© 2026 WorkLink — Freelance Platform</p>" +
            "</td></tr>" +

            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    /**
     * Envoie un email de notification HTML à un utilisateur.
     * Appelé directement par NotificationService pour les notifications email.
     */
    public void sendNotificationEmail(String toEmail, String recipientName,
                                       String title, String message, String actionUrl) {
        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom("WorkLink <" + fromEmail + ">");
                helper.setTo(toEmail);
                helper.setSubject("WorkLink — " + title);
                helper.setText(buildNotificationEmailHtml(recipientName, title, message, actionUrl), true);
                mailSender.send(mimeMessage);
                log.info("Notification email sent to {} — {}", toEmail, title);
            } catch (Exception e) {
                log.error("Failed to send notification email to {}: {}", toEmail, e.getMessage());
            }
        });
    }

    private String buildNotificationEmailHtml(String recipientName, String title,
                                               String message, String actionUrl) {
        // Convert plain text line breaks to HTML
        String htmlMessage = message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n\n", "</p><p style='margin:0 0 14px; font-size:15px; color:#374151; line-height:1.7;'>")
                .replace("\n", "<br>");

        String actionButton = "";
        if (actionUrl != null && !actionUrl.isBlank()) {
            String fullUrl = actionUrl.startsWith("http") ? actionUrl : "http://localhost:4200" + actionUrl;
            actionButton =
                "<tr><td align='center' style='padding:8px 40px 32px;'>" +
                "<a href='" + fullUrl + "' " +
                "style='display:inline-block; background:#3793B0; color:#ffffff; " +
                "padding:13px 32px; border-radius:10px; text-decoration:none; " +
                "font-size:15px; font-weight:700; font-family:Arial,sans-serif;'>" +
                "Open WorkLink" +
                "</a></td></tr>";
        }

        return "<!DOCTYPE html>" +
            "<html lang='en'><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>WorkLink Notification</title></head>" +
            "<body style='margin:0; padding:0; background-color:#f4f4f5; font-family:Arial,sans-serif;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f4f5; padding:40px 20px;'>" +
            "<tr><td align='center'>" +

            "<table width='580' cellpadding='0' cellspacing='0' " +
            "style='background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 4px 24px rgba(0,0,0,0.08);'>" +

            // Header bar
            "<tr><td style='background:#3793B0; padding:28px 40px;'>" +
            "<span style='font-size:26px; font-weight:900; color:#ffffff; letter-spacing:-0.5px;'>" +
            "Work<span style='color:#d1f0fb;'>Link</span>" +
            "<span style='color:#d1f0fb; font-size:30px;'>.</span></span>" +
            "</td></tr>" +

            // Greeting
            "<tr><td style='padding:32px 40px 8px;'>" +
            "<p style='margin:0; font-size:15px; color:#9ca3af;'>Hello, <strong style='color:#1a1a2e;'>" +
            recipientName + "</strong></p>" +
            "</td></tr>" +

            // Title
            "<tr><td style='padding:12px 40px 20px;'>" +
            "<h1 style='margin:0; font-size:20px; font-weight:800; color:#1a1a2e; line-height:1.3;'>" +
            title + "</h1>" +
            "</td></tr>" +

            // Divider
            "<tr><td style='padding:0 40px 20px;'>" +
            "<hr style='border:none; border-top:1px solid #f0f0f0; margin:0;'>" +
            "</td></tr>" +

            // Message
            "<tr><td style='padding:0 40px 20px;'>" +
            "<p style='margin:0 0 14px; font-size:15px; color:#374151; line-height:1.7;'>" +
            htmlMessage + "</p>" +
            "</td></tr>" +

            // CTA button
            actionButton +

            // Sign off
            "<tr><td style='padding:0 40px 28px;'>" +
            "<p style='margin:0; font-size:15px; color:#374151; line-height:1.8;'>" +
            "Best regards,<br><strong>The WorkLink Team</strong></p>" +
            "</td></tr>" +

            // Footer
            "<tr><td style='background:#f8f9fb; padding:20px 40px; border-top:1px solid #f0f0f0;'>" +
            "<p style='margin:0; font-size:12px; color:#9ca3af; text-align:center;'>" +
            "You received this email because you have an account on WorkLink.<br>" +
            "© 2026 WorkLink — Freelance Platform</p>" +
            "</td></tr>" +

            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    public void sendReportRejectionEmail(String toEmail, String recipientName, String reason) {
        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom("WorkLink <" + fromEmail + ">");
                helper.setTo(toEmail);
                helper.setSubject("WorkLink — Votre signalement a été examiné");
                helper.setText(buildReportRejectionEmailHtml(recipientName, reason), true);
                mailSender.send(mimeMessage);
                log.info("Report rejection email sent to {}", toEmail);
            } catch (Exception e) {
                log.error("Failed to send report rejection email to {}: {}", toEmail, e.getMessage());
            }
        });
    }

    private String buildReportRejectionEmailHtml(String recipientName, String reason) {
        String safeReason = reason
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");

        return "<!DOCTYPE html>" +
            "<html lang='fr'><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>Signalement rejeté</title></head>" +
            "<body style='margin:0; padding:0; background-color:#f4f4f5; font-family:Arial,sans-serif;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f4f5; padding:40px 20px;'>" +
            "<tr><td align='center'>" +
            "<table width='580' cellpadding='0' cellspacing='0' " +
            "style='background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 4px 24px rgba(0,0,0,0.08);'>" +

            "<tr><td style='background:#3793B0; padding:28px 40px;'>" +
            "<span style='font-size:26px; font-weight:900; color:#ffffff; letter-spacing:-0.5px;'>" +
            "Work<span style='color:#d1f0fb;'>Link</span><span style='color:#d1f0fb; font-size:30px;'>.</span></span>" +
            "</td></tr>" +

            "<tr><td style='padding:32px 40px 8px;'>" +
            "<p style='margin:0; font-size:15px; color:#9ca3af;'>Bonjour, <strong style='color:#1a1a2e;'>" + recipientName + "</strong></p>" +
            "</td></tr>" +

            "<tr><td style='padding:12px 40px 20px;'>" +
            "<h1 style='margin:0; font-size:20px; font-weight:800; color:#1a1a2e;'>Votre signalement a été rejeté</h1>" +
            "</td></tr>" +

            "<tr><td style='padding:0 40px 20px;'>" +
            "<hr style='border:none; border-top:1px solid #f0f0f0; margin:0;'>" +
            "</td></tr>" +

            "<tr><td style='padding:0 40px 20px;'>" +
            "<p style='margin:0 0 14px; font-size:15px; color:#374151; line-height:1.7;'>" +
            "Après examen de votre signalement par notre équipe de modération, celui-ci a été rejeté pour le motif suivant :</p>" +
            "<div style='background:#fef2f2; border-left:4px solid #ef4444; padding:14px 18px; border-radius:8px; margin:16px 0;'>" +
            "<p style='margin:0; font-size:15px; color:#b91c1c; font-style:italic;'>" + safeReason + "</p>" +
            "</div>" +
            "<p style='margin:14px 0 0; font-size:15px; color:#374151; line-height:1.7;'>" +
            "Si vous pensez que cette décision est erronée, vous pouvez contacter notre support.</p>" +
            "</td></tr>" +

            "<tr><td style='padding:0 40px 28px;'>" +
            "<p style='margin:0; font-size:15px; color:#374151; line-height:1.8;'>" +
            "Cordialement,<br><strong>L'équipe WorkLink</strong></p>" +
            "</td></tr>" +

            "<tr><td style='background:#f8f9fb; padding:20px 40px; border-top:1px solid #f0f0f0;'>" +
            "<p style='margin:0; font-size:12px; color:#9ca3af; text-align:center;'>" +
            "© 2026 WorkLink — Plateforme Freelance</p>" +
            "</td></tr>" +

            "</table></td></tr></table></body></html>";
    }

    public boolean verifyCode(String email, String code) {
        Optional<VerificationCode> vc = verificationCodeRepository.findByEmailAndCode(email, code);
        return vc.isPresent();
    }

    public void deleteCode(String email) {
        verificationCodeRepository.deleteByEmail(email);
    }
}
