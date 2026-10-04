package japlearn.demo.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import japlearn.demo.Entity.Student;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.UserRepository;
import japlearn.demo.Service.StudentAuthorizationService;
import japlearn.demo.Service.StudentService;

class StudentOwnershipFilterTest {

    private final StudentAuthorizationService auth = mock(StudentAuthorizationService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final StudentService studentService = mock(StudentService.class);
    private final StudentOwnershipFilter filter = new StudentOwnershipFilter(auth, users, studentService);

    private MockHttpServletRequest scoreSave(String email, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/scores/save");
        request.setContentType("application/json");
        request.setContent(("{\"email\":\"" + email + "\",\"score\":5}").getBytes(StandardCharsets.UTF_8));
        if (token != null) request.addHeader("X-Student-Token", token);
        return request;
    }

    private MockHttpServletRequest scoreRead(String email) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/scores/student");
        request.setParameter("email", email);
        return request;
    }

    private void staff(String token, String role, String email) {
        User u = new User();
        u.setRole(role); u.setEmail(email); u.setApproved(true);
        u.setPortalSessionToken(token);
        u.setPortalSessionExpiresAt(LocalDateTime.now(ZoneId.of("Asia/Manila")).plusHours(1));
        when(users.findByPortalSessionToken(token)).thenReturn(u);
    }

    @Test
    void ownTokenPassesAndBodyIsStillReadable() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(scoreSave("ana@gmail.com", "good"), new MockHttpServletResponse(), chain);
        verify(auth).requireOwnStudent("ana@gmail.com", "good", false);
        String body = new String(chain.getRequest().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(body).contains("\"score\":5");
    }

    @Test
    void someoneElsesTokenIsRejected() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(auth).requireOwnStudent(eq("victim@gmail.com"), anyString(), anyBoolean());
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(scoreSave("victim@gmail.com", "attacker-token"), response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void readsAlwaysRequireTheStudentsToken() throws Exception {
        MockHttpServletRequest request = scoreRead("victim@gmail.com");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        verify(auth).requireOwnStudent(eq("victim@gmail.com"), isNull(), eq(true)); // mandatory for reads
    }

    @Test
    void adminCanReadAnyStudent() throws Exception {
        staff("admin-token", "admin", "boss@gmail.com");
        MockHttpServletRequest request = scoreRead("ana@gmail.com");
        request.addHeader("X-Portal-Token", "admin-token");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void teacherReadsOnlyOwnClassStudents() throws Exception {
        staff("t-token", "teacher", "sensei@gmail.com");
        Student mine = new Student(); mine.setEmail("ana@gmail.com");
        when(studentService.getStudentsForTeacher("sensei@gmail.com")).thenReturn(List.of(mine));

        MockHttpServletRequest own = scoreRead("ana@gmail.com"); own.addHeader("X-Portal-Token", "t-token");
        MockFilterChain okChain = new MockFilterChain();
        filter.doFilter(own, new MockHttpServletResponse(), okChain);
        assertThat(okChain.getRequest()).isNotNull();

        MockHttpServletRequest other = scoreRead("someone@gmail.com"); other.addHeader("X-Portal-Token", "t-token");
        MockFilterChain blocked = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(other, response, blocked);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(blocked.getRequest()).isNull();
    }

    @Test
    void fakeStaffTokenIsRejected() throws Exception {
        MockHttpServletRequest request = scoreRead("ana@gmail.com");
        request.addHeader("X-Portal-Token", "made-up");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void unrelatedRoutesAreUntouched() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/health"), new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).isNotNull();
    }
}
