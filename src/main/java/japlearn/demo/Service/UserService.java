package japlearn.demo.Service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${app.base-url}")
    private String appBaseUrl;
    
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
    // Change the reset URL to use localhost
    String resetUrl = appBaseUrl + "/ResetPassword?token=" + token;

    MimeMessage mimeMessage = mailSender.createMimeMessage();
    try {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(email);
        helper.setFrom("JapLearn <mizuchwaan@gmail.com>"); // Use the same sender details
        helper.setSubject("Reset Password - JapLearn");

        // HTML email content (following the same design as confirmation email)
        String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 20px auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;'>"
                           + "<h2 style='text-align: center; color: #333;'>Password Reset Request</h2>"
                           + "<p style='text-align: center; color: #555;'>You have requested to reset your password. Please click the button below to reset your password:</p>"
                           + "<div style='text-align: center; margin: 30px;'>"
                           + "  <a href='" + resetUrl + "' style='background-color: #4CAF50; color: white; padding: 12px 20px; text-decoration: none; font-size: 16px; border-radius: 5px;'>Reset Password</a>"
                           + "</div>"
                           + "<p style='text-align: center; color: #777;'>If you did not request this, please ignore this email.</p>"
                           + "<p style='text-align: center; color: #777;'>Thank you, <br> The JapLearn Team</p>"
                           + "</div>";

        helper.setText(htmlContent, true); // Set 'true' to send HTML content
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
            String confirmationUrl = appBaseUrl+ "/api/users/confirm?token=" + token;
        
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            try {
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.setTo(email);
                helper.setFrom("JapLearn <mizuchwaan@gmail.com>");
                helper.setSubject("Email Confirmation - JapLearn");
        
                // Hosted image URL
                String hostedImageUrl = "https://Unnivyu.github.io/Japlearn-1/assets/svg/jpLogo.svg";
        
                // HTML email content
                String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 20px auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;'>"
                                   + "  <h2 style='text-align: center; color: #333;'>Welcome to JapLearn!</h2>"
                                   + "  <div style='text-align: center; margin: 20px auto;'>"
                                   + "    <img src='" + hostedImageUrl + "' alt='JapLearn Mascot' style='max-width: 150px; height: auto;' />"
                                   + "  </div>"
                                   + "  <p style='text-align: center; color: #555;'>Please confirm your email address by clicking the button below:</p>"
                                   + "  <div style='text-align: center; margin: 30px;'>"
                                   + "    <a href='" + confirmationUrl + "' style='background-color: #4CAF50; color: white; padding: 12px 20px; text-decoration: none; font-size: 16px; border-radius: 5px;'>Confirm Email</a>"
                                   + "  </div>"
                                   + "  <p style='text-align: center; color: #777;'>If you did not request this, please ignore this email.</p>"
                                   + "  <p style='text-align: center; color: #777;'>Thank you, <br> The JapLearn Team</p>"
                                   + "</div>";
        
                helper.setText(htmlContent, true); // Set 'true' to send HTML content
        
                mailSender.send(mimeMessage);
            } catch (MessagingException e) {
                e.printStackTrace();  // Log the exception
                throw new RuntimeException("Failed to send email", e);
            }
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
