package com.hazem.worklink.dto.response;


import com.hazem.worklink.models.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    private String email;

    private Role role;

    private String id;

    private String message;

    private Boolean needsRegistration;

    private LinkedInProfileResponse linkedInProfile;

    // Existing 5-arg constructor for backwards compatibility
    public AuthResponse(String token, String email, Role role, String id, String message) {
        this.token = token;
        this.email = email;
        this.role = role;
        this.id = id;
        this.message = message;
        this.needsRegistration = false;
        this.linkedInProfile = null;
    }
}
