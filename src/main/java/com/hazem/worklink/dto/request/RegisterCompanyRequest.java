package com.hazem.worklink.dto.request;


import com.hazem.worklink.models.enums.LegalForm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCompanyRequest {

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String companyName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    @NotBlank(message = "L'adresse est obligatoire")
    private String address;

    private String websiteUrl;

    @NotNull(message = "La forme juridique est obligatoire")
    private LegalForm legalForm;

    @NotBlank(message = "Le registre de commerce est obligatoire")
    private String tradeRegister;

    @NotNull(message = "La date de fondation est obligatoire")
    @Past(message = "La date de fondation doit être dans le passé")
    private LocalDate foundationDate;

    @NotBlank(message = "Le secteur d'activité est obligatoire")
    private String businessSector;

    @NotBlank(message = "Le nom du responsable est obligatoire")
    private String managerName;

    @NotBlank(message = "L'email du responsable est obligatoire")
    @Email(message = "Format d'email invalide")
    private String managerEmail;

    @NotBlank(message = "Le poste du responsable est obligatoire")
    private String managerPosition;

    @NotBlank(message = "Le téléphone du responsable est obligatoire")
    @Pattern(regexp = "^[+]?[0-9]{8,15}$", message = "Format de téléphone invalide")
    private String managerPhoneNumber;

    private String description;

    private Integer numberOfEmployees;
}