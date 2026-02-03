package com.hazem.worklink.dto.request;

import com.hazem.worklink.models.embedded.Certification;
import com.hazem.worklink.models.embedded.Education;
import com.hazem.worklink.models.embedded.Project;
import com.hazem.worklink.models.embedded.WorkExperience;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCvDataRequest {
    private String bio;
    private List<Education> education;
    private List<Project> projects;
    private List<String> skills;
    private List<Certification> certifications;
    private List<WorkExperience> workExperience;
}
