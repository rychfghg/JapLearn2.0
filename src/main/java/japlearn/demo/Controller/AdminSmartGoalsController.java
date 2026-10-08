package japlearn.demo.Controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import japlearn.demo.Entity.Classes;
import japlearn.demo.Entity.SmartRefactoringPriority;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.ClassesRepository;
import japlearn.demo.Repository.SmartRefactoringPriorityRepository;
import japlearn.demo.Repository.UserRepository;
import japlearn.demo.Service.SmartGoalsService;
import japlearn.demo.Service.SmartSurveyService;

/** SMART Goals Validation Dashboard. Admin-only (guarded by PortalAuthorizationFilter under /api/admin/). */
@RestController
@RequestMapping("/api/admin/smart-goals")
public class AdminSmartGoalsController {

    private final SmartGoalsService goals;
    private final SmartSurveyService surveys;
    private final ClassesRepository classes;
    private final UserRepository users;
    private final SmartRefactoringPriorityRepository priorities;

    public AdminSmartGoalsController(SmartGoalsService goals, SmartSurveyService surveys,
            ClassesRepository classes, UserRepository users, SmartRefactoringPriorityRepository priorities) {
        this.priorities = priorities;
        this.goals = goals;
        this.surveys = surveys;
        this.classes = classes;
        this.users = users;
    }

    @GetMapping("/report")
    public ResponseEntity<?> report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String classCode,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "false") boolean includeUnapproved,
            @RequestParam(required = false) String surveyId) {
        // surveyId given: calculate only for students whose account email matched a respondent in that upload.
        if (surveyId != null && !surveyId.isBlank()) {
            try {
                return ResponseEntity.ok(goals.report(new SmartGoalsService.Filters(from, to, classCode, studentId, module, includeUnapproved,
                        surveys.matchedStudentIds(surveyId), "SURVEY_MATCHED")));
            } catch (ResponseStatusException e) {
                return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", String.valueOf(e.getReason())));
            }
        }
        return ResponseEntity.ok(goals.report(new SmartGoalsService.Filters(from, to, classCode, studentId, module, includeUnapproved)));
    }

    // ---- Requirements & Design Refactoring priorities (written by the team from validation evidence)

    @GetMapping("/refactoring")
    public List<SmartRefactoringPriority> listPriorities() {
        return priorities.findAllByOrderByRankAscUpdatedAtAsc();
    }

    public record PriorityRequest(String id, String goalKey, Integer rank, Boolean included, String learned, String evidence,
            String objective, String aspect, String expectedImprovement) {}

    @PostMapping("/refactoring")
    public ResponseEntity<?> savePriority(@RequestBody PriorityRequest request,
            @RequestHeader(value = "X-Portal-Token", required = false) String token) {
        if (request == null) return error("Missing priority.");
        String goal = request.goalKey() == null ? "" : request.goalKey().trim().toUpperCase(Locale.ROOT);
        if (!List.of("SMART1", "SMART2", "SMART3", "SMART4", "OTHER").contains(goal)) return error("Choose a SMART objective.");
        SmartRefactoringPriority p = request.id() == null || request.id().isBlank()
                ? new SmartRefactoringPriority()
                : priorities.findById(request.id()).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        p.setGoalKey(goal);
        p.setRank(request.rank() == null ? p.getRank() : Math.max(0, Math.min(99, request.rank())));
        if (request.included() != null) p.setIncluded(request.included());
        p.setLearned(text(request.learned()));
        p.setEvidence(text(request.evidence()));
        p.setObjective(text(request.objective()));
        p.setAspect(text(request.aspect()));
        p.setExpectedImprovement(text(request.expectedImprovement()));
        p.setUpdatedAt(Instant.now());
        p.setUpdatedBy(adminName(token));
        return ResponseEntity.ok(priorities.save(p));
    }

    @DeleteMapping("/refactoring/{id}")
    public ResponseEntity<?> deletePriority(@PathVariable String id) {
        if (!priorities.existsById(id)) return ResponseEntity.notFound().build();
        priorities.deleteById(id);
        return ResponseEntity.ok(Map.of("deleted", id));
    }

    /** Plain text only: control characters (except line breaks) and angle brackets removed, capped at 3000. */
    private static String text(String value) {
        if (value == null) return "";
        String v = value.replaceAll("[\\p{Cntrl}&&[^\\n]]|[<>]", " ").trim();
        return v.length() > 3000 ? v.substring(0, 3000) : v;
    }

    /** Matched respondents: questionnaire agreement beside measured performance, same filters as /report. */
    @GetMapping("/surveys/{id}/comparison")
    public ResponseEntity<?> comparison(@PathVariable String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String classCode,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "false") boolean includeUnapproved) {
        try {
            return ResponseEntity.ok(surveys.comparison(id,
                    goals.report(new SmartGoalsService.Filters(from, to, classCode, studentId, module, includeUnapproved))));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", String.valueOf(e.getReason())));
        }
    }

    @GetMapping("/surveys")
    public List<Map<String, Object>> listSurveys() {
        return surveys.summaries();
    }

    @PostMapping("/surveys")
    public ResponseEntity<?> uploadSurvey(@RequestBody SmartSurveyService.UploadRequest request,
            @RequestHeader(value = "X-Portal-Token", required = false) String token) {
        try {
            return ResponseEntity.ok(surveys.summary(surveys.upload(request, adminName(token))));
        } catch (ResponseStatusException e) {
            return error(e.getReason());
        }
    }

    private Map<String, String> classTitles() {
        Map<String, String> titles = new LinkedHashMap<>();
        for (Classes c : classes.findAll()) if (c.getClassCodes() != null) titles.putIfAbsent(c.getClassCodes(), c.getClassTitle());
        return titles;
    }

    private String adminName(String token) {
        User admin = token == null || token.isBlank() ? null : users.findByPortalSessionToken(token.trim());
        if (admin == null) return "admin";
        String n = ((admin.getFname() == null ? "" : admin.getFname()) + " " + (admin.getLname() == null ? "" : admin.getLname())).trim();
        return n.isEmpty() ? "admin" : n;
    }

    private static ResponseEntity<Map<String, String>> error(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message == null ? "Invalid request." : message));
    }
}
