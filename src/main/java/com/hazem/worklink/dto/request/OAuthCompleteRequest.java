package com.hazem.worklink.dto.request;

import com.hazem.worklink.models.enums.AuthProvider;
import com.hazem.worklink.models.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthCompleteRequest {

    @NotNull(message = "Le rôle est requis")
    private Role role;

    @NotBlank(message = "L'email est requis")
    private String email;

    @NotBlank(message = "L'identifiant du provider est requis")
    private String providerId;

    @NotNull(message = "Le provider est requis")
    private AuthProvider provider;

    // Basic profile info from OAuth provider
    private String firstName;
    private String lastName;
    private String profilePicture;
}
