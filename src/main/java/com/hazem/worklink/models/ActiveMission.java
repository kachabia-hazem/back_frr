package com.hazem.worklink.models;

import com.hazem.worklink.models.enums.ActiveMissionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "active_missions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveMission {

    @Id
    private String id;

    private String contractId;
    private String freelancerId;
    private String companyId;

    private String title;
    private String description;

    private ActiveMissionStatus status = ActiveMissionStatus.ACTIVE;
    private int progress = 0;

    private LocalDate startDate;
    private LocalDate endDate;

    // Git Activity (auto-fetched from GitHub API)
    private String gitRepositoryUrl;
    private String gitCurrentBranch;
    private Integer gitCommitCount;
    private LocalDateTime gitLastPushDate;
    private String gitLastCommitMessage;

    private LocalDateTime createdAt;
}
