package japlearn.demo.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private String id;
    private String fname;
    private String lname;
    private String email;
    private String password;
    private String role;
    private String apiToken;
    private String confirmationToken;
    private boolean isEmailConfirmed = false;
    
    // New field to track if the user has been approved by the teacher
    private boolean isApproved = false;
    private String resetToken;
    private LocalDateTime resetTokenExpiry;
    private int dailyGoalStreak;
    private LocalDate dailyGoalLastCompletedDate;

    public User() {
    }

    public User(String id, String fname, String lname, String email, String password, String role) {
        this.id = id;
        this.fname = fname;
        this.lname = lname;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // Getter and Setter for isApproved
    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }

    public int getDailyGoalStreak() {
        return dailyGoalStreak;
    }

    public void setDailyGoalStreak(int dailyGoalStreak) {
        this.dailyGoalStreak = dailyGoalStreak;
    }

    public LocalDate getDailyGoalLastCompletedDate() {
        return dailyGoalLastCompletedDate;
    }

    public void setDailyGoalLastCompletedDate(LocalDate dailyGoalLastCompletedDate) {
        this.dailyGoalLastCompletedDate = dailyGoalLastCompletedDate;
    }

    // Other Getters and Setters remain unchanged

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @JsonIgnore
    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public boolean isEmailConfirmed() {
        return isEmailConfirmed;
    }

    public void setEmailConfirmed(boolean emailConfirmed) {
        isEmailConfirmed = emailConfirmed;
    }
}
