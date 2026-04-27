package com.hazem.worklink.repositories;


import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.enums.CompanyStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends MongoRepository<Company, String> {

    Optional<Company> findByEmail(String email);

    Boolean existsByEmail(String email);

    Optional<Company> findByTradeRegister(String tradeRegister);

    List<Company> findByVerificationStatus(CompanyStatus status);

    long countByVerificationStatus(CompanyStatus status);
}