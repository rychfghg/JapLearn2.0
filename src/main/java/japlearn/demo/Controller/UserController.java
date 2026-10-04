package japlearn.demo.Controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestHeader;

import japlearn.demo.DTO.LoginRequest;
import japlearn.demo.Entity.User;
import japlearn.demo.Service.AccountDeletionService;
import japlearn.demo.Service.LoginAttemptLimiter;
import japlearn.demo.Service.StudentAuthorizationService;
import japlearn.demo.Service.UserService;
import japlearn.demo.Service.WebAccountDeletionService;

// CORS is now handled once, globally, in SecurityConfig's allow-listed
// CorsConfigurationSource — the old @CrossOrigin(origins = "*") on this
// controller let any website on the internet call these endpoints from a
// browser; it has been removed in favor of that allow-list.
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService japlearnService;
    private final StudentAuthorizationService studentAuthorization;
    private final AccountDeletionService accountDeletion;
    private final WebAccountDeletionService webAccountDeletion;
    private final LoginAttemptLimiter loginAttempts;
    @Autowired
    private japlearn.demo.Service.EmailActionLimiter emailActions;
    @Autowired
    private japlearn.demo.Service.RegistrationGuard registrationGuard;
    @Autowired
    private japlearn.demo.Service.AdminAlertService adminAlerts;
    @Autowired
    private japlearn.demo.Repository.UserRepository userRepository;

    // Server-side only: used to decide whether a failed sign-in should alert the owner.
    private boolean isAdminAccount(String email) {
        if (email == null || email.isBlank()) return false;
        User u = userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
        return u != null && "admin".equalsIgnoreCase(u.getRole());
    }

    @Autowired
    public UserController(UserService japlearnService,
            StudentAuthorizationService studentAuthorization,
            AccountDeletionService accountDeletion,
            WebAccountDeletionService webAccountDeletion,
            LoginAttemptLimiter loginAttempts) {
        this.japlearnService = japlearnService;
        this.studentAuthorization = studentAuthorization;
        this.accountDeletion = accountDeletion;
        this.webAccountDeletion = webAccountDeletion;
        this.loginAttempts = loginAttempts;
    }

    // Public web deletion is closed: requests are handled by email to the JapLearn team
    // (or in the signed-in app via /delete-account). These routes take no input and give
    // the same answer to everyone, so they cannot reveal anything about any account.
    private static final Map<String, String> DELETION_BY_EMAIL = Map.of(
            "status", "closed",
            "message", "Account deletion is handled by email. Write to japlearnofficial@gmail.com from your account's email address.");

    @PostMapping("/request-account-deletion")
    public ResponseEntity<?> requestAccountDeletion() {
        return ResponseEntity.status(HttpStatus.GONE).body(DELETION_BY_EMAIL);
    }

    @GetMapping("/account-deletion-request")
    public ResponseEntity<?> accountDeletionRequest() {
        return ResponseEntity.status(HttpStatus.GONE).body(DELETION_BY_EMAIL);
    }

    @PostMapping("/confirm-account-deletion")
    public ResponseEntity<?> confirmAccountDeletion() {
        return ResponseEntity.status(HttpStatus.GONE).body(DELETION_BY_EMAIL);
    }

    /**
     * Permanently deletes the signed-in learner's account and records.
     * Required by Google Play for apps that offer account creation.
     */
    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(@RequestParam("email") String email,
            @RequestHeader("X-Student-Token") String token,
            @RequestBody(required = false) Map<String, String> request) {
        // Only the signed-in owner of this account may delete it.
        String owner = studentAuthorization.requireStudent(email, token);

        String confirmation = request == null ? "" : String.valueOf(request.getOrDefault("confirmation", "")).trim();
        if (!"DELETE".equalsIgnoreCase(confirmation)) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", "Type DELETE to confirm removing this account."));
        }

        long removed = accountDeletion.deleteLearnerAccount(owner);
        return ResponseEntity.ok(Map.of(
                "message", "Your JapLearn account and learning records have been deleted.",
                "recordsRemoved", removed));
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            // Strict single-address check first: anything else (lists, line breaks, code) gets the generic reply and nothing is sent.
            String email = japlearn.demo.Service.EmailInput.normalize(request == null ? null : request.get("email"));
            if (email == null) return ResponseEntity.ok(Collections.singletonMap("message", "If the account exists, a password reset email has been sent."));
            // Admin accounts: never send a reset link; alert the owner instead. Same generic reply.
            if (isAdminAccount(email)) {
                adminAlerts.alert(japlearn.demo.Service.AdminAlertService.Event.RESET_ATTEMPT, httpRequest, "/admin/login");
                return ResponseEntity.ok(Collections.singletonMap("message", "If the account exists, a password reset email has been sent."));
            }
            // Cooldown per address; the reply is the same either way.
            if (emailActions.allow("reset", email)) japlearnService.sendForgotPasswordEmail(email);
            return ResponseEntity.ok(Collections.singletonMap("message", "If the account exists, a password reset email has been sent."));
        } catch (RuntimeException ex) {
            // Unknown address, blocked address or a send failure: never reveal which, always the same reply.
            return ResponseEntity.ok(Collections.singletonMap("message", "If the account exists, a password reset email has been sent."));
        }
    }

    @PostMapping("/reset-password")
