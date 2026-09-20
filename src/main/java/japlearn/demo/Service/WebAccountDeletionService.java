package japlearn.demo.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.UserRepository;

/**
 * Public, signed-out account deletion for portal.japlearn.com/delete-account.
 *
 * Learners confirm by email and the deletion happens automatically. Teacher
 * accounts are handed to the JapLearn team instead, because removing a teacher
 * would also orphan their classes and learner records.
 */
@Service
public class WebAccountDeletionService {

    private static final ZoneId JAPLEARN_TIME_ZONE = ZoneId.of("Asia/Manila");
    private static final int DELETION_TOKEN_VALID_MINUTES = 30;

    /** What the public page should tell the visitor next. */
    public enum Outcome { EMAIL_SENT, MANUAL_REVIEW, UNKNOWN_ACCOUNT }

    @Value("${app.portal-web-url:https://portal.japlearn.com}")
    private String portalWebUrl;

    @Value("${app.mail.from-address}")
    private String mailFromAddress;

    @Value("${app.support-email:japlearnofficial@gmail.com}")
    private String supportEmail;

    private final UserRepository users;
    private final AccountDeletionService deletion;
    private final JavaMailSender mailSender;

    public WebAccountDeletionService(UserRepository users, AccountDeletionService deletion, JavaMailSender mailSender) {
        this.users = users;
        this.deletion = deletion;
        this.mailSender = mailSender;
    }

    /** Step 1: email the owner a confirmation link, or ask the team to review. */
    public Outcome requestDeletion(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            return Outcome.UNKNOWN_ACCOUNT;
        }

        User user = users.findByEmail(email);
        if (user == null) {
            return Outcome.UNKNOWN_ACCOUNT;
        }

        if (!"student".equalsIgnoreCase(user.getRole())) {
            // Teacher and admin accounts are reviewed by a person first.
            notifyTeamOfReview(user);
            return Outcome.MANUAL_REVIEW;
        }

        String token = UUID.randomUUID().toString();
        user.setDeletionToken(token);
        user.setDeletionTokenExpiry(LocalDateTime.now(JAPLEARN_TIME_ZONE).plusMinutes(DELETION_TOKEN_VALID_MINUTES));
        users.save(user);

        sendConfirmationEmail(user.getEmail(), token);
        return Outcome.EMAIL_SENT;
    }

    /** Reads a confirmation link, so the page can show whose account it is. */
    public User findByDeletionToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        User user = users.findByDeletionToken(token);
        if (user == null) {
            return null;
        }
        LocalDateTime expiry = user.getDeletionTokenExpiry();
        if (expiry == null || LocalDateTime.now(JAPLEARN_TIME_ZONE).isAfter(expiry)) {
            // Burn an expired link so it cannot be retried.
            user.setDeletionToken(null);
            user.setDeletionTokenExpiry(null);
            users.save(user);
            return null;
        }
        return user;
    }

    /** Step 2: the confirmation link was opened and DELETE was typed. */
    public boolean confirmDeletion(String token, String typedConfirmation) {
        if (!"DELETE".equalsIgnoreCase(typedConfirmation == null ? "" : typedConfirmation.trim())) {
            return false;
        }
        User user = findByDeletionToken(token);
        if (user == null || !"student".equalsIgnoreCase(user.getRole())) {
            return false;
        }
        deletion.deleteLearnerAccount(user.getEmail());
        return true;
    }

    private void sendConfirmationEmail(String email, String token) {
        String confirmUrl = portalWebUrl + "/delete-account/confirm?token=" + token;
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setFrom("JapLearn <" + mailFromAddress + ">");
            helper.setSubject("Confirm account deletion - JapLearn");
            helper.setText(
                    "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:auto;padding:24px;color:#2f1a3a\">"
                            + "<p style=\"color:#6ea63f;font-size:12px;font-weight:bold;letter-spacing:1.4px;margin:0\">ACCOUNT DELETION</p>"
                            + "<h1 style=\"font-size:22px;margin:10px 0 12px\">Confirm you want to delete your JapLearn account</h1>"
                            + "<p style=\"color:#6b5a74;line-height:1.6;margin:0 0 8px\">Someone asked to delete the JapLearn account for <b>"
                            + escape(email) + "</b>.</p>"
                            + "<p style=\"color:#6b5a74;line-height:1.6;margin:0 0 18px\">This permanently removes your account, lesson progress, badges, game scores and speaking feedback. It cannot be undone.</p>"
                            + "<p style=\"margin:0 0 18px\"><a href=\"" + confirmUrl
                            + "\" style=\"display:inline-block;background:#c53d47;color:#fff;text-decoration:none;padding:13px 22px;border-radius:12px;font-weight:bold\">Confirm deletion</a></p>"
                            + "<p style=\"color:#8a7d90;font-size:13px;line-height:1.6;margin:0\">This link expires in "
                            + DELETION_TOKEN_VALID_MINUTES
                            + " minutes. If you did not ask for this, ignore this email and your account stays exactly as it is.</p>"
                            + "</div>",
                    true);
            helper.addInline("japlearnLogo", new ClassPathResource("mail/japlearn-logo.png"));
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send the account deletion email", e);
        }
    }

    /** Teacher or admin deletions, and anyone who lost access to their inbox. */
    private void notifyTeamOfReview(User user) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(supportEmail);
            helper.setFrom("JapLearn <" + mailFromAddress + ">");
            helper.setReplyTo(user.getEmail());
            helper.setSubject("Account deletion review - " + user.getEmail());
            helper.setText(
                    "<div style=\"font-family:Arial,sans-serif;color:#2f1a3a\">"
                            + "<h2 style=\"font-size:18px\">Account deletion request needing review</h2>"
                            + "<p><b>Email:</b> " + escape(user.getEmail()) + "<br>"
                            + "<b>Name:</b> " + escape(String.valueOf(user.getFname()) + " " + String.valueOf(user.getLname())) + "<br>"
                            + "<b>Role:</b> " + escape(String.valueOf(user.getRole())) + "</p>"
                            + "<p>This account is not a learner account, so it was not deleted automatically. "
                            + "Check the classes and learner records attached to it before removing it.</p>"
                            + "</div>",
                    true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send the account deletion review email", e);
        }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
