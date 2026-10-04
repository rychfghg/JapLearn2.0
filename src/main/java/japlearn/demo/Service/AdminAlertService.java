package japlearn.demo.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Emails the JapLearn owner when the admin area is opened or an admin sign-in
 * is attempted. Alerts never contain passwords or what was typed.
 * Throttled (per event + IP, and overall) so nobody can flood the inbox or the
 * daily email quota by triggering alerts.
 */
@Service
public class AdminAlertService {
    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final long PER_IP_COOLDOWN_SECONDS = 600;
    private static final int MAX_PER_HOUR = 30;

    private final JavaMailSender mailSender;
    private final Map<String, Long> lastSent = new ConcurrentHashMap<>();
    private final Deque<Long> recent = new ArrayDeque<>();

    // Set ADMIN_ALERT_EMAIL on Render. If it is empty, no alerts are sent.
    @Value("${app.admin-alert-email:}")
    private String alertEmail;

    @Value("${app.mail.from-address}")
    private String mailFromAddress;

    @Value("${app.proxy-shared-secret:}")
    private String proxySecret = "";

    @Value("${app.admin-alerts-enabled:true}")
    private boolean enabled = true;

    public AdminAlertService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public enum Event {
        PAGE_OPENED("Admin page opened", "Someone opened the JapLearn admin area."),
        FAILED_SIGN_IN("Failed admin sign-in", "Someone tried to sign in to an admin account and failed."),
        SIGNED_IN("Admin signed in", "Someone signed in to a JapLearn admin account successfully."),
        RESET_ATTEMPT("Admin password reset attempted", "Someone requested a password reset for an admin account. It was blocked: admin passwords cannot be reset by email.");

        final String title, summary;
        Event(String title, String summary) { this.title = title; this.summary = summary; }
    }

    /** Records the alert and sends it in the background (never slows the request). */
    public void alert(Event event, HttpServletRequest request, String page) {
        if (!enabled || alertEmail == null || alertEmail.isBlank()) return;
        String ip = clientIp(request);
        String agent = trim(request.getHeader("User-Agent"), 180);
        if (!allow(event.name() + "|" + ip)) return;
        String when = ZonedDateTime.now(MANILA).format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm:ss a"));
        String safePage = page == null ? "" : trim(page.replaceAll("[^a-zA-Z0-9/_-]", ""), 60);
        CompletableFuture.runAsync(() -> send(event, ip, agent, when, safePage));
    }

    private synchronized boolean allow(String key) {
        long now = Instant.now().getEpochSecond();
        Long last = lastSent.get(key);
        if (last != null && now - last < PER_IP_COOLDOWN_SECONDS) return false;
        while (!recent.isEmpty() && now - recent.peekFirst() > 3600) recent.pollFirst();
        if (recent.size() >= MAX_PER_HOUR) return false;
        recent.addLast(now);
        lastSent.put(key, now);
        if (lastSent.size() > 5000) lastSent.entrySet().removeIf((e) -> now - e.getValue() > PER_IP_COOLDOWN_SECONDS);
        return true;
    }

    private void send(Event event, String ip, String agent, String when, String page) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(EmailInput.recipient(alertEmail));
            helper.setFrom("JapLearn Security <" + mailFromAddress + ">");
            helper.setSubject("[JapLearn security] " + event.title);
            String color = event == Event.SIGNED_IN ? "#2f7a1c" : (event == Event.FAILED_SIGN_IN || event == Event.RESET_ATTEMPT) ? "#c0392b" : "#8423d9";
            helper.setText("<div style=\"font-family:Arial,sans-serif;color:#2f1a3a;max-width:520px\">"
                    + "<h2 style=\"font-size:18px;color:" + color + ";margin:0 0 8px\">" + event.title + "</h2>"
                    + "<p style=\"margin:0 0 14px\">" + event.summary + "</p>"
                    + "<table style=\"font-size:14px;border-collapse:collapse\">"
                    + row("When", when + " (Manila)")
                    + (page.isEmpty() ? "" : row("Page", "portal.japlearn.com" + escape(page)))
                    + row("IP address", escape(ip))
                    + row("Device / browser", escape(agent))
                    + "</table>"
                    + "<p style=\"font-size:13px;color:#7b6a86;margin-top:16px\">If this was you, no action is needed. "
                    + "If not, change the admin password right away and review the admin account.</p></div>", true);
            mailSender.send(message);
        } catch (Exception ignored) {
            // An alert failing must never affect sign-in.
        }
    }

    private static String row(String k, String v) {
        return "<tr><td style=\"padding:4px 14px 4px 0;color:#7b6a86\">" + k + "</td><td style=\"padding:4px 0\"><b>" + v + "</b></td></tr>";
    }

    private static String trim(String s, int max) {
        if (s == null) return "unknown";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // Same rule as RateLimitingFilter: trust the portal proxy's client IP only with the shared secret.
    private String clientIp(HttpServletRequest request) {
        String proxied = request.getHeader("X-JapLearn-Client-IP"), secret = request.getHeader("X-JapLearn-Proxy-Secret");
        if (proxySecret != null && !proxySecret.isBlank() && proxied != null && !proxied.isBlank() && secret != null
                && MessageDigest.isEqual(proxySecret.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8))) {
            return proxied.trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }
}
