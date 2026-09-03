package japlearn.demo.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.security.SecureRandom;
import java.util.Base64;

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
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();
    private static final ZoneId JAPLEARN_TIME_ZONE = ZoneId.of("Asia/Manila");

    public String issueApiToken(User user) {
        byte[] bytes = new byte[32];
        TOKEN_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        user.setApiToken(token);
        userRepository.save(user);
        return token;
    }
    @Value("${app.backend-url}")
    private String appBackendUrl;

    @Value("${app.student-web-url}")
    private String studentWebUrl;

    @Value("${app.mail.from-address}")
    private String mailFromAddress;
    
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
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
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        userRepository.save(user);

        // Send reset email
        sendPasswordResetEmail(user.getEmail(), resetToken);

        return "success";
    }

    // Send reset password email
private void sendPasswordResetEmail(String email, String token) {
    String resetUrl = studentWebUrl + "/ResetPassword?token=" + token;

    MimeMessage mimeMessage = mailSender.createMimeMessage();
    try {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(email);
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
    User user = userRepository.findByResetToken(token);
    if (user == null) {
        return "invalid";
    }

    // Encrypt new password
    String encryptedPassword = passwordEncoder.encode(newPassword);
    user.setPassword(encryptedPassword);
    user.setResetToken(null); // Invalidate the token after reset
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

        public synchronized Map<String, Object> getDailyGoalStreak(String email) {
            User user = requireUserByEmail(email);
            LocalDate today = LocalDate.now(JAPLEARN_TIME_ZONE);
            LocalDate lastCompleted = user.getDailyGoalLastCompletedDate();

            if (lastCompleted != null
                    && lastCompleted.isBefore(today.minusDays(1))
                    && user.getDailyGoalStreak() != 0) {
                user.setDailyGoalStreak(0);
                userRepository.save(user);
            }

            return dailyGoalStreakResponse(user, today);
        }

        public synchronized Map<String, Object> completeDailyGoal(String email) {
            User user = requireUserByEmail(email);
            LocalDate today = LocalDate.now(JAPLEARN_TIME_ZONE);
            LocalDate lastCompleted = user.getDailyGoalLastCompletedDate();

            if (!today.equals(lastCompleted)) {
                int nextStreak = today.minusDays(1).equals(lastCompleted)
                        ? user.getDailyGoalStreak() + 1
                        : 1;

                user.setDailyGoalStreak(nextStreak);
                user.setDailyGoalLastCompletedDate(today);
                userRepository.save(user);
            }

            return dailyGoalStreakResponse(user, today);
        }

        private User requireUserByEmail(String email) {
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email is required");
            }

            User user = userRepository.findByEmail(email.trim().toLowerCase());
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }

            return user;
        }

        private Map<String, Object> dailyGoalStreakResponse(User user, LocalDate today) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("streak", user.getDailyGoalStreak());
            response.put(
                    "lastCompletedDate",
                    user.getDailyGoalLastCompletedDate() == null
                            ? ""
                            : user.getDailyGoalLastCompletedDate().toString()
            );
            response.put("completedToday", today.equals(user.getDailyGoalLastCompletedDate()));
            return response;
        }

        public User createManagedUser(Map<String, Object> values) {
            String email = String.valueOf(values.get("email")).trim().toLowerCase();
            if (userRepository.findByEmail(email) != null) throw new IllegalArgumentException("Email already exists");
            User user = new User();
            user.setFname(String.valueOf(values.get("fname")).trim());
            user.setLname(String.valueOf(values.get("lname")).trim());
            user.setEmail(email);
            user.setRole(String.valueOf(values.get("role")).trim().toLowerCase());
            user.setPassword(passwordEncoder.encode(String.valueOf(values.get("password"))));
            user.setEmailConfirmed(Boolean.TRUE.equals(values.get("emailConfirmed")));
            user.setApproved(Boolean.TRUE.equals(values.get("approved")));
            return userRepository.save(user);
        }

        public User updateUser(String userId, Map<String, Object> updates) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (updates.containsKey("fname")) user.setFname(String.valueOf(updates.get("fname")).trim());
            if (updates.containsKey("lname")) user.setLname(String.valueOf(updates.get("lname")).trim());
            if (updates.containsKey("email")) user.setEmail(String.valueOf(updates.get("email")).trim().toLowerCase());
            if (updates.containsKey("role")) user.setRole(String.valueOf(updates.get("role")).trim().toLowerCase());
            if (updates.containsKey("approved")) user.setApproved(Boolean.TRUE.equals(updates.get("approved")));
            if (updates.containsKey("emailConfirmed")) user.setEmailConfirmed(Boolean.TRUE.equals(updates.get("emailConfirmed")));
            if (updates.containsKey("password") && updates.get("password") != null
                    && !String.valueOf(updates.get("password")).isBlank()) {
                user.setPassword(passwordEncoder.encode(String.valueOf(updates.get("password"))));
            }
            User saved = userRepository.save(user);
            Student student = studentRepository.findByEmail(saved.getEmail());
            if (student != null) {
                student.setFname(saved.getFname());
                student.setLname(saved.getLname());
                student.setApproved(saved.isApproved());
                student.setEmailConfirmed(saved.isEmailConfirmed());
                studentRepository.save(student);
            }
            return saved;
        }

        public void deleteUser(String userId) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            Student student = studentRepository.findByEmail(user.getEmail());
            if (student != null) studentRepository.delete(student);
            userRepository.delete(user);
        }

        public String registerUser(User user) {
            try {
                if (userRepository.findByEmail(user.getEmail()) != null) {
                    return "duplicate"; 
                }
                
                String encryptedPassword = passwordEncoder.encode(user.getPassword());
                user.setPassword(encryptedPassword);
                
                // Generate confirmation token
                String confirmationToken = UUID.randomUUID().toString();
                user.setConfirmationToken(confirmationToken);
                
                // Set default email confirmation to false
                user.setEmailConfirmed(false);
                
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
                helper.setTo(email);
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
        
        
        
        
    

    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
    
        // If the user is a student, check both email confirmation and approval status
        if ("student".equals(user.getRole())) {
            if (!user.isEmailConfirmed()) {
                throw new IllegalStateException("Email not confirmed");
            }
            if (!user.isApproved()) {
                throw new IllegalStateException("User not approved");
            }
        }
    
        // Check password regardless of role
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }
    
        return user;
    }
    

    public String confirmUser(String token) {
        User user = userRepository.findByConfirmationToken(token);
        if (user == null) {
            return "invalid";
        }

        user.setEmailConfirmed(true);
        user.setConfirmationToken(null); // Clear the token after confirmation
        userRepository.save(user);

        return "confirmed";
    }
}
