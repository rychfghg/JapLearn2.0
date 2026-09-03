package japlearn.demo.Service;

import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.UserRepository;

@Service
public class ApiAccessService {
 private final UserRepository users;
 public ApiAccessService(UserRepository users){this.users=users;}
 public User require(String authorization,String email,String...roles){
  if(authorization==null||!authorization.startsWith("Bearer "))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sign in is required.");
  User user=users.findByApiToken(authorization.substring(7).trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Your sign-in has expired."));
  boolean elevated=Arrays.stream(roles).anyMatch(r->r.equalsIgnoreCase(user.getRole()));
  if(email!=null&&!email.equalsIgnoreCase(user.getEmail())&&!elevated)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot access another learner's practice.");
  return user;
 }
}
