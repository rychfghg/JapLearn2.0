package japlearn.demo.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import japlearn.demo.Entity.*;
import japlearn.demo.Repository.*;

/** Activity history is not mastery: failed, timed and unfinished work remains visible. */
@Service
public class LearningHistoryService {
    private final SituationalRunRepository runs;
    private final ResponseRushProgressRepository rush;
    private final DialogueRelayProgressRepository relay;
    public LearningHistoryService(SituationalRunRepository runs, ResponseRushProgressRepository rush,
            DialogueRelayProgressRepository relay) { this.runs=runs; this.rush=rush; this.relay=relay; }

    public List<Map<String,Object>> history(String email, List<SituationalAttempt> situations,
            List<ReplyCoachAttempt> replies, List<QuackTalkSession> talks, List<Score> scores) {
        List<Map<String,Object>> rows=new ArrayList<>();
        Map<String,Instant> finished=new HashMap<>();
        for (var a:situations) {
            String kind=a.getGameType(), status=a.isCompleted()?"COMPLETED":
                    "GAME_OVER".equals(a.getEndReason())?"FAILED":"IN_PROGRESS";
            var entry=row("situational:"+a.getId(), a.getClientAttemptId(), label(kind), kind, status,
                    (int)Math.round(a.getAccuracy()), a.getScore(), a.getMaxScore(), a.getCompletedAt(), a.getAnsweredCount());
            entry.put("level",a.getLevel());rows.add(entry);
            String finishedKey="EXPRESSION_MATCH".equals(kind)?kind+"_LEVEL_"+a.getLevel():kind;
            if (!status.equals("IN_PROGRESS")) finished.merge(finishedKey,time(a.getCompletedAt()),(x,y)->x.isAfter(y)?x:y);
        }
        for(var a:replies) rows.add(row("reply:"+a.getId(),null,"Reply Coach · "+Objects.toString(a.getChapterTitle(),"Chapter"),
                "REPLY_COACH", Objects.toString(a.getStatus(),"IN_PROGRESS"),
                "COMPLETED".equalsIgnoreCase(a.getStatus())?Integer.valueOf(a.getFinalPercentage()):percent(a.getScore(),a.getMaximumScore()),
                a.getScore(),a.getMaximumScore(),a.getCompletedAt()!=null?a.getCompletedAt():a.getUpdatedAt(),null));
        for(var a:talks) rows.add(row("talk:"+a.getId(),null,"QuackTalk · "+("GUIDED_PHRASE".equals(a.getRoomType())?"Guided Phrase":"Talk with Sumi"),
                a.getRoomType(),a.isCompleted()?"COMPLETED":"IN_PROGRESS",a.isEvaluated()?a.getScore():null,
                a.isEvaluated()?a.getScore():null,a.isEvaluated()?100:null,a.getPracticedAt(),a.getConversationTurns()));
        for(var a:scores) {
            Instant at=scoreTime(a);
            Map<String,Object> item=row("score:"+a.getId(),a.getClientAttemptId(),label(a.getGame()),a.getGame(),
                    a.isCompleted()?"COMPLETED":"IN_PROGRESS",percent(a.getScore(),a.getMaxScore()),a.getScore(),a.getMaxScore(),at,a.getTotalQuestions());
            item.put("mode",Objects.toString(a.getMode(),""));
            item.put("timed",ReplyRushEvidence.isTimed(a));
            rows.add(item);
            if(a.isCompleted())finished.merge(a.getGame(),at,(x,y)->x.isAfter(y)?x:y);
        }
        for(var run:runs.findByEmailIgnoreCase(email)) {
            String kind=run.getGameType();
            if(run.getQuestionIndex()<=0 && run.getCorrectCount()<=0 && run.getEasyMistakes()+run.getHardMistakes()<=0)continue;
            String finalKind=kind!=null&&kind.startsWith("EXPRESSION_MATCH_LEVEL_")?"EXPRESSION_MATCH":kind;
            if(!time(run.getUpdatedAt()).isAfter(finished.getOrDefault(kind,Instant.EPOCH)))continue;
            rows.add(row("run:"+run.getId(),null,label(finalKind),kind,"IN_PROGRESS",null,null,null,run.getUpdatedAt(),run.getQuestionIndex()));
        }
        rush.findByEmailIgnoreCase(email).filter(a->!a.isCompleted()&&a.getAnswers()!=null&&!a.getAnswers().isEmpty())
            .filter(a->time(a.getUpdatedAt()).isAfter(finished.getOrDefault("QUACKRESPONSE_RUSH",Instant.EPOCH)))
            .ifPresent(a->{var item=row("rush-run:"+a.getId(),a.getRunId(),"Response Rush","QUACKRESPONSE_RUSH","IN_PROGRESS",null,null,null,a.getUpdatedAt(),a.getAnswers().size());item.put("mode",Objects.toString(a.getMode(),""));rows.add(item);});
        relay.findByEmailIgnoreCase(email).filter(a->a.getAnswered()>0)
            .filter(a->time(a.getUpdatedAt()).isAfter(finished.getOrDefault("QUACKRESPONSE_RELAY",Instant.EPOCH)))
            .ifPresent(a->rows.add(row("relay-run:"+a.getId(),null,"Dialogue Relay","QUACKRESPONSE_RELAY","IN_PROGRESS",null,null,null,a.getUpdatedAt(),a.getAnswered())));
        rows.sort(Comparator.comparing((Map<String,Object> a)->Instant.parse(String.valueOf(a.get("playedAt")))).reversed());
        return rows;
    }
    private static Map<String,Object> row(String id,String client,String title,String game,String status,Integer percentage,Integer raw,Integer max,Instant at,Integer answered) {
        Map<String,Object> r=new LinkedHashMap<>();r.put("id",id);r.put("clientAttemptId",client);r.put("title",title);r.put("gameType",game);
        r.put("status",status);r.put("score",percentage);r.put("rawScore",raw);r.put("maxScore",max);r.put("playedAt",time(at).toString());r.put("answeredCount",answered);return r;
    }
    private static Instant time(Instant value){return value==null?Instant.EPOCH:value;}
    private static Integer percent(int value,int max){return max>0?Math.max(0,Math.min(100,(int)Math.round(value*100.0/max))):null;}
    private static Instant scoreTime(Score a){try{return Instant.parse(a.getDate());}catch(Exception ignored){}if(a.getId()!=null&&ObjectId.isValid(a.getId()))return new ObjectId(a.getId()).getDate().toInstant();try{return LocalDate.parse(a.getDate()).atStartOfDay().toInstant(ZoneOffset.UTC);}catch(Exception ignored){return Instant.EPOCH;}}
    private static String label(String game){return switch(Objects.toString(game,"").toUpperCase(Locale.ROOT)){case "RECOGNITION"->"Ahiru Rescue";case "EXPRESSION_MATCH"->"Expression Match";case "POLITENESS"->"Politeness";case "QUACKRESPONSE_RUSH"->"Response Rush";case "QUACKRESPONSE_RELAY"->"Dialogue Relay";case "QUACKAMOLE"->"Quack-a-Mole";case "QUACKMAN"->"Quackman";case "QUACKSLATE"->"QuackSlate";default->Objects.toString(game,"Activity");};}
}
