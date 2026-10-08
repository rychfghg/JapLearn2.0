package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.SmartCommunicationAssignment;

public interface SmartCommunicationAssignmentRepository extends MongoRepository<SmartCommunicationAssignment, String> {
    List<SmartCommunicationAssignment> findAllByOrderByDueAtDesc();
}
