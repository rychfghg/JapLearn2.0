package japlearn.demo.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.gridfs.model.GridFSFile;
import japlearn.demo.Entity.SituationalQuestion;
import japlearn.demo.Repository.SituationalQuestionRepository;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExpressionMediaSeederTest {
    private SituationalQuestion question(){
        var q=new SituationalQuestion();q.setGameType("EXPRESSION_MATCH");q.setLevel(1);q.setSetNumber(1);
        q.setCorrectAnswer("ただいま");q.setScenario("You return home and greet your family.");
        q.setSecondaryScenario("You meet someone for the first time.");return q;
    }
    @Test void fillsPlaceholdersAndPreservesThemOnRerun(){
        var files=mock(GridFsTemplate.class);var repo=mock(SituationalQuestionRepository.class);
        var existing=mock(GridFSFile.class);var id=new ObjectId();when(existing.getObjectId()).thenReturn(id);
        when(files.findOne(any(Query.class))).thenReturn(existing);
        var q=question();q.setImageUrl("/expression-match/gesture-wave.png");q.setSecondaryImageUrl("https://japlearn2-0.onrender.com/expression-match/gesture-bow.png");
        when(repo.findByGameTypeIgnoreCaseOrderByOrderAsc("EXPRESSION_MATCH")).thenReturn(List.of(q));
        var seed=new ExpressionMediaSeeder(files,repo,new ObjectMapper());seed.seed();seed.seed();
        assertEquals("/api/situational/media/"+id,q.getImageUrl());assertEquals(q.getImageUrl(),q.getSecondaryImageUrl());assertEquals(q.getImageUrl(),q.getAudioUrl());
        verify(repo,times(1)).save(q);verify(files,times(3)).findOne(any(Query.class));
    }
    @Test void preservesAdminMediaAndQuestionContent(){
        var files=mock(GridFsTemplate.class);var repo=mock(SituationalQuestionRepository.class);var q=question();
        q.setImageUrl("/api/situational/media/admin-one");q.setSecondaryImageUrl("https://example.com/custom.webp");q.setAudioUrl("/api/situational/media/admin-audio");
        when(repo.findByGameTypeIgnoreCaseOrderByOrderAsc("EXPRESSION_MATCH")).thenReturn(List.of(q));
        new ExpressionMediaSeeder(files,repo,new ObjectMapper()).seed();
        verifyNoInteractions(files);verify(repo,never()).save(any());assertEquals("https://example.com/custom.webp",q.getSecondaryImageUrl());
    }
    @Test void ignoresDifferentScenariosAndNonCanonicalSets(){
        var files=mock(GridFsTemplate.class);var repo=mock(SituationalQuestionRepository.class);var changed=question();changed.setScenario("A custom teacher-authored scene.");var otherSet=question();otherSet.setSetNumber(2);
        when(repo.findByGameTypeIgnoreCaseOrderByOrderAsc("EXPRESSION_MATCH")).thenReturn(List.of(changed,otherSet));
        new ExpressionMediaSeeder(files,repo,new ObjectMapper()).seed();verifyNoInteractions(files);verify(repo,never()).save(any());
    }
    @Test void genericDetectionNeverTreatsCustomMediaAsPlaceholder(){
        assertTrue(ExpressionMediaSeeder.isPlaceholder("/expression-match/gesture-wave.png"));
        assertFalse(ExpressionMediaSeeder.isPlaceholder("https://example.com/customer-picture.webp"));
        assertFalse(ExpressionMediaSeeder.isPlaceholder("/api/situational/media/abc"));
    }
}
