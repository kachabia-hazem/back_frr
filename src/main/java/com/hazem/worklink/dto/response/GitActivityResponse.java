package com.hazem.worklink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitActivityResponse {
    private String lastCommitMessage;
    private LocalDateTime lastPushDate;
    private String branch;
    private Integer commitCount;
}
