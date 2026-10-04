package japlearn.demo.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import japlearn.demo.Service.StudentAuthorizationService;

class StudentOwnershipFilterTest {

    private MockHttpServletRequest scoreSave(String email, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/scores/save");
        request.setContentType("application/json");
        request.setContent(("{\"email\":\"" + email + "\",\"score\":5}").getBytes(StandardCharsets.UTF_8));
        if (token != null) request.addHeader("X-Student-Token", token);
        return request;
    }

    @Test
    void ownTokenPassesAndBodyIsStillReadable() throws Exception {
        StudentAuthorizationService auth = mock(StudentAuthorizationService.class);
        StudentOwnershipFilter filter = new StudentOwnershipFilter(auth);
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(scoreSave("ana@gmail.com", "good"), new MockHttpServletResponse(), chain);

        verify(auth).requireOwnStudent("ana@gmail.com", "good");
        // The controller must still be able to read the full JSON body.
        String body = new String(chain.getRequest().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(body).contains("\"score\":5");
    }

    @Test
    void someoneElsesTokenIsRejected() throws Exception {
        StudentAuthorizationService auth = mock(StudentAuthorizationService.class);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(auth).requireOwnStudent(eq("victim@gmail.com"), anyString());
        StudentOwnershipFilter filter = new StudentOwnershipFilter(auth);
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(scoreSave("victim@gmail.com", "attacker-token"), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void oldAppWithoutTokenStillWorksInTransitionMode() throws Exception {
        StudentAuthorizationService auth = mock(StudentAuthorizationService.class); // lenient: does not throw
        StudentOwnershipFilter filter = new StudentOwnershipFilter(auth);
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(scoreSave("ana@gmail.com", null), new MockHttpServletResponse(), chain);

        verify(auth).requireOwnStudent(eq("ana@gmail.com"), isNull());
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void unrelatedRoutesAreUntouched() throws Exception {
        StudentAuthorizationService auth = mock(StudentAuthorizationService.class);
        StudentOwnershipFilter filter = new StudentOwnershipFilter(auth);
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/health"), new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).isNotNull();
    }
}
