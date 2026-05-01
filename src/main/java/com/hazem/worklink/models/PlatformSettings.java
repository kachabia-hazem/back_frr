package com.hazem.worklink.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "platform_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSettings {

    @Id
    private String id = "platform_default";

    private int platformFeePercent = 7;

    private int applicationCost = 3;
    private int aiMatchingCost  = 5;
    private int aiRankingCost   = 5;
    private int welcomeBonus    = 25;

    private LocalDateTime updatedAt = LocalDateTime.now();
}
