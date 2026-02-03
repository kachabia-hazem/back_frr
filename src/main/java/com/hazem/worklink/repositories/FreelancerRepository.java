package com.hazem.worklink.repositories;


import com.hazem.worklink.models.Freelancer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FreelancerRepository extends MongoRepository<Freelancer, String> {

    Optional<Freelancer> findByEmail(String email);

    Boolean existsByEmail(String email);
}