package japlearn.demo.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import japlearn.demo.Config.ExpressionMatchSeeder;
import japlearn.demo.Entity.SituationalQuestion;
import japlearn.demo.Repository.SituationalQuestionRepository;

class ExpressionMatchReplacementTest {
    @Test void replacesDotAndSeedsMissingExpressionsDespiteDuplicateRows() throws Exception {
        var repo=mock(SituationalQuestionRepository.class);
        List<SituationalQuestion> stored=new ArrayList<>();
        for(int i=0;i<3;i++){
            var row=new SituationalQuestion();row.setId("duplicate-"+i);row.setLevel(1);row.setSetNumber(1);row.setOrder(i);
            row.setCorrectAnswer("またあした");row.setScenario(".");row.setSecondaryScenario(".");stored.add(row);
        }
        var dot=new SituationalQuestion();dot.setId("dot");dot.setLevel(2);dot.setSetNumber(1);
        dot.setCorrectAnswer("お待たせしました");dot.setScenario("Waiting");dot.setSecondaryScenario(".");stored.add(dot);
        when(repo.findByGameTypeIgnoreCaseOrderByOrderAsc("EXPRESSION_MATCH")).thenAnswer(call->new ArrayList<>(stored));
        when(repo.save(any(SituationalQuestion.class))).thenAnswer(call->{
            SituationalQuestion row=call.getArgument(0);
            if(row.getId()==null){row.setId(UUID.randomUUID().toString());stored.add(row);}return row;
        });
        var seeder=new ExpressionMatchSeeder(repo);seeder.run();
        assertEquals("You apologize after making someone wait.",dot.getScenario());
        assertEquals("You ask a guest to wait for a moment.",dot.getSecondaryScenario());
        assertTrue(dot.getChoices().stream().anyMatch(c->"お待たせしました".equals(c.getJapanese())));
        for(int level=1;level<=3;level++){
            final int current=level;
            assertEquals(20,stored.stream().filter(row->row.getLevel()==current).map(SituationalQuestion::getCorrectAnswer).distinct().count());
        }
        int size=stored.size();seeder.run();assertEquals(size,stored.size(),"Restarting the server must not add more copies");
    }
}
