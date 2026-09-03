package japlearn.demo.Entity;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
@Document(collection="guided_practice_usage")
@CompoundIndex(name="guided_usage_user_day", def="{'email':1,'date':1}", unique=true)
public class GuidedPracticeUsage {
 @Id private String id; private String email; private LocalDate date; private int secondsUsed;
 public String getId(){return id;} public void setId(String v){id=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;}
 public LocalDate getDate(){return date;} public void setDate(LocalDate v){date=v;} public int getSecondsUsed(){return secondsUsed;} public void setSecondsUsed(int v){secondsUsed=v;}
}
