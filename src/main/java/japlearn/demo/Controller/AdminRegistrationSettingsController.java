package japlearn.demo.Controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Service.RegistrationSettingsService;

/** Admin-only (guarded by PortalAuthorizationFilter under /api/admin/). */
@RestController
@RequestMapping("/api/admin/registration-settings")
public class AdminRegistrationSettingsController {

    private final RegistrationSettingsService settings;

    public AdminRegistrationSettingsController(RegistrationSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public Map<String, Object> get() {
        return Map.of("studentAutoApprove", settings.isStudentAutoApprove(),
                "teacherAutoApprove", settings.isTeacherAutoApprove());
    }

    /** Accepts either or both of studentAutoApprove / teacherAutoApprove. */
    @PutMapping
    public ResponseEntity<?> update(@RequestBody Map<String, Object> request) {
        if (request == null || !(request.containsKey("studentAutoApprove") || request.containsKey("teacherAutoApprove"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "studentAutoApprove or teacherAutoApprove is required."));
        }
        if (request.containsKey("studentAutoApprove")) settings.setStudentAutoApprove(Boolean.TRUE.equals(request.get("studentAutoApprove")));
        if (request.containsKey("teacherAutoApprove")) settings.setTeacherAutoApprove(Boolean.TRUE.equals(request.get("teacherAutoApprove")));
        return ResponseEntity.ok(get());
    }
}
