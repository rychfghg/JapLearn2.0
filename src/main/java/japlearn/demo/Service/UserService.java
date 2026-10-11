package japlearn.demo.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import japlearn.demo.Entity.Student;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.StudentRepository;
import japlearn.demo.Repository.UserRepository;

@Service
public class UserService {
    private static final ZoneId JAPLEARN_TIME_ZONE = ZoneId.of("Asia/Manila");

    // Only these roles may ever come out of self-service registration
    // (/register and /register-teacher). Anything else in the request body —
    // "admin" included — is downgraded to "student". This is what stops a
    // client from POSTing {"role":"admin"} and handing themselves the keys.
    private static final Set<String> ALLOWED_SELF_REGISTER_ROLES = Set.of("student", "teacher");

    private static final int RESET_TOKEN_VALID_HOURS = 1;
    @Value("${app.backend-url}")
    private String appBackendUrl;

    @Value("${app.student-web-url}")
    private String studentWebUrl;

    @Value("${app.portal-web-url:https://portal.japlearn.com}")
    private String portalWebUrl;

    @Value("${app.mail.from-address}")
    private String mailFromAddress;
    
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    @Autowired
    private RegistrationSettingsService registrationSettings;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender; // Add JavaMailSender for sending emails

    @Autowired
    public UserService(UserRepository userRepository, JavaMailSender mailSender, StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.mailSender = mailSender;
        this.studentRepository = studentRepository; // Initialize studentRepository here
    }

    // Method to handle forgot password
    public String sendForgotPasswordEmail(String email) {
        email = EmailInput.normalize(email);
        if (email == null) throw new UsernameNotFoundException("User not found");
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        // Admin passwords are never reset by email. They are changed only in the database,
        // so knowing (or guessing) an admin email gives an attacker nothing to work with.
        if ("admin".equalsIgnoreCase(user.getRole())) {
            return "admin";
        }

        // Generate reset token. It used to never expire — resetTokenExpiry
        // was set on the entity but nothing ever wrote or checked it, so a
        // token leaked from an old email/log stayed valid forever. It now
        // expires in RESET_TOKEN_VALID_HOURS.
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now(JAPLEARN_TIME_ZONE).plusHours(RESET_TOKEN_VALID_HOURS));
        userRepository.save(user);

        // Send reset email
        boolean isTeacher = "teacher".equalsIgnoreCase(user.getRole());
        sendPasswordResetEmail(user.getEmail(), resetToken, isTeacher);

        return "success";
    }

    // Send reset password email
