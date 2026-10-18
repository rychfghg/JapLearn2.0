package japlearn.demo.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Component;
import japlearn.demo.Entity.SituationalQuestion;
import japlearn.demo.Repository.SituationalQuestionRepository;

/** Runs after question seeders; never overwrites an administrator's media. */
@Component
@ConditionalOnProperty(name="expression-match.media-seed.enabled", havingValue="true", matchIfMissing=true)
public class ExpressionMediaSeeder {
    private static final Logger log=LoggerFactory.getLogger(ExpressionMediaSeeder.class);
    private static final String ROOT="expression-media-seed/";
    private final GridFsTemplate files;
    private final SituationalQuestionRepository questions;
    private final ObjectMapper mapper;
    public ExpressionMediaSeeder(GridFsTemplate files,SituationalQuestionRepository questions,ObjectMapper mapper){
        this.files=files;this.questions=questions;this.mapper=mapper;
    }
    public record MediaRow(String key,int level,String japanese,String romaji,String scenario,
            String secondaryScenario,String hint,String scene,String alternative,String audio) {}

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        try(var manifest=new ClassPathResource(ROOT+"manifest.json").getInputStream()) {
            List<MediaRow> bank=mapper.readValue(manifest,mapper.getTypeFactory().constructCollectionType(List.class,MediaRow.class));
            if(bank.size()!=60)throw new IOException("Expected 60 authored media mappings");
            // Validate the entire pack before any database writes.
            for(var row:bank)for(String filename:List.of(row.scene(),row.alternative(),row.audio()))
                if(!new ClassPathResource(ROOT+filename).exists())throw new IOException("Missing media for "+row.key());
            var existing=questions.findByGameTypeIgnoreCaseOrderByOrderAsc("EXPRESSION_MATCH");
            int updated=0;
            for(var row:bank) {
                for(var question:existing) {
                    if(!matches(question,row))continue;
                    boolean changed=false;
                    if(isPlaceholder(question.getImageUrl())){question.setImageUrl(store(row.scene()));changed=true;}
                    if(isPlaceholder(question.getSecondaryImageUrl())){question.setSecondaryImageUrl(store(row.alternative()));changed=true;}
                    if(question.getAudioUrl()==null||question.getAudioUrl().isBlank()){
                        question.setAudioUrl(store(row.audio()));changed=true;
                    }
                    if(changed){questions.save(question);updated++;}
                }
            }
            log.info("Expression media seed complete: {} question records updated; custom media preserved",updated);
        } catch(IOException | RuntimeException error) {
            // Partial successes are safe to resume; existing bundled APK media remains intact.
            log.error("Expression media seed incomplete; keep bundled assets until migration is verified",error);
        }
    }
    static boolean matches(SituationalQuestion question,MediaRow row){
        return question.getLevel()==row.level()&&question.getSetNumber()==1
            &&Objects.equals(question.getCorrectAnswer(),row.japanese())
            &&Objects.equals(question.getScenario(),row.scenario())
            &&Objects.equals(question.getSecondaryScenario(),row.secondaryScenario());
    }
    static boolean isPlaceholder(String value){
        return value==null||value.isBlank()||value.matches("(?:https?://[^/]+)?/expression-match/gesture-(?:wave|bow)\\.png(?:[?#].*)?");
    }
    private String store(String filename)throws IOException {
        String seedKey="expression-match-v1/"+filename;
        var query=Query.query(Criteria.where("metadata.seedKey").is(seedKey));
        var stored=files.findOne(query);
        if(stored!=null)return "/api/situational/media/"+stored.getObjectId().toHexString();
        String type=filename.endsWith(".webp")?"image/webp":"audio/mpeg";
        try(var input=new ClassPathResource(ROOT+filename).getInputStream()){
            var id=files.store(input,filename,type,new Document("seedKey",seedKey));
            return "/api/situational/media/"+id.toHexString();
        }
    }
}
