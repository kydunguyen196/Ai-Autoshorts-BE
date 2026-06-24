package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.PublishStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.UUID;

public interface VideoJobRepository extends JpaRepository<VideoJob, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from VideoJob j where j.id = :jobId")
    Optional<VideoJob> findByIdForUpdate(@Param("jobId") UUID jobId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from VideoJob j where j.id = :jobId and j.userId = :userId")
    Optional<VideoJob> findByIdAndUserIdForUpdate(@Param("jobId") UUID jobId, @Param("userId") UUID userId);

    Optional<VideoJob> findByIdAndUserId(UUID jobId, UUID userId);

    List<VideoJob> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<VideoJob> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, JobStatus status, Pageable pageable);

    Page<VideoJob> findAllByUserId(UUID userId, Pageable pageable);

    Page<VideoJob> findAllByUserIdAndStatus(UUID userId, JobStatus status, Pageable pageable);

    Page<VideoJob> findAllByUserIdAndGenerationGroupId(UUID userId, UUID generationGroupId, Pageable pageable);

    Page<VideoJob> findAllByUserIdAndGenerationGroupIdAndStatus(
        UUID userId,
        UUID generationGroupId,
        JobStatus status,
        Pageable pageable
    );

    Page<VideoJob> findAllByUserIdAndGenerationBatchId(UUID userId, UUID generationBatchId, Pageable pageable);

    Page<VideoJob> findAllByUserIdAndGenerationBatchIdAndStatus(
        UUID userId,
        UUID generationBatchId,
        JobStatus status,
        Pageable pageable
    );

    Page<VideoJob> findAllByUserIdAndGenerationGroupIdAndTopCandidateTrue(
        UUID userId,
        UUID generationGroupId,
        Pageable pageable
    );

    List<VideoJob> findAllByUserIdAndGenerationGroupId(UUID userId, UUID generationGroupId);

    List<VideoJob> findAllByUserIdAndGenerationGroupIdAndSelectedForPublishTrue(UUID userId, UUID generationGroupId);

    long countByUserIdAndGenerationGroupId(UUID userId, UUID generationGroupId);

    boolean existsByTopicIgnoreCaseAndStyleIgnoreCaseAndUserIdAndChannelIdAndStatusIn(
        String topic,
        String style,
        UUID userId,
        UUID channelId,
        Collection<JobStatus> statuses
    );

    // --- Admin / dashboard aggregates ---
    long countByStatus(JobStatus status);

    long countByCreatedAtAfter(Instant createdAfter);

    Page<VideoJob> findByStatus(JobStatus status, Pageable pageable);

    List<VideoJob> findByStatusAndUpdatedAtBefore(JobStatus status, Instant updatedBefore, Pageable pageable);

    List<VideoJob> findByStatusAndCreatedAtBefore(JobStatus status, Instant createdBefore, Pageable pageable);

    /**
     * Posts submitted to an async publish provider that are awaiting completion.
     * Ordered oldest-checked-first (never-checked first) so the reconciler is fair under backlog.
     */
    @Query("select j from VideoJob j where j.publishStatus = :status and j.publishProvider = :provider "
        + "and j.publishExternalId is not null order by j.publishLastStatusCheckAt asc nulls first")
    List<VideoJob> findPendingAsyncPublishes(
        @Param("status") PublishStatus status,
        @Param("provider") String provider,
        Pageable pageable
    );
}
