package com.hazem.worklink.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLinkedInRequest {

    @NotBlank(message = "Le code LinkedIn est requis")
    private String code;
}
