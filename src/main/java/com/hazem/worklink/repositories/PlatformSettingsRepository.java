package com.hazem.worklink.repositories;

import com.hazem.worklink.models.PlatformSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformSettingsRepository extends MongoRepository<PlatformSettings, String> {}
