package com.hazem.worklink.dto.response;

import com.hazem.worklink.models.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthProfileResponse {

    private String providerId;
    private String email;
    private String firstName;
    private String lastName;
    private String picture;
    private AuthProvider provider;
}
