package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

    Optional<PromptTemplate> findFirstByStyleKeyIgnoreCaseAndActiveTrue(String styleKey);

    @Query("""
        select distinct p.styleKey
        from PromptTemplate p
        where p.active = true
        order by p.styleKey asc
        """)
    List<String> findDistinctActiveStyleKeys();
}
