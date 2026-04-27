package com.hazem.worklink.dto.request;

import com.hazem.worklink.models.enums.ReportType;
import lombok.Data;

@Data
public class CreateReportRequest {
    private ReportType type;
    private String reportedAgainstId;
    private String contractId;   // optional
    private String description;
}
