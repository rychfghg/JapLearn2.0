package japlearn.demo.Service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import japlearn.demo.Entity.*;

class PolitenessContextTest {
    @Test void preservesObservedContextAndAcceptedAlternatives() throws Exception {
        var mapper=new ObjectMapper().findAndRegisterModules();
        var row=new PolitenessQuestionResult("16","Unfamiliar adult · company receptionist","Interview applicant",
            "Introduce yourself","お名前をお願いいたします。","バコロドです",
            List.of("バコロドと申します","バコロドです"),true,false,"2026-10-16T00:00:00Z");
        var attempt=new SituationalAttempt();attempt.setGameType("POLITENESS");attempt.setPolitenessResponses(List.of(row));
        var restored=mapper.readValue(mapper.writeValueAsString(attempt),SituationalAttempt.class);
        assertEquals(List.of(row),restored.getPolitenessResponses());
        assertNull(new SituationalAttempt().getPolitenessResponses(),"Legacy totals must not acquire invented response history");
    }
    @Test void publishedQuestionsSupportExplicitContextAndChoiceFeedback() throws Exception {
        var mapper=new ObjectMapper();var question=new SituationalQuestion();
        question.setRelationship("Customer");question.setLearnerRole("Staff member");
        var choice=new SituationalQuestion.ResponseChoice("確認いたします","Kakunin itashimasu");
        choice.setAcceptable(true);choice.setFeedback("This respectfully offers to check a customer's request.");question.setChoices(List.of(choice));
        var restored=mapper.readValue(mapper.writeValueAsString(question),SituationalQuestion.class);
        assertEquals("Customer",restored.getRelationship());assertEquals("Staff member",restored.getLearnerRole());
        assertTrue(restored.getChoices().get(0).getAcceptable());assertNotNull(restored.getChoices().get(0).getFeedback());
    }
}
