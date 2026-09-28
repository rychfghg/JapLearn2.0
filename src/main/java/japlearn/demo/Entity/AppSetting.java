package japlearn.demo.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** A single named on/off switch the admin controls (for example, student auto-approval). */
@Document(collection = "app_settings")
public class AppSetting {

    @Id
    private String id;
    private boolean enabled;

    public AppSetting() {
    }

    public AppSetting(String id, boolean enabled) {
        this.id = id;
        this.enabled = enabled;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
