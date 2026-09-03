package japlearn.demo.Service;

import java.time.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import japlearn.demo.Entity.*;
import japlearn.demo.Repository.*;

@Service
public class GuidedPracticeClockService {
 public static final int DAILY_LIMIT_SECONDS=1200; private static final ZoneId ZONE=ZoneId.of("Asia/Manila");
 private final QuackTalkSessionRepository sessions; private final GuidedPracticeUsageRepository usage;
 public GuidedPracticeClockService(QuackTalkSessionRepository sessions,GuidedPracticeUsageRepository usage){this.sessions=sessions;this.usage=usage;}
 @Scheduled(fixedDelay=10000) public synchronized void persistActiveTime(){for(QuackTalkSession s:sessions.findByRoomTypeAndStatus("GUIDED_PHRASE","ACTIVE"))bill(s);}
 public void bill(QuackTalkSession s){Instant now=Instant.now();Instant started=s.getStartedAt();if(started==null)return;LocalDate today=LocalDate.now(ZONE);if(!started.atZone(ZONE).toLocalDate().equals(today)){s.setStatus("ENDED");s.setEndedAt(now);sessions.save(s);return;}GuidedPracticeUsage d=usage.findByEmailIgnoreCaseAndDate(s.getEmail(),today).orElseGet(()->{GuidedPracticeUsage u=new GuidedPracticeUsage();u.setEmail(s.getEmail().toLowerCase());u.setDate(today);return u;});int wallSeconds=(int)Math.min(DAILY_LIMIT_SECONDS,Math.max(0,Duration.between(started,now).getSeconds()));int increment=Math.max(0,Math.min(wallSeconds-s.getDurationSeconds(),DAILY_LIMIT_SECONDS-d.getSecondsUsed()));d.setSecondsUsed(d.getSecondsUsed()+increment);usage.save(d);s.setDurationSeconds(s.getDurationSeconds()+increment);s.setUsageRecordedAt(now);if(d.getSecondsUsed()>=DAILY_LIMIT_SECONDS){s.setStatus("ENDED");s.setEndedAt(now);}sessions.save(s);}
}
