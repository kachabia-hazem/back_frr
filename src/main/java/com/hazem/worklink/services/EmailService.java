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

    @Value("${spring.mail.username}")
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
                helper.setSubject("WorkLink — Confirmez votre adresse e-mail");
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
                "<td style='padding: 0 6px;'>" +
                "<div style='width:48px; height:56px; background:#f0f4ff; border:2px solid #4F46E5;" +
                "border-radius:10px; display:inline-flex; align-items:center; justify-content:center;" +
                "font-size:28px; font-weight:800; color:#4F46E5; line-height:56px; text-align:center;" +
                "font-family:Arial,sans-serif;'>" +
                digit +
                "</div></td>"
            );
        }

        return "<!DOCTYPE html>" +
            "<html lang='fr'><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>WorkLink - Vérification</title></head>" +
            "<body style='margin:0; padding:0; background-color:#f4f4f5; font-family:Arial,sans-serif;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f4f5; padding:40px 20px;'>" +
            "<tr><td align='center'>" +

            // Card
            "<table width='560' cellpadding='0' cellspacing='0' style='background:#ffffff;" +
            "border-radius:16px; overflow:hidden; box-shadow:0 4px 24px rgba(0,0,0,0.08);'>" +

            // Header
            "<tr><td align='center' style='padding:36px 40px 24px;'>" +
            "<span style='font-size:28px; font-weight:900; color:#1a1a2e; letter-spacing:-1px;'>" +
            "Work<span style='color:#4F46E5;'>Link</span>" +
            "<span style='color:#4F46E5; font-size:32px;'>.</span></span>" +
            "</td></tr>" +

            // Divider
            "<tr><td style='padding:0 40px;'>" +
            "<hr style='border:none; border-top:1px solid #f0f0f0; margin:0;'>" +
            "</td></tr>" +

            // Title
            "<tr><td align='center' style='padding:32px 40px 12px;'>" +
            "<h1 style='margin:0; font-size:22px; font-weight:800; color:#1a1a2e; line-height:1.3;" +
            "text-align:center;'>Vous y êtes presque !<br>Confirmez votre adresse e-mail.</h1>" +
            "</td></tr>" +

            // Description
            "<tr><td align='center' style='padding:12px 48px 28px;'>" +
            "<p style='margin:0; font-size:15px; color:#6b7280; line-height:1.6; text-align:center;'>" +
            "Félicitations, votre compte WorkLink est créé.<br>" +
            "Saisissez ce code pour l'activer." +
            "</p></td></tr>" +

            // Code label
            "<tr><td align='center' style='padding:0 40px 12px;'>" +
            "<p style='margin:0; font-size:14px; color:#9ca3af; text-align:center;'>Votre code :</p>" +
            "</td></tr>" +

            // Code digits
            "<tr><td align='center' style='padding:0 40px 36px;'>" +
            "<table cellpadding='0' cellspacing='0' style='margin:0 auto;'><tr>" +
            digitBoxes +
            "</tr></table></td></tr>" +

            // Expiry warning
            "<tr><td align='center' style='padding:0 40px 32px;'>" +
            "<p style='margin:0; font-size:13px; color:#f59e0b; text-align:center;'>" +
            "⏱ Ce code expire dans <strong>5 minutes</strong>.</p>" +
            "</td></tr>" +

            // Sign off
            "<tr><td style='padding:0 40px 32px;'>" +
            "<p style='margin:0; font-size:15px; color:#374151; line-height:1.8;'>" +
            "Merci,<br><strong>L'équipe WorkLink</strong></p>" +
            "</td></tr>" +

            // Footer divider
            "<tr><td style='padding:0 40px;'>" +
            "<hr style='border:none; border-top:1px solid #f0f0f0; margin:0;'>" +
            "</td></tr>" +

            // Footer
            "<tr><td align='center' style='padding:20px 40px;'>" +
            "<p style='margin:0; font-size:12px; color:#9ca3af; text-align:center;'>" +
            "Si vous n'avez pas créé de compte WorkLink, ignorez cet email.<br>" +
            "© 2026 WorkLink — Plateforme Freelance</p>" +
            "</td></tr>" +

            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    public boolean verifyCode(String email, String code) {
        Optional<VerificationCode> vc = verificationCodeRepository.findByEmailAndCode(email, code);
        return vc.isPresent();
    }

    public void deleteCode(String email) {
        verificationCodeRepository.deleteByEmail(email);
    }
}
