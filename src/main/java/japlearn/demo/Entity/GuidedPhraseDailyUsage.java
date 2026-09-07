package japlearn.demo.Entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="guided_phrase_daily_usage")
@CompoundIndex(name="guided_usage_user_date", def="{'email':1,'usageDate':1}", unique=true)
public class GuidedPhraseDailyUsage {
    @Id private String id;
    private String email;
    private String usageDate;
    private int secondsUsed;
    private Instant updatedAt=Instant.now();
    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getEmail(){return email;} public void setEmail(String value){email=value;}
    public String getUsageDate(){return usageDate;} public void setUsageDate(String value){usageDate=value;}
    public int getSecondsUsed(){return secondsUsed;} public void setSecondsUsed(int value){secondsUsed=value;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant value){updatedAt=value;}
}
