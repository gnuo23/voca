package com.voca.backend.enrichment;

import java.time.LocalDateTime;

public record EnrichmentJobResponse(
        Long id,
        EnrichmentJobStatus status,
        int totalItems,
        int processedItems,
        int failedItems,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
    static EnrichmentJobResponse from(EnrichmentJob job) {
        return new EnrichmentJobResponse(
                job.getId(),
                job.getStatus(),
                job.getTotalItems(),
                job.getProcessedItems(),
                job.getFailedItems(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getCompletedAt()
        );
    }
}
