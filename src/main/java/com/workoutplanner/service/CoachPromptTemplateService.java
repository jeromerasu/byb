package com.workoutplanner.service;

import com.workoutplanner.model.CoachPromptTemplate;
import com.workoutplanner.repository.CoachPromptTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * CRUD for CoachPromptTemplate.
 * Upsert semantics: creating a template for an existing (coach, user) pair updates it.
 */
@Service
@Transactional
public class CoachPromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(CoachPromptTemplateService.class);

    private final CoachPromptTemplateRepository templateRepository;

    public CoachPromptTemplateService(CoachPromptTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Create or update a template. If a template already exists for (coachId, userId)
     * it is updated in place (upsert). userId=null targets the coach's default template.
     */
    public CoachPromptTemplate upsert(String coachId, String userId, String promptContent) {
        Optional<CoachPromptTemplate> existing = userId == null
                ? templateRepository.findByCoachIdAndUserIdIsNull(coachId)
                : templateRepository.findByCoachIdAndUserId(coachId, userId);

        CoachPromptTemplate template = existing.orElseGet(CoachPromptTemplate::new);
        template.setCoachId(coachId);
        template.setUserId(userId);
        template.setPromptContent(promptContent);

        CoachPromptTemplate saved = templateRepository.save(template);
        log.info("coach.template.upserted id={} coachId={} userId={} isDefault={}",
                saved.getId(), coachId, userId, saved.isCoachDefault());
        return saved;
    }

    /**
     * Resolves the best-matching template for a user under a given coach.
     * Fallback chain: per-client → coach default → empty (callers fall back to base prompt).
     */
    @Transactional(readOnly = true)
    public Optional<CoachPromptTemplate> resolve(String coachId, String userId) {
        Optional<CoachPromptTemplate> perClient = templateRepository.findByCoachIdAndUserId(coachId, userId);
        if (perClient.isPresent()) return perClient;
        return templateRepository.findByCoachIdAndUserIdIsNull(coachId);
    }

    @Transactional(readOnly = true)
    public Optional<CoachPromptTemplate> findById(String id) {
        return templateRepository.findById(id);
    }

    public void deleteById(String id) {
        templateRepository.deleteById(id);
        log.info("coach.template.deleted id={}", id);
    }
}