private void sendPasswordResetEmail(String email, String token, boolean isTeacher) {
    // Teachers reset inside the teacher portal so they land back on the teacher login.
    String resetUrl = isTeacher
            ? portalWebUrl + "/teacher/reset-password?token=" + token
            : studentWebUrl + "/ResetPassword?token=" + token;

    MimeMessage mimeMessage = mailSender.createMimeMessage();
    try {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(EmailInput.recipient(email)); // exactly one clean address, never a list
        // Previous sender fallback:
        // helper.setFrom("JapLearn <bacolod2186@gmail.com>");
        helper.setFrom("JapLearn <" + mailFromAddress + ">");
        helper.setSubject("Reset Password - JapLearn");

        String htmlContent = buildEmailTemplate(
                "ACCOUNT SECURITY",
                "Reset your password",
                "We received a request to create a new password for your JapLearn account.",
                "Reset password",
                resetUrl,
                "If you did not request this change, you can safely ignore this email. Your current password will remain unchanged."
        );

        helper.setText(htmlContent, true);
        helper.addInline("japlearnLogo", new ClassPathResource("mail/japlearn-logo.png"));
        mailSender.send(mimeMessage);
    } catch (MessagingException e) {
        e.printStackTrace();  // Log the exception
        throw new RuntimeException("Failed to send email", e);
    }
}


    // Method to reset the password
    public String resetPassword(String token, String newPassword) {
    if (token == null || token.isBlank()) {
        return "invalid";
    }
    if (!TextInput.validPassword(newPassword)) {
        return "invalid_password";
    }

    User user = userRepository.findByResetToken(token);
    if (user == null) {
        return "invalid";
    }
    // Even an old leftover reset link can never change an admin password.
    if ("admin".equalsIgnoreCase(user.getRole())) {
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        return "invalid";
    }

    LocalDateTime expiry = user.getResetTokenExpiry();
    if (expiry == null || LocalDateTime.now(JAPLEARN_TIME_ZONE).isAfter(expiry)) {
        // Token expired (or predates this check) — burn it either way so it
        // can't be retried, and make the caller request a fresh one.
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        return "invalid";
    }

    // Encrypt new password
    String encryptedPassword = passwordEncoder.encode(newPassword);
    user.setPassword(encryptedPassword);
    user.setResetToken(null); // Invalidate the token after reset
    user.setResetTokenExpiry(null);
    user.setPortalSessionToken(null);
    user.setPortalSessionExpiresAt(null);
    userRepository.save(user);

    // If the user is also in the Student table, update the password there
    Student student = studentRepository.findByEmail(user.getEmail());
    if (student != null) {
        student.setPassword(encryptedPassword);
        studentRepository.save(student);
    }

    return "password_reset";
}



    public List<User> getUsersAwaitingApproval() {
        return userRepository.findByIsEmailConfirmedTrueAndIsApprovedFalse();
        }

    

        public void approveUser(String userId) {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
            user.setApproved(true); // Set the user as approved
            userRepository.save(user);
            
            // Transfer user data to Student if the user has a "student" role
        }

        public List<User> getAllUsers() {
            return userRepository.findAll();
        }

    public List<User> getUsersByRole(String role) {
            return userRepository.findByRoleIgnoreCase(role);
        }


        public User createManagedUser(Map<String, Object> values) {
            String email = EmailInput.normalize(values.get("email"));
            if (email == null) throw new IllegalArgumentException("Invalid email address");
            if (userRepository.findByEmail(email) != null) throw new IllegalArgumentException("Email already exists");
            User user = new User();
            String createdFname = TextInput.name(values.get("fname")), createdLname = TextInput.name(values.get("lname"));
            if (createdFname == null || createdLname == null) throw new IllegalArgumentException(TextInput.NAME_RULE);
            user.setFname(createdFname);
            user.setLname(createdLname);
            user.setEmail(email);
            String role = String.valueOf(values.get("role")).trim().toLowerCase();
            // Admin accounts can never be created through the API (only directly in the database).
            if (!Set.of("student", "teacher").contains(role)) {
                throw new IllegalArgumentException("Invalid account role");
            }
            String password = String.valueOf(values.get("password"));
            if (!TextInput.validPassword(password)) throw new IllegalArgumentException(TextInput.PASSWORD_RULE);
            user.setRole(role);
            user.setPassword(passwordEncoder.encode(password));
            user.setEmailConfirmed(Boolean.TRUE.equals(values.get("emailConfirmed")));
            user.setApproved(Boolean.TRUE.equals(values.get("approved")));
            user.setGuidedPhraseEnabled(Boolean.TRUE.equals(values.get("guidedPhraseEnabled")));
            return userRepository.save(user);
        }

        public User updateUser(String userId, Map<String, Object> updates) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            // Admin accounts are managed only directly in the database, never through the API.
            if ("admin".equalsIgnoreCase(user.getRole())) {
                throw new IllegalArgumentException("Admin accounts cannot be changed here");
            }
            if (updates.containsKey("fname")) {
                String fname = TextInput.name(updates.get("fname"));
                if (fname == null) throw new IllegalArgumentException(TextInput.NAME_RULE);
                user.setFname(fname);
            }
            if (updates.containsKey("lname")) {
                String lname = TextInput.name(updates.get("lname"));
                if (lname == null) throw new IllegalArgumentException(TextInput.NAME_RULE);
                user.setLname(lname);
            }
            if (updates.containsKey("email")) {
                String email = EmailInput.normalize(updates.get("email"));
                if (email == null) throw new IllegalArgumentException("Invalid email address");
                user.setEmail(email);
            }
            if (updates.containsKey("role")) {
                // Only student <-> teacher; nobody can be promoted to admin (or any other role) via the API.
                String role = String.valueOf(updates.get("role")).trim().toLowerCase();
                if (!Set.of("student", "teacher").contains(role)) throw new IllegalArgumentException("Invalid account role");
                user.setRole(role);
            }
            if (updates.containsKey("approved")) user.setApproved(Boolean.TRUE.equals(updates.get("approved")));
            if (updates.containsKey("emailConfirmed")) user.setEmailConfirmed(Boolean.TRUE.equals(updates.get("emailConfirmed")));
            if (updates.containsKey("guidedPhraseEnabled")) user.setGuidedPhraseEnabled(Boolean.TRUE.equals(updates.get("guidedPhraseEnabled")));
            if (updates.containsKey("password") && updates.get("password") != null
                    && !String.valueOf(updates.get("password")).isBlank()) {
                String password = String.valueOf(updates.get("password"));
                if (!TextInput.validPassword(password)) throw new IllegalArgumentException(TextInput.PASSWORD_RULE);
                user.setPassword(passwordEncoder.encode(password));
                user.setPortalSessionToken(null);
                user.setPortalSessionExpiresAt(null);
            }
            User saved = userRepository.save(user);
            Student student = studentRepository.findByEmail(saved.getEmail());
            if (student != null) {
                student.setFname(saved.getFname());
                student.setLname(saved.getLname());
                student.setApproved(saved.isApproved());
                student.setEmailConfirmed(saved.isEmailConfirmed());
                student.setGuidedPhraseEnabled(saved.isGuidedPhraseEnabled());
                studentRepository.save(student);
            }
            return saved;
        }

        public void deleteUser(String userId) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if ("admin".equalsIgnoreCase(user.getRole())) {
                throw new IllegalArgumentException("Admin accounts cannot be deleted here");
            }
            Student student = studentRepository.findByEmail(user.getEmail());
            if (student != null) studentRepository.delete(student);
            userRepository.delete(user);
        }

        public String registerUser(User user) {
            try {
                if (user == null || user.getEmail() == null || user.getPassword() == null) {
                    return "invalid";
                }

                String email = EmailInput.normalize(user.getEmail());
                if (email == null) {
                    return "invalid";
                }
                if (!TextInput.validPassword(user.getPassword())) {
                    return "invalid_password";
                }
                // Names: real letters only (any language); no emojis, numbers, symbols or markup.
                String fname = TextInput.name(user.getFname()), lname = TextInput.name(user.getLname());
                if (fname == null || lname == null) {
                    return "invalid_name";
                }
                user.setFname(fname);
                user.setLname(lname);

                if (userRepository.findByEmail(email) != null) {
                    return "duplicate";
                }

                // --- Everything below this line is what actually closes the
                // hole: the incoming User is bound straight from the request
                // body by Jackson, so a client could previously send
                // {"role":"admin","isApproved":true,"isEmailConfirmed":true, ...}
                // and register a fully active admin account with no checks
                // at all. Every security-relevant field is now forced here,
                // regardless of what the request body contained. ---
                String requestedRole = user.getRole() == null ? "student" : user.getRole().trim().toLowerCase();
                user.setRole(ALLOWED_SELF_REGISTER_ROLES.contains(requestedRole) ? requestedRole : "student");
                user.setId(null);
                user.setEmail(email);
                // Students and teachers each have their own auto-approve switch (both off = manual approval).
                user.setApproved(registrationSettings.autoApproveFor(user.getRole()));
                user.setEmailConfirmed(false);
                user.setResetToken(null);
                user.setResetTokenExpiry(null);

                String encryptedPassword = passwordEncoder.encode(user.getPassword());
                user.setPassword(encryptedPassword);

                // Generate confirmation token
                String confirmationToken = UUID.randomUUID().toString();
                user.setConfirmationToken(confirmationToken);

                userRepository.save(user);

                // Send confirmation email
                sendConfirmationEmail(user.getEmail(), confirmationToken);

                return "success";
            } catch (Exception e) {
                e.printStackTrace();
                return "error";
            }
        }
        

        private void sendConfirmationEmail(String email, String token) {
            String confirmationUrl = studentWebUrl + "/ConfirmEmail?token=" + token;
        
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            try {
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.setTo(EmailInput.recipient(email)); // exactly one clean address, never a list
                // Previous sender fallback:
                // helper.setFrom("JapLearn <bacolod2186@gmail.com>");
                helper.setFrom("JapLearn <" + mailFromAddress + ">");
                helper.setSubject("Email Confirmation - JapLearn");
        
                String htmlContent = buildEmailTemplate(
                        "WELCOME TO JAPLEARN",
                        "Your Japanese journey starts here",
                        "Confirm your email address to activate your account and begin learning with JapLearn.",
                        "Confirm my email",
                        confirmationUrl,
                        "If you did not create a JapLearn account, you can safely ignore this email."
                );
        
                helper.setText(htmlContent, true);
                helper.addInline("japlearnLogo", new ClassPathResource("mail/japlearn-logo.png"));
        
                mailSender.send(mimeMessage);
            } catch (MessagingException e) {
                e.printStackTrace();  // Log the exception
                throw new RuntimeException("Failed to send email", e);
            }
        }

        private String buildEmailTemplate(
                String eyebrow,
                String title,
                String description,
                String actionText,
                String actionUrl,
                String securityNote
        ) {
            return "<!doctype html>"
                    + "<html><body style='margin:0;padding:0;background-color:#F7F3FA;'>"
                    + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' border='0' style='background-color:#F7F3FA;'>"
                    + "<tr><td align='center' style='padding:32px 14px;'>"
                    + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' border='0' style='max-width:560px;background:#FFFFFF;border-radius:24px;overflow:hidden;border:1px solid #E8DFF0;'>"
                    + "<tr><td style='height:8px;background:#8423D9;font-size:0;line-height:0;'>&nbsp;</td></tr>"
                    + "<tr><td align='center' style='padding:34px 34px 12px;'>"
                    + "<div style='width:76px;height:76px;margin:0 auto 16px;border-radius:22px;background:#F0E4FA;padding:8px;box-sizing:border-box;'>"
                    + "<img src='cid:japlearnLogo' width='60' height='60' alt='JapLearn' style='display:block;width:60px;height:60px;border:0;border-radius:16px;'>"
                    + "</div>"
                    + "<div style='font-family:Arial,sans-serif;font-size:12px;line-height:18px;font-weight:700;letter-spacing:1.5px;color:#65A936;'>" + eyebrow + "</div>"
                    + "<h1 style='margin:8px 0 10px;font-family:Arial,sans-serif;font-size:28px;line-height:35px;font-weight:700;color:#34203F;'>" + title + "</h1>"
                    + "<p style='margin:0 auto;max-width:430px;font-family:Arial,sans-serif;font-size:15px;line-height:24px;color:#6E6275;'>" + description + "</p>"
                    + "</td></tr>"
                    + "<tr><td align='center' style='padding:20px 34px 28px;'>"
                    + "<a href='" + actionUrl + "' style='display:inline-block;background:#8423D9;color:#FFFFFF;text-decoration:none;font-family:Arial,sans-serif;font-size:15px;line-height:20px;font-weight:700;padding:14px 28px;border-radius:12px;'>" + actionText + " &nbsp;&#8594;</a>"
                    + "<p style='margin:22px auto 0;max-width:420px;padding:13px 16px;border-radius:12px;background:#FAF7FC;font-family:Arial,sans-serif;font-size:12px;line-height:19px;color:#817586;'>" + securityNote + "</p>"
                    + "</td></tr>"
                    + "<tr><td style='padding:20px 30px;background:#F5EFF9;text-align:center;'>"
                    + "<p style='margin:0;font-family:Arial,sans-serif;font-size:12px;line-height:18px;color:#6F6277;'>Keep learning, one small step at a time.</p>"
                    + "<p style='margin:4px 0 0;font-family:Arial,sans-serif;font-size:12px;line-height:18px;font-weight:700;color:#4D365A;'>The JapLearn Team</p>"
                    + "<p style='margin:8px 0 0;font-family:Arial,sans-serif;font-size:11px;line-height:17px;color:#9A8FA0;'>japlearnofficial@gmail.com</p>"
                    + "</td></tr></table>"
                    + "</td></tr></table>"
                    + "</body></html>";
        }
        
        
        
        
    

    @org.springframework.beans.factory.annotation.Value("${app.registration.allowed-domains:gmail.com,cit.edu}")
    private String googleAllowedDomains;

    /**
     * Google sign-in for students only.
     * 1. Only Gmail / cit.edu Google accounts (same domains as sign-up).
     * 2. A new Google user becomes a student; approval follows the student auto-approve switch.
     * 3. An existing student with the same email signs into that same account.
     * 4. Teacher and admin accounts can never sign in with Google.
     * Google has already verified the email address, so no confirmation email is needed.
     */
    public User googleSignIn(GoogleTokenVerifier.GoogleProfile profile) {
        String email = EmailInput.normalize(profile == null ? null : profile.email());
        if (email == null) throw new BadCredentialsException("Invalid Google account");
        String domain = email.substring(email.indexOf('@') + 1);
        boolean allowedDomain = java.util.Arrays.stream(googleAllowedDomains.split(","))
                .map(String::trim).anyMatch(d -> !d.isEmpty() && d.equalsIgnoreCase(domain));
        if (!allowedDomain) throw new IllegalStateException("Google domain not allowed");

        User user = userRepository.findByEmail(email);
        if (user != null) {
            if (!"student".equalsIgnoreCase(user.getRole())) throw new IllegalStateException("Google sign-in students only");
            if (user.getGoogleId() != null && !user.getGoogleId().equals(profile.subject())) throw new BadCredentialsException("Different Google account");
            user.setGoogleId(profile.subject());
            if (!user.isEmailConfirmed()) { user.setEmailConfirmed(true); user.setConfirmationToken(null); }
        } else {
            user = new User();
            user.setEmail(email);
            user.setRole("student");
            user.setGoogleId(profile.subject());
            String fname = TextInput.name(profile.givenName()), lname = TextInput.name(profile.familyName());
            if (fname == null || lname == null) {
                String[] parts = String.valueOf(profile.fullName()).trim().split("\\s+", 2);
                if (fname == null) fname = TextInput.name(parts[0]);
                if (lname == null && parts.length > 1) lname = TextInput.name(parts[1]);
            }
            user.setFname(fname == null ? "JapLearn" : fname);
            user.setLname(lname == null ? "Learner" : lname);
            user.setEmailConfirmed(true);
            user.setApproved(registrationSettings.autoApproveFor("student"));
            // No usable password: the learner signs in with Google (or sets one with "Forgot password").
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString() + UUID.randomUUID()));
        }
        if (!user.isApproved()) { userRepository.save(user); throw new IllegalStateException("User not approved"); }
        LocalDateTime now = LocalDateTime.now(JAPLEARN_TIME_ZONE);
        boolean stillValid = user.getPortalSessionToken() != null && user.getPortalSessionExpiresAt() != null
                && now.isBefore(user.getPortalSessionExpiresAt());
        if (!stillValid) user.setPortalSessionToken(UUID.randomUUID().toString());
        user.setPortalSessionExpiresAt(now.plusDays(StudentAuthorizationService.STUDENT_SESSION_DAYS));
        return userRepository.save(user);
    }

    public User authenticate(String email, String rawPassword) {
        email = EmailInput.normalize(email);
        if (email == null || rawPassword == null || rawPassword.length() > 256) {
            throw new BadCredentialsException("Invalid credentials");
        }
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
    
        // Check the password before revealing any account-state detail.
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        // Self-created teacher accounts must confirm their email too. Student
        // accounts retain the existing JapLearn admin approval requirement.
        if (!"admin".equalsIgnoreCase(user.getRole()) && !user.isEmailConfirmed()) {
            throw new IllegalStateException("Email not confirmed");
        }
        // Students and teachers both need JapLearn admin approval. Teachers can manage
        // classes and see their students, so a self-registered teacher must be vetted first.
        if ("student".equalsIgnoreCase(user.getRole()) || "teacher".equalsIgnoreCase(user.getRole())) {
            if (!user.isApproved()) {
                throw new IllegalStateException("User not approved");
            }
        }

        if ("student".equalsIgnoreCase(user.getRole())) {
            // Keep a still-valid session so a second device does not sign out the first one.
            LocalDateTime now = LocalDateTime.now(JAPLEARN_TIME_ZONE);
            boolean stillValid = user.getPortalSessionToken() != null && user.getPortalSessionExpiresAt() != null
                    && now.isBefore(user.getPortalSessionExpiresAt());
            if (!stillValid) user.setPortalSessionToken(UUID.randomUUID().toString());
            user.setPortalSessionExpiresAt(now.plusDays(StudentAuthorizationService.STUDENT_SESSION_DAYS));
            userRepository.save(user);
        } else if ("teacher".equalsIgnoreCase(user.getRole()) || "admin".equalsIgnoreCase(user.getRole())) {
            user.setPortalSessionToken(UUID.randomUUID().toString());
            // Admin sessions are short (2 hours); teacher sessions last a school day (12 hours).
            boolean admin = "admin".equalsIgnoreCase(user.getRole());
            user.setPortalSessionExpiresAt(LocalDateTime.now(JAPLEARN_TIME_ZONE).plusHours(admin ? 2 : 12));
            userRepository.save(user);
        }
    
        return user;
    }
    

    public User confirmUser(String token) {
        User user = userRepository.findByConfirmationToken(token);
        if (user == null) {
            return null;
        }

        user.setEmailConfirmed(true);
        user.setConfirmationToken(null); // Clear the token after confirmation
        userRepository.save(user);

        return user;
    }
}
