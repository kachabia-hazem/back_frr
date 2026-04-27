package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Report;
import com.hazem.worklink.models.enums.ReportStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {
    List<Report> findAllByOrderByCreatedAtDesc();
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    List<Report> findByReportedByIdOrderByCreatedAtDesc(String userId);
    long countByStatus(ReportStatus status);
}
