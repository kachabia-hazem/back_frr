package com.hazem.worklink.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateLegitRequest {
    private String activeMissionId;
    private String description;
    private Double totalAmount;
    private String resolution;
    private List<String> evidenceFiles = new ArrayList<>();
}
