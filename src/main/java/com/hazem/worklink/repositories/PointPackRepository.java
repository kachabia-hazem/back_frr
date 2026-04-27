package com.hazem.worklink.repositories;

import com.hazem.worklink.models.PointPack;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PointPackRepository extends MongoRepository<PointPack, String> {
    List<PointPack> findAllByOrderByDisplayOrderAsc();
}
