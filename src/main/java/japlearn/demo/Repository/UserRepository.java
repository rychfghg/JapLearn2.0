package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.User;

public interface UserRepository extends MongoRepository<User, String>{
    User findByEmail(String email);
    List<User> findByFname(String fname);
    User findByConfirmationToken(String confirmationToken);
    List<User> findByIsEmailConfirmedTrueAndIsApprovedFalse();
    User findByResetToken(String resetToken);
}
