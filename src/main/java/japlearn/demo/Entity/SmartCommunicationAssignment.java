package japlearn.demo.Entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A communication activity assigned to a class for SMART goal 4 (activity completion).
 * New records only: assignments are created prospectively from the admin dashboard,
 * completion is read from the existing game records inside the assignment window.
 */
@Document(collection = "smart_communication_assignments")
public class SmartCommunicationAssignment {
    @Id private String id;
    private String classCode;
    /** RECOGNITION, EXPRESSION_MATCH, POLITENESS, REPLY_COACH, RESPONSE_RUSH, DIALOGUE_RELAY, QUACKTALK */
    private String activityKey;
    private Integer level;
    private String chapterId;
    /** How many distinct finished units (levels/sets, chapters, runs or sessions) complete this assignment. Default 1. */
    private Integer requiredCount;
    private String title;
    private Instant startsAt;
    private Instant dueAt;
    private Instant createdAt = Instant.now();
    private String createdBy;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }
    public String getActivityKey() { return activityKey; }
    public void setActivityKey(String activityKey) { this.activityKey = activityKey; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getChapterId() { return chapterId; }
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }
    public Integer getRequiredCount() { return requiredCount; }
    public void setRequiredCount(Integer requiredCount) { this.requiredCount = requiredCount; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
