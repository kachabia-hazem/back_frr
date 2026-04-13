package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.response.ContractResponse;
import com.hazem.worklink.services.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    /** GET /api/contracts/freelancer — list contracts for the authenticated freelancer */
    @GetMapping("/freelancer")
    public ResponseEntity<List<ContractResponse>> getFreelancerContracts(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(contractService.getFreelancerContracts(userDetails.getUsername()));
    }

    /** GET /api/contracts/company — list contracts for the authenticated company */
    @GetMapping("/company")
    public ResponseEntity<List<ContractResponse>> getCompanyContracts(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(contractService.getCompanyContracts(userDetails.getUsername()));
    }

    /** GET /api/contracts/{id} — get a single contract (freelancer or company) */
    @GetMapping("/{id}")
    public ResponseEntity<ContractResponse> getContract(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(contractService.getContractById(id, userDetails.getUsername()));
    }

    /** POST /api/contracts/{id}/sign — freelancer signs the contract */
    @PostMapping("/{id}/sign")
    public ResponseEntity<ContractResponse> signContract(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String signatureBase64 = body.get("signatureBase64");
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(contractService.signContract(id, userDetails.getUsername(), signatureBase64));
    }

    /** POST /api/contracts/{id}/reject — freelancer rejects the contract */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ContractResponse> rejectContract(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String reason = body.getOrDefault("reason", "");
        return ResponseEntity.ok(contractService.rejectContract(id, userDetails.getUsername(), reason));
    }

    /** POST /api/contracts/{id}/sign-company — company signs the contract */
    @PostMapping("/{id}/sign-company")
    public ResponseEntity<ContractResponse> signContractAsCompany(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String signatureBase64 = body.get("signatureBase64");
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(contractService.signContractAsCompany(id, userDetails.getUsername(), signatureBase64));
    }

    /** DELETE /api/contracts/{id} — freelancer deletes a contract from their list */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContract(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        contractService.deleteContract(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /** GET /api/files/contracts/{fileName} — serve contract PDF file */
    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<Resource> downloadContractFile(@PathVariable String fileName) {
        Resource resource = contractService.loadContractFile(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
