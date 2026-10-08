package japlearn.demo.Entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One Requirements & Design Refactoring priority written by the team from MVP validation evidence.
 * Answers the five questions of the refactoring submission for one SMART objective.
 */
@Document(collection = "smart_refactoring_priorities")
public class SmartRefactoringPriority {
    @Id private String id;
    private String goalKey;
    private int rank;
    private boolean included = true;
    private String learned;
    private String evidence;
    private String objective;
    private String aspect;
    private String expectedImprovement;
    private Instant updatedAt = Instant.now();
    private String updatedBy;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGoalKey() { return goalKey; }
    public void setGoalKey(String goalKey) { this.goalKey = goalKey; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public boolean isIncluded() { return included; }
    public void setIncluded(boolean included) { this.included = included; }
    public String getLearned() { return learned; }
    public void setLearned(String learned) { this.learned = learned; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    public String getAspect() { return aspect; }
    public void setAspect(String aspect) { this.aspect = aspect; }
    public String getExpectedImprovement() { return expectedImprovement; }
    public void setExpectedImprovement(String expectedImprovement) { this.expectedImprovement = expectedImprovement; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
