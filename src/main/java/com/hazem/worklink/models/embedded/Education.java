package com.hazem.worklink.models.embedded;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Education {
    private String id;
    private String diploma;
    private String institution;
    private Integer year;
    private String description;
}
