package com.hazem.worklink.models;


import com.hazem.worklink.models.enums.AuthProvider;
import com.hazem.worklink.models.enums.Role;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
public abstract class User {

    @Id
    protected String id;

    @Indexed(unique = true)
    protected String email;

    protected String password;

    protected Role role;

    protected AuthProvider authProvider = AuthProvider.LOCAL;

    protected String providerId;

    protected Boolean isActive = true;

    protected LocalDateTime createdAt = LocalDateTime.now();

    protected LocalDateTime updatedAt = LocalDateTime.now();
}