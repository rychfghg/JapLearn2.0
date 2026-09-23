package japlearn.demo.Config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

class RateLimitingFilterTest {

    private RateLimitingFilter filter(String secret) {
        RateLimitingFilter filter = new RateLimitingFilter();
        ReflectionTestUtils.setField(filter, "proxySecret", secret);
        return filter;
    }

    private MockHttpServletRequest relayed(String ip, String secret) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/login");
        request.setRemoteAddr("76.76.21.21"); // a Vercel address
        if (ip != null) request.addHeader("X-JapLearn-Client-IP", ip);
        if (secret != null) request.addHeader("X-JapLearn-Proxy-Secret", secret);
        return request;
    }

    @Test void usesTheRelayedClientIpWhenTheSecretMatches() {
        assertThat(filter("s3cret").clientIp(relayed("203.0.113.7", "s3cret"))).isEqualTo("203.0.113.7");
    }

    @Test void ignoresTheRelayedIpWhenTheSecretIsWrongOrMissing() {
        assertThat(filter("s3cret").clientIp(relayed("203.0.113.7", "forged"))).isEqualTo("76.76.21.21");
        assertThat(filter("s3cret").clientIp(relayed("203.0.113.7", null))).isEqualTo("76.76.21.21");
    }

    @Test void neverTrustsTheHeaderWhenNoSecretIsConfigured() {
        assertThat(filter("").clientIp(relayed("203.0.113.7", ""))).isEqualTo("76.76.21.21");
    }

    @Test void oneSharedCarrierIpCanServeFortyLoginRequests() throws Exception {
        RateLimitingFilter limiter = filter("");
        for (int i = 0; i < 40; i++) {
            MockHttpServletRequest request = relayed(null, null);
            MockHttpServletResponse response = new MockHttpServletResponse();
            limiter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
