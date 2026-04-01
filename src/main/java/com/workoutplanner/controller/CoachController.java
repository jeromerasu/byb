package com.workoutplanner.controller;

import com.workoutplanner.dto.CoachDirectiveRequest;
import com.workoutplanner.dto.CoachDirectiveResponse;
import com.workoutplanner.dto.CoachDirectiveUpdateRequest;
import com.workoutplanner.dto.CoachPromptTemplateRequest;
import com.workoutplanner.dto.CoachPromptTemplateResponse;
import com.workoutplanner.model.CoachDirective;
import com.workoutplanner.model.CoachPromptTemplate;
import com.workoutplanner.service.CoachDirectiveService;
import com.workoutplanner.service.CoachPromptTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Coach-authed endpoints for managing prompt templates and directives.
 *
 * Phase 1 auth note: in production this controller should require a coach JWT.
 * TODO(PROD-HARDEN): Add coach JWT validation before production cutover.
 *   Owner: backend team. Target: Phase 2 coach portal delivery.
 */
@RestController
@RequestMapping("/api/v1/coach")
public class CoachController {

    private static final Logger log = LoggerFactory.getLogger(CoachController.class);

    private final CoachPromptTemplateService templateService;
    private final CoachDirectiveService directiveService;

    public CoachController(CoachPromptTemplateService templateService,
                           CoachDirectiveService directiveService) {
        this.templateService = templateService;
        this.directiveService = directiveService;
    }

    // -------------------------------------------------------------------------
    // Prompt template endpoints
    // -------------------------------------------------------------------------

    /**
     * POST /api/v1/coach/prompt-templates
     * Create or update a template (upsert by coach + user).
     * Requires X-Coach-Id header to identify the coach.
     */
    @PostMapping("/prompt-templates")
    public ResponseEntity<?> upsertPromptTemplate(
            @RequestBody CoachPromptTemplateRequest request,
            @RequestHeader(value = "X-Coach-Id", required = false) String coachId) {

        if (coachId == null || coachId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "X-Coach-Id header is required"));
        }
        if (request.getPromptContent() == null || request.getPromptContent().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "prompt_content is required"));
        }

        try {
            CoachPromptTemplate template = templateService.upsert(
                    coachId, request.getUserId(), request.getPromptContent());
            log.info("coach.template.upserted coachId={} userId={}", coachId, request.getUserId());
            return ResponseEntity.ok(CoachPromptTemplateResponse.from(template));
        } catch (Exception e) {
            log.error("coach.template.upsert_error coachId={} error={}", coachId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/v1/coach/prompt-templates/{userId}
     * Fetch resolved template for a client (per-client → coach default → not found).
     */
    @GetMapping("/prompt-templates/{userId}")
    public ResponseEntity<?> getPromptTemplate(
            @PathVariable String userId,
            @RequestHeader(value = "X-Coach-Id", required = false) String coachId) {

        if (coachId == null || coachId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "X-Coach-Id header is required"));
        }

        try {
            Optional<CoachPromptTemplate> resolved = templateService.resolve(coachId, userId);
            if (resolved.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(CoachPromptTemplateResponse.from(resolved.get()));
        } catch (Exception e) {
            log.error("coach.template.get_error coachId={} userId={} error={}", coachId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Directive endpoints
    // -------------------------------------------------------------------------

    /**
     * POST /api/v1/coach/directives
     * Add a directive for a client.
     */
    @PostMapping("/directives")
    public ResponseEntity<?> createDirective(
            @RequestBody CoachDirectiveRequest request,
            @RequestHeader(value = "X-Coach-Id", required = false) String coachId) {

        if (coachId == null || coachId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "X-Coach-Id header is required"));
        }
        if (request.getUserId() == null || request.getDirectiveType() == null || request.getContent() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "user_id, directive_type, and content are required"));
        }

        try {
            CoachDirective directive = directiveService.create(
                    coachId, request.getUserId(), request.getDirectiveType(), request.getContent());
            log.info("coach.directive.created coachId={} userId={}", coachId, request.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(CoachDirectiveResponse.from(directive));
        } catch (Exception e) {
            log.error("coach.directive.create_error coachId={} error={}", coachId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/v1/coach/directives/{id}
     * Update content or toggle active flag.
     */
    @PutMapping("/directives/{id}")
    public ResponseEntity<?> updateDirective(
            @PathVariable String id,
            @RequestBody CoachDirectiveUpdateRequest request) {

        try {
            CoachDirective directive = directiveService.update(id, request.getContent(), request.getActive());
            return ResponseEntity.ok(CoachDirectiveResponse.from(directive));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("coach.directive.update_error id={} error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/v1/coach/directives/{userId}?active=true
     * List all directives for a client, optionally filtered by active flag.
     */
    @GetMapping("/directives/{userId}")
    public ResponseEntity<?> listDirectives(
            @PathVariable String userId,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestHeader(value = "X-Coach-Id", required = false) String coachId) {

        if (coachId == null || coachId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "X-Coach-Id header is required"));
        }

        try {
            List<CoachDirective> directives = active != null
                    ? directiveService.findByCoachAndUser(coachId, userId, active)
                    : directiveService.findByCoachAndUser(coachId, userId);

            List<CoachDirectiveResponse> response = directives.stream()
                    .map(CoachDirectiveResponse::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("coach.directive.list_error coachId={} userId={} error={}", coachId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/v1/coach/directives/{id}?hard=false
     * Soft-delete (active=false) by default; hard=true for permanent removal.
     */
    @DeleteMapping("/directives/{id}")
    public ResponseEntity<?> deleteDirective(
            @PathVariable String id,
            @RequestParam(value = "hard", defaultValue = "false") boolean hard) {

        try {
            if (hard) {
                directiveService.deleteById(id);
            } else {
                directiveService.deactivate(id);
            }
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("coach.directive.delete_error id={} error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
