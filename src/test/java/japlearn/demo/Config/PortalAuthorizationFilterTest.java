package japlearn.demo.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.UserRepository;

class PortalAuthorizationFilterTest {
    @Test void rejectsMissingSession() throws Exception {
        PortalAuthorizationFilter filter = new PortalAuthorizationFilter(mock(UserRepository.class));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/users"), response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test void rejectsTeacherFromAdminRoute() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByPortalSessionToken("teacher-token")).thenReturn(user("teacher", "teacher-token"));
        PortalAuthorizationFilter filter = new PortalAuthorizationFilter(repository);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("X-Teacher-Token", "teacher-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test void allowsAdministratorAndCorsPreflight() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByPortalSessionToken("admin-token")).thenReturn(user("admin", "admin-token"));
        PortalAuthorizationFilter filter = new PortalAuthorizationFilter(repository);
        MockHttpServletRequest adminRequest = new MockHttpServletRequest("GET", "/api/users");
        adminRequest.addHeader("X-Portal-Token", "admin-token");
        MockFilterChain adminChain = new MockFilterChain();
        filter.doFilter(adminRequest, new MockHttpServletResponse(), adminChain);
        assertThat(adminChain.getRequest()).isNotNull();

        MockFilterChain optionsChain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("OPTIONS", "/api/users/id"), new MockHttpServletResponse(), optionsChain);
        assertThat(optionsChain.getRequest()).isNotNull();
    }

    @Test void allowsAccountDeletionWithoutAPortalSession() throws Exception {
        // The public deletion page and the in-app delete button are not
        // administrator actions, so they must pass this filter untouched.
        PortalAuthorizationFilter filter = new PortalAuthorizationFilter(mock(UserRepository.class));
        String[][] routes = {
            {"POST", "/api/users/request-account-deletion"},
            {"POST", "/api/users/confirm-account-deletion"},
            {"GET", "/api/users/account-deletion-request"},
            {"DELETE", "/api/users/delete-account"},
        };
        for (String[] route : routes) {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest(route[0], route[1]), response, chain);
            assertThat(chain.getRequest()).as("%s %s should reach the controller", route[0], route[1]).isNotNull();
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test void restrictsStudentClassRoutesToAdministrators() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByPortalSessionToken("teacher-token")).thenReturn(user("teacher", "teacher-token"));
        when(repository.findByPortalSessionToken("admin-token")).thenReturn(user("admin", "admin-token"));
        PortalAuthorizationFilter filter = new PortalAuthorizationFilter(repository);

        MockHttpServletResponse anonymous = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/admin/student-classes"), anonymous, new MockFilterChain());
        assertThat(anonymous.getStatus()).isEqualTo(401);

        MockHttpServletRequest teacherRequest = new MockHttpServletRequest("PUT", "/api/admin/student-classes/abc");
        teacherRequest.addHeader("X-Teacher-Token", "teacher-token");
        MockHttpServletResponse teacher = new MockHttpServletResponse();
        filter.doFilter(teacherRequest, teacher, new MockFilterChain());
        assertThat(teacher.getStatus()).isEqualTo(403);

        MockHttpServletRequest adminRequest = new MockHttpServletRequest("GET", "/api/admin/student-classes");
        adminRequest.addHeader("X-Portal-Token", "admin-token");
        MockFilterChain adminChain = new MockFilterChain();
        filter.doFilter(adminRequest, new MockHttpServletResponse(), adminChain);
        assertThat(adminChain.getRequest()).isNotNull();
    }

    private User user(String role, String token) {
        User user = new User();
        user.setRole(role);
        user.setPortalSessionToken(token);
        user.setPortalSessionExpiresAt(LocalDateTime.now().plusHours(1));
        return user;
    }
}
