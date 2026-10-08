package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.SmartSurveyUpload;

public interface SmartSurveyUploadRepository extends MongoRepository<SmartSurveyUpload, String> {
    List<SmartSurveyUpload> findAllByOrderByUploadedAtDesc();
}
