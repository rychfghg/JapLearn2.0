package japlearn.demo.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Document(collection = "users")
public class User {

    @Id
    private String id;
    private String fname;
    private String lname;
    @Indexed
    private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String role;
    @JsonIgnore
    private String confirmationToken;
    private boolean isEmailConfirmed = false;
    
    // New field to track if the user has been approved by the teacher
    private boolean isApproved = false;
    @JsonIgnore
    private String resetToken;
    @JsonIgnore
    private LocalDateTime resetTokenExpiry;
    private int dailyGoalStreak;
    private LocalDate dailyGoalLastCompletedDate;
    private LocalDate dailyGoalMinutesDate;
    private int dailyGoalMinutes;
    private boolean guidedPhraseEnabled = false;
    // Looked up on every authenticated portal request.
    @Indexed
    @JsonIgnore
    private String portalSessionToken;
    @JsonIgnore
    private LocalDateTime portalSessionExpiresAt;
    // Confirms a web account-deletion request made from the public page.
    @JsonIgnore
    private String deletionToken;
    @JsonIgnore
    private LocalDateTime deletionTokenExpiry;

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

    public LocalDate getDailyGoalMinutesDate() { return dailyGoalMinutesDate; }
    public void setDailyGoalMinutesDate(LocalDate dailyGoalMinutesDate) { this.dailyGoalMinutesDate = dailyGoalMinutesDate; }
    public int getDailyGoalMinutes() { return dailyGoalMinutes; }
    public void setDailyGoalMinutes(int dailyGoalMinutes) { this.dailyGoalMinutes = dailyGoalMinutes; }

    public boolean isGuidedPhraseEnabled() { return guidedPhraseEnabled; }
    public void setGuidedPhraseEnabled(boolean guidedPhraseEnabled) { this.guidedPhraseEnabled = guidedPhraseEnabled; }
    public String getPortalSessionToken() { return portalSessionToken; }
    public void setPortalSessionToken(String portalSessionToken) { this.portalSessionToken = portalSessionToken; }
    public LocalDateTime getPortalSessionExpiresAt() { return portalSessionExpiresAt; }
    public void setPortalSessionExpiresAt(LocalDateTime portalSessionExpiresAt) { this.portalSessionExpiresAt = portalSessionExpiresAt; }
    public String getDeletionToken() { return deletionToken; }
    public void setDeletionToken(String deletionToken) { this.deletionToken = deletionToken; }
    public LocalDateTime getDeletionTokenExpiry() { return deletionTokenExpiry; }
    public void setDeletionTokenExpiry(LocalDateTime deletionTokenExpiry) { this.deletionTokenExpiry = deletionTokenExpiry; }

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
