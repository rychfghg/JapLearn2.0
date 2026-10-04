package japlearn.demo.Controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import japlearn.demo.Service.AdminAlertService;

/** The portal pings this when the admin login page or admin dashboard is opened. */
@RestController
@RequestMapping("/api/security")
public class SecurityAlertController {
    private final AdminAlertService alerts;

    public SecurityAlertController(AdminAlertService alerts) {
        this.alerts = alerts;
    }

    @PostMapping("/admin-visit")
    public ResponseEntity<Void> adminVisit(@RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        String page = body == null ? "/admin" : body.getOrDefault("page", "/admin");
        // Only admin paths are reported; anything else is ignored.
        if (page != null && page.startsWith("/admin")) alerts.alert(AdminAlertService.Event.PAGE_OPENED, request, page);
        return ResponseEntity.noContent().build();
    }
}
