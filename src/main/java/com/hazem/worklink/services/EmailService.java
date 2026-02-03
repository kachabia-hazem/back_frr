package com.hazem.worklink.services;

import com.hazem.worklink.models.VerificationCode;
import com.hazem.worklink.repositories.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

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

        // Remove any existing code for this email
        verificationCodeRepository.deleteByEmail(email);

        // Save new code
        VerificationCode vc = new VerificationCode();
        vc.setEmail(email);
        vc.setCode(code);
        vc.setCreatedAt(Instant.now());
        verificationCodeRepository.save(vc);

        // Send email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("WorkLink - Email Verification Code");
        message.setText("Your verification code is: " + code + "\n\nThis code expires in 5 minutes.");

        try {
            mailSender.send(message);
            log.info("Verification code sent to {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again.");
        }
    }

    public boolean verifyCode(String email, String code) {
        Optional<VerificationCode> vc = verificationCodeRepository.findByEmailAndCode(email, code);
        if (vc.isPresent()) {
            verificationCodeRepository.deleteByEmail(email);
            return true;
        }
        return false;
    }
}
