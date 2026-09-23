package japlearn.demo.Service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {
    @Test
    void fortyStudentsCanSignInFromTheSameNetwork() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        for (int i = 0; i < 40; i++) {
            assertThat(limiter.isBlocked("student" + i + "@example.com")).isFalse();
        }
    }

    @Test
    void repeatedFailuresAreLimitedPerEmailAndSuccessClearsThem() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        for (int i = 0; i < 6; i++) limiter.recordFailure(" Learner@Example.com ");
        assertThat(limiter.isBlocked("learner@example.com")).isTrue();
        assertThat(limiter.isBlocked("another@example.com")).isFalse();
        limiter.clear("learner@example.com");
        assertThat(limiter.isBlocked("learner@example.com")).isFalse();
    }
}
