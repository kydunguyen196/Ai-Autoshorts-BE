package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.TopicIdea;
import com.autoshorts.ai.entity.TopicIdeaStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicIdeaRepository extends JpaRepository<TopicIdea, UUID> {

    List<TopicIdea> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<TopicIdea> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, TopicIdeaStatus status, Pageable pageable);

    Page<TopicIdea> findAllByUserId(UUID userId, Pageable pageable);

    Page<TopicIdea> findAllByUserIdAndStatus(UUID userId, TopicIdeaStatus status, Pageable pageable);

    @Query(value = """
        SELECT *
        FROM topic_ideas
        WHERE status = 'PENDING'
          AND (scheduled_for IS NULL OR scheduled_for <= :now)
        ORDER BY priority DESC, created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<TopicIdea> findClaimablePendingTopics(@Param("now") Instant now, @Param("limit") int limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TopicIdea t where t.id = :topicId")
    Optional<TopicIdea> findByIdForUpdate(@Param("topicId") UUID topicId);

    Optional<TopicIdea> findByIdAndUserId(UUID topicId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TopicIdea t where t.id = :topicId and t.userId = :userId")
    Optional<TopicIdea> findByIdAndUserIdForUpdate(@Param("topicId") UUID topicId, @Param("userId") UUID userId);

    @Query("""
        select count(t) > 0
        from TopicIdea t
        where lower(t.topic) = lower(:topic)
          and lower(coalesce(t.contentStyle, '')) = lower(coalesce(:contentStyle, ''))
          and t.userId = :userId
          and t.channelId = :channelId
          and t.status in :statuses
        """)
    boolean existsByTopicAndContentStyleInStatuses(
        @Param("topic") String topic,
        @Param("contentStyle") String contentStyle,
        @Param("userId") UUID userId,
        @Param("channelId") UUID channelId,
        @Param("statuses") List<TopicIdeaStatus> statuses
    );
}
