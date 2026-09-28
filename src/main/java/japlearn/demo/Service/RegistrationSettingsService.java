package japlearn.demo.Service;

import org.springframework.stereotype.Service;

import japlearn.demo.Entity.AppSetting;
import japlearn.demo.Repository.AppSettingRepository;

/** Whether new self-registered students are approved automatically. Off by default (manual approval). */
@Service
public class RegistrationSettingsService {

    private static final String STUDENT_AUTO_APPROVE = "studentAutoApprove";

    private final AppSettingRepository settings;

    public RegistrationSettingsService(AppSettingRepository settings) {
        this.settings = settings;
    }

    public boolean isStudentAutoApprove() {
        return settings.findById(STUDENT_AUTO_APPROVE).map(AppSetting::isEnabled).orElse(false);
    }

    public boolean setStudentAutoApprove(boolean enabled) {
        settings.save(new AppSetting(STUDENT_AUTO_APPROVE, enabled));
        return enabled;
    }
}
