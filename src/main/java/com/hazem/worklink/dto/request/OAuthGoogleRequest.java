package com.hazem.worklink.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthGoogleRequest {

    @NotBlank(message = "Le token Google est requis")
    private String idToken;
}
