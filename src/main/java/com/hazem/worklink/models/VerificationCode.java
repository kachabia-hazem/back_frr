package com.hazem.worklink.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "verification_codes")
public class VerificationCode {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String code;

    @Indexed(expireAfter = "300s")
    private Instant createdAt;
}
