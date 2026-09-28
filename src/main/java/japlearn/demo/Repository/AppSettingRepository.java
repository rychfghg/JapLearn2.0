package japlearn.demo.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.AppSetting;

public interface AppSettingRepository extends MongoRepository<AppSetting, String> {
}
