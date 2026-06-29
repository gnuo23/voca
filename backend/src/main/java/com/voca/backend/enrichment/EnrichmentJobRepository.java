package com.voca.backend.enrichment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnrichmentJobRepository extends JpaRepository<EnrichmentJob, Long> {

    Optional<EnrichmentJob> findByIdAndOwnerId(Long id, Long ownerId);
}
