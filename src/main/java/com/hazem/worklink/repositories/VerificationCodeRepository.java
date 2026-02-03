package com.hazem.worklink.repositories;

import com.hazem.worklink.models.VerificationCode;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface VerificationCodeRepository extends MongoRepository<VerificationCode, String> {

    Optional<VerificationCode> findByEmailAndCode(String email, String code);

    void deleteByEmail(String email);
}