public ResponseEntity<?> resetPassword(@RequestParam("token") String token, @RequestBody Map<String, String> request) {
    String newPassword = request.get("newPassword");
    String result = japlearnService.resetPassword(token, newPassword);
    if ("password_reset".equals(result)) {
        return ResponseEntity.ok(Collections.singletonMap("message", "Password reset successful"));
    } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "Invalid token"));
    }
}


    @GetMapping("/pending-approval")
    public ResponseEntity<List<User>> getPendingUsers() {
        List<User> usersAwaitingApproval = japlearnService.getUsersAwaitingApproval();
        return ResponseEntity.ok(usersAwaitingApproval);
    }

    @PostMapping("/approve/{userId}")
    public ResponseEntity<?> approveUser(@PathVariable String userId) {
        japlearnService.approveUser(userId);
        return ResponseEntity.ok("User approved");
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(@RequestParam(required = false) String role) {
        List<User> users = role == null || role.isBlank()
                ? japlearnService.getAllUsers()
                : japlearnService.getUsersByRole(role);
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @PostMapping("/admin-create")
    public ResponseEntity<?> createManagedUser(@RequestBody Map<String, Object> values) {
        try {
            User user = japlearnService.createManagedUser(values);
            user.setPassword(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", error.getMessage()));
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable String userId, @RequestBody Map<String, Object> updates) {
        try {
            User user = japlearnService.updateUser(userId, updates);
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", error.getMessage()));
        }
    }

    @PutMapping("/{userId}/guided-phrase-access")
    public ResponseEntity<?> updateGuidedPhraseAccess(@PathVariable String userId, @RequestBody Map<String, Object> request) {
        if (!request.containsKey("enabled")) return ResponseEntity.badRequest().body(Map.of("error", "The enabled value is required."));
        try {
            User user = japlearnService.updateUser(userId, Map.of("guidedPhraseEnabled", Boolean.TRUE.equals(request.get("enabled"))));
            user.setPassword(null);
            return ResponseEntity.ok(Map.of("userId", user.getId(), "guidedPhraseEnabled", user.isGuidedPhraseEnabled()));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", error.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        try {
            japlearnService.deleteUser(userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", error.getMessage()));
        }
    }

    @GetMapping("/confirm")
    public ResponseEntity<?> confirmEmail(@RequestParam("token") String token) {
        User confirmedUser = japlearnService.confirmUser(token);
        if (confirmedUser == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid or expired confirmation link"));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Email confirmed",
                "role", confirmedUser.getRole() == null ? "student" : confirmedUser.getRole().toLowerCase()
        ));
}


    @PostMapping("/register")
public ResponseEntity<?> registerUser(@RequestBody User user) {
    try {
        // Block throw-away inboxes, fake domains and sign-up floods before anything is saved or emailed.
        String cleanEmail = japlearn.demo.Service.EmailInput.normalize(user == null ? null : user.getEmail());
        if (cleanEmail == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Please use a valid Gmail address."));
        }
        user.setEmail(cleanEmail);
        japlearn.demo.Service.RegistrationGuard.Verdict verdict = registrationGuard.check(user == null ? null : user.getEmail(), user == null ? null : user.getRole());
        if (verdict == japlearn.demo.Service.RegistrationGuard.Verdict.NOT_ALLOWED) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Please use a valid Gmail address."));
        }
        if (verdict == japlearn.demo.Service.RegistrationGuard.Verdict.DISPOSABLE || verdict == japlearn.demo.Service.RegistrationGuard.Verdict.BAD_DOMAIN) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error",
                    "Please use a real email address you can access (for example Gmail, Outlook, Yahoo, or your school email)."));
        }
        if (verdict == japlearn.demo.Service.RegistrationGuard.Verdict.BUSY) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "900").body(Collections.singletonMap("error",
                    "Sign-ups are very busy right now. Please try again in a few minutes."));
        }
        String result = japlearnService.registerUser(user);
        // Same answer for a new or an already-registered email, so sign-up cannot be used
        // to check who has an account. (No email is sent for an existing address; the
        // owner can use "Forgot password" if needed.)
        if ("success".equals(result) || "duplicate".equals(result)) {
            return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("message", "Registration successful"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "Registration failed"));
        }
    } catch (Exception e) {
        e.printStackTrace();  // Log the exception
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "An unexpected error occurred"));
    }
}

    @GetMapping("/daily-goal/streak")
    public ResponseEntity<?> getDailyGoalStreak(@RequestParam String email) {
        try {
            return ResponseEntity.ok(japlearnService.getDailyGoalStreak(email));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", error.getMessage()));
        }
    }

    @PostMapping("/daily-goal/minute")
    public ResponseEntity<?> recordDailyGoalMinute(@RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(japlearnService.recordDailyGoalMinute(request.get("email")));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", error.getMessage()));
        }
    }

    @PostMapping("/daily-goal/complete")
    public ResponseEntity<?> completeDailyGoal(@RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(japlearnService.completeDailyGoal(request.get("email")));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", error.getMessage()));
        }
    }

    @PostMapping("/register-teacher")
    public ResponseEntity<?> registerTeacher(@RequestBody User user) {
        user.setRole("teacher");
        return registerUser(user);
    }


    @PostMapping("/login")
