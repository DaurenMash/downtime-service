package com.example.downtime.repository;

import com.example.downtime.model.Settings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettingsRepository extends MongoRepository<Settings, String> {

    Optional<Settings> findByKey(String key);

    List<Settings> findByCategory(String category);

    boolean existsByKey(String key);

    void deleteByKey(String key);
}