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
import japlearn.demo.Entity.SmartCommunicationAssignment;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.ClassesRepository;
import japlearn.demo.Repository.SmartCommunicationAssignmentRepository;
import japlearn.demo.Repository.UserRepository;
import japlearn.demo.Service.SmartGoalsService;
import japlearn.demo.Service.SmartSurveyService;

/** SMART Goals Validation Dashboard. Admin-only (guarded by PortalAuthorizationFilter under /api/admin/). */
@RestController
@RequestMapping("/api/admin/smart-goals")
public class AdminSmartGoalsController {

    private final SmartGoalsService goals;
    private final SmartSurveyService surveys;
    private final SmartCommunicationAssignmentRepository assignments;
    private final ClassesRepository classes;
    private final UserRepository users;

    public AdminSmartGoalsController(SmartGoalsService goals, SmartSurveyService surveys,
            SmartCommunicationAssignmentRepository assignments, ClassesRepository classes, UserRepository users) {
        this.goals = goals;
        this.surveys = surveys;
        this.assignments = assignments;
        this.classes = classes;
        this.users = users;
    }

    @GetMapping("/report")
    public Map<String, Object> report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String classCode,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "false") boolean includeUnapproved) {
        return goals.report(new SmartGoalsService.Filters(from, to, classCode, studentId, module, includeUnapproved));
    }

    @GetMapping("/assignments")
    public List<Map<String, Object>> listAssignments() {
        Map<String, String> titles = classTitles();
        return assignments.findAllByOrderByDueAtDesc().stream().map(a -> SmartGoalsService.assignmentView(a, titles)).toList();
    }

    public record AssignmentRequest(String classCode, String activityKey, Integer level, String chapterId, String title,
            Instant startsAt, Instant dueAt) {}

    @PostMapping("/assignments")
    public ResponseEntity<?> createAssignment(@RequestBody AssignmentRequest request,
            @RequestHeader(value = "X-Portal-Token", required = false) String token) {
        if (request == null) return error("Missing assignment.");
        String key = request.activityKey() == null ? "" : request.activityKey().trim().toUpperCase(Locale.ROOT);
        if (!SmartGoalsService.ASSIGNABLE.contains(key)) return error("Choose a communication activity.");
        if (request.classCode() == null || classes.findByClassCodes(request.classCode().trim()).isEmpty()) return error("Choose an existing class.");
        if (request.startsAt() == null || request.dueAt() == null || !request.dueAt().isAfter(request.startsAt())) return error("The due date must be after the start date.");
        if (request.level() != null && (request.level() < 1 || request.level() > 3)) return error("Level must be 1 to 3.");
        String title = request.title() == null ? "" : request.title().replaceAll("[\\p{Cntrl}<>]", " ").trim();
        if (title.length() > 80) title = title.substring(0, 80);
        String chapter = request.chapterId() == null || request.chapterId().isBlank() ? null : request.chapterId().trim();
        if (chapter != null && !chapter.matches("[A-Za-z0-9_-]{1,64}")) return error("Invalid Reply Coach chapter.");

        SmartCommunicationAssignment a = new SmartCommunicationAssignment();
        a.setClassCode(request.classCode().trim());
        a.setActivityKey(key);
        a.setLevel(key.equals("RECOGNITION") || key.equals("EXPRESSION_MATCH") || key.equals("POLITENESS") ? request.level() : null);
        a.setChapterId(key.equals("REPLY_COACH") ? chapter : null);
        a.setTitle(title.isEmpty() ? key : title);
        a.setStartsAt(request.startsAt());
        a.setDueAt(request.dueAt());
        a.setCreatedBy(adminName(token));
        return ResponseEntity.ok(SmartGoalsService.assignmentView(assignments.save(a), classTitles()));
    }

    /** Removes an assignment created by mistake. Student game records are never touched. */
    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<?> deleteAssignment(@PathVariable String id) {
        if (!assignments.existsById(id)) return ResponseEntity.notFound().build();
        assignments.deleteById(id);
        return ResponseEntity.ok(Map.of("deleted", id));
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
