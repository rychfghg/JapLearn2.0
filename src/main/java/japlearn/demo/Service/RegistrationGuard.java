package japlearn.demo.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Blocks automated / throw-away sign-ups before an account is created or an email is sent:
 *  - disposable email services (built-in list + BLOCKED_EMAIL_DOMAINS env var)
 *  - domains that cannot receive email (no MX / A record)
 *  - a global ceiling on sign-ups per hour, so a botnet spread over many IPs
 *    cannot burn through the daily email quota.
 */
@Service
public class RegistrationGuard {

    public enum Verdict { OK, DISPOSABLE, BAD_DOMAIN, NOT_ALLOWED, BUSY }

    // Common disposable / temporary inbox providers (including the one seen in the attack).
    private static final Set<String> DISPOSABLE = Set.of(
            "maxxspace.com", "mailinator.com", "guerrillamail.com", "guerrillamail.net", "guerrillamailblock.com", "sharklasers.com",
            "grr.la", "10minutemail.com", "10minutemail.net", "temp-mail.org", "tempmail.com", "tempmail.net", "tempmailo.com",
            "temp-mail.io", "tempmail.dev", "yopmail.com", "yopmail.net", "trashmail.com", "trashmail.de", "getnada.com",
            "nada.email", "dispostable.com", "maildrop.cc", "mohmal.com", "emailondeck.com", "fakeinbox.com", "throwawaymail.com",
            "minuteinbox.com", "mail.tm", "mail.gw", "tempr.email", "discard.email", "spamgourmet.com", "mintemail.com",
            "mytemp.email", "moakt.com", "emailfake.com", "fakemail.net", "burnermail.io", "inboxkitten.com", "luxusmail.org",
            "mailnesia.com", "mailcatch.com", "spambox.us", "tempinbox.com", "33mail.com", "harakirimail.com", "1secmail.com",
            "1secmail.net", "1secmail.org", "esiix.com", "wwjmp.com", "xojxe.com", "yoggm.com", "kzccv.com", "qiott.com",
            "dpptd.com", "rteet.com", "vjuum.com", "laafd.com", "txcct.com", "dcctb.com", "tmpmail.org", "tmpmail.net",
            "emailtemporanea.com", "crazymailing.com", "mailpoof.com", "tmail.ws", "smailpro.com", "cuvox.de", "armyspy.com",
            "dayrep.com", "einrot.com", "fleckens.hu", "gustr.com", "jourrapide.com", "rhyta.com", "superrito.com", "teleworm.us"
    );

    @Value("${app.registration.blocked-domains:}")
    private String extraBlocked = "";

    @Value("${app.registration.max-per-hour:120}")
    private int maxPerHour = 120;

    private final Deque<Long> recent = new ArrayDeque<>();
    private final ConcurrentHashMap<String, Boolean> domainCache = new ConcurrentHashMap<>();

    // Only these email domains can self-register (students AND teachers), enforced on the
    // server so it cannot be skipped by calling the API directly. Comma-separated.
    // Accounts the admin creates in the portal are not affected.
    @Value("${app.registration.allowed-domains:gmail.com,cit.edu}")
    private String allowedDomains = "gmail.com,cit.edu";

    public Verdict check(String email, String role) {
        String domain = domainOf(email);
        if (domain.isEmpty()) return Verdict.BAD_DOMAIN;
        Set<String> allowed = Arrays.stream(allowedDomains.split(",")).map((d) -> d.trim().toLowerCase(Locale.ROOT)).filter((d) -> !d.isEmpty()).collect(Collectors.toSet());
        if (allowed.isEmpty()) allowed = Set.of("gmail.com", "cit.edu"); // never fall back to "any domain"
        if (!allowed.contains(domain)) return Verdict.NOT_ALLOWED;
        if (isDisposable(domain)) return Verdict.DISPOSABLE;
        if (!canReceiveMail(domain)) return Verdict.BAD_DOMAIN;
        if (!takeSlot()) return Verdict.BUSY;
        return Verdict.OK;
    }

    private String domainOf(String email) {
        if (email == null) return "";
        int at = email.lastIndexOf('@');
        return at < 0 ? "" : email.substring(at + 1).trim().toLowerCase(Locale.ROOT);
    }

    private boolean isDisposable(String domain) {
        Set<String> extra = Arrays.stream(extraBlocked.split(","))
                .map((d) -> d.trim().toLowerCase(Locale.ROOT)).filter((d) -> !d.isEmpty()).collect(Collectors.toSet());
        // Match the domain itself and any parent domain (e.g. a.b.maxxspace.com).
        for (String d = domain; d.contains("."); d = d.substring(d.indexOf('.') + 1)) {
            if (DISPOSABLE.contains(d) || extra.contains(d)) return true;
        }
        return false;
    }

    // A domain that has neither MX nor A records cannot receive the confirmation email.
    // DNS failures are treated as "allowed" so an outage never blocks real students.
    private boolean canReceiveMail(String domain) {
        return domainCache.computeIfAbsent(domain, (d) -> {
            try {
                Hashtable<String, String> env = new Hashtable<>();
                env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
                env.put("com.sun.jndi.dns.timeout.initial", "2000");
                env.put("com.sun.jndi.dns.timeout.retries", "1");
                InitialDirContext ctx = new InitialDirContext(env);
                Attributes attrs = ctx.getAttributes(d, new String[] { "MX", "A" });
                Attribute mx = attrs.get("MX"), a = attrs.get("A");
                ctx.close();
                return (mx != null && mx.size() > 0) || (a != null && a.size() > 0);
            } catch (javax.naming.NameNotFoundException e) {
                return false;
            } catch (Exception e) {
                return true;
            }
        });
    }

    private synchronized boolean takeSlot() {
        long now = Instant.now().getEpochSecond();
        while (!recent.isEmpty() && now - recent.peekFirst() > 3600) recent.pollFirst();
        if (recent.size() >= maxPerHour) return false;
        recent.addLast(now);
        return true;
    }
}
