package com.workoutplanner.service;

import com.workoutplanner.model.CoachDirective;
import com.workoutplanner.model.DirectiveType;
import com.workoutplanner.repository.CoachDirectiveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * CRUD for CoachDirective.
 * Soft-delete (active=false) is the default deactivation path;
 * hard-delete is also supported via deleteById.
 */
@Service
@Transactional
public class CoachDirectiveService {

    private static final Logger log = LoggerFactory.getLogger(CoachDirectiveService.class);

    private final CoachDirectiveRepository directiveRepository;

    public CoachDirectiveService(CoachDirectiveRepository directiveRepository) {
        this.directiveRepository = directiveRepository;
    }

    public CoachDirective create(String coachId, String userId, DirectiveType type, String content) {
        CoachDirective directive = new CoachDirective();
        directive.setCoachId(coachId);
        directive.setUserId(userId);
        directive.setDirectiveType(type);
        directive.setContent(content);
        directive.setActive(true);
        CoachDirective saved = directiveRepository.save(directive);
        log.info("coach.directive.created id={} coachId={} userId={} type={}", saved.getId(), coachId, userId, type);
        return saved;
    }

    /**
     * Update content and/or toggle active flag. Only provided non-null values are changed.
     */
    public CoachDirective update(String directiveId, String content, Boolean active) {
        CoachDirective directive = directiveRepository.findById(directiveId)
                .orElseThrow(() -> new IllegalArgumentException("Directive not found: " + directiveId));
        if (content != null) directive.setContent(content);
        if (active != null) directive.setActive(active);
        CoachDirective saved = directiveRepository.save(directive);
        log.info("coach.directive.updated id={} active={}", directiveId, saved.isActive());
        return saved;
    }

    /** Soft-delete: sets active=false. */
    public void deactivate(String directiveId) {
        update(directiveId, null, false);
        log.info("coach.directive.deactivated id={}", directiveId);
    }

    /** Hard-delete: removes the row. */
    public void deleteById(String directiveId) {
        directiveRepository.deleteById(directiveId);
        log.info("coach.directive.deleted id={}", directiveId);
    }

    @Transactional(readOnly = true)
    public Optional<CoachDirective> findById(String id) {
        return directiveRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<CoachDirective> findByCoachAndUser(String coachId, String userId) {
        return directiveRepository.findByCoachIdAndUserId(coachId, userId);
    }

    @Transactional(readOnly = true)
    public List<CoachDirective> findByCoachAndUser(String coachId, String userId, boolean activeOnly) {
        if (activeOnly) {
            return directiveRepository.findByCoachIdAndUserIdAndActive(coachId, userId, true);
        }
        return directiveRepository.findByCoachIdAndUserId(coachId, userId);
    }
}