public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest, jakarta.servlet.http.HttpServletRequest httpRequest) {
    // Strict single-address check; anything that is not one plain email is a failed sign-in.
    String email = japlearn.demo.Service.EmailInput.normalize(loginRequest == null ? null : loginRequest.getEmail());
    if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Invalid credentials"));
    boolean adminAccount = isAdminAccount(email);
    // Admin accounts lock for 30 minutes after 5 wrong passwords; others for a minute after 6.
    // Same wording either way, so the reply never reveals that an email is an admin account.
    if (loginAttempts.isBlocked(email) || (adminAccount && loginAttempts.isAdminLocked(email))) {
        if (adminAccount) adminAlerts.alert(japlearn.demo.Service.AdminAlertService.Event.FAILED_SIGN_IN, httpRequest, "/admin/login");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Collections.singletonMap("error", "Too many incorrect attempts for this account. Please try again later."));
    }
    try {
        User authenticatedUser = japlearnService.authenticate(email, loginRequest.getPassword());
        loginAttempts.clear(email);
        String portalSessionToken = authenticatedUser.getPortalSessionToken();
        authenticatedUser.setPassword(null); 
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("id", authenticatedUser.getId());
        response.put("email", authenticatedUser.getEmail());
        response.put("fname", authenticatedUser.getFname());
        response.put("lname", authenticatedUser.getLname());
        response.put("role", authenticatedUser.getRole());
        response.put("portalSessionToken", portalSessionToken);
        if ("admin".equalsIgnoreCase(authenticatedUser.getRole())) adminAlerts.alert(japlearn.demo.Service.AdminAlertService.Event.SIGNED_IN, httpRequest, "/admin/login");
        return ResponseEntity.ok(response);
    } catch (UsernameNotFoundException | BadCredentialsException ex) {
        loginAttempts.recordFailure(email);
        if (adminAccount) loginAttempts.recordAdminFailure(email);
        if (adminAccount) adminAlerts.alert(japlearn.demo.Service.AdminAlertService.Event.FAILED_SIGN_IN, httpRequest, "/admin/login");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Invalid credentials"));
    } catch (IllegalStateException ex) {
        // Expected, user-facing account states ("Email not confirmed",
        // "User not approved") — safe to surface, the frontend matches on
        // this exact text. Not a security-relevant leak.
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error", ex.getMessage()));
    } catch (Exception e) {
        // Anything else is unexpected (DB hiccup, bug, etc.) — log it
        // server-side only. Returning e.getMessage() here used to leak raw
        // internal exception text (stack details, driver errors, etc.) back
        // to the client, which is exactly the kind of thing worth hiding.
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "An unexpected error occurred. Please try again."));
    }
}


}
