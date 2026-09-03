package japlearn.demo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.User;

public interface UserRepository extends MongoRepository<User, String>{
    Optional<User> findByApiToken(String apiToken);
    User findByEmail(String email);
    List<User> findByFname(String fname);
    User findByConfirmationToken(String confirmationToken);
    List<User> findByIsEmailConfirmedTrueAndIsApprovedFalse();
    User findByResetToken(String resetToken);
    List<User> findByRoleIgnoreCase(String role);
}
