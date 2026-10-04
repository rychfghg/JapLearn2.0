package japlearn.demo.Service;

import org.springframework.stereotype.Service;

import japlearn.demo.Entity.AppSetting;
import japlearn.demo.Repository.AppSettingRepository;

/** Whether new self-registered students / teachers are approved automatically. Both off by default (manual approval). */
@Service
public class RegistrationSettingsService {

    private static final String STUDENT_AUTO_APPROVE = "studentAutoApprove";
    private static final String TEACHER_AUTO_APPROVE = "teacherAutoApprove";

    private final AppSettingRepository settings;

    public RegistrationSettingsService(AppSettingRepository settings) {
        this.settings = settings;
    }

    private boolean get(String key) {
        return settings.findById(key).map(AppSetting::isEnabled).orElse(false);
    }

    private boolean set(String key, boolean enabled) {
        settings.save(new AppSetting(key, enabled));
        return enabled;
    }

    public boolean isStudentAutoApprove() { return get(STUDENT_AUTO_APPROVE); }
    public boolean setStudentAutoApprove(boolean enabled) { return set(STUDENT_AUTO_APPROVE, enabled); }

    public boolean isTeacherAutoApprove() { return get(TEACHER_AUTO_APPROVE); }
    public boolean setTeacherAutoApprove(boolean enabled) { return set(TEACHER_AUTO_APPROVE, enabled); }

    /** Auto-approval for a self-registering account of the given role (admins never self-register). */
    public boolean autoApproveFor(String role) {
        if ("student".equalsIgnoreCase(role)) return isStudentAutoApprove();
        if ("teacher".equalsIgnoreCase(role)) return isTeacherAutoApprove();
        return false;
    }
}
