package japlearn.demo.Entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="lessonQuizAttempts")
public class LessonQuizAttempt {
 @Id private String id;
 private String lessonId, classCode, studentEmail, studentName;
 private int score, maxScore; private double percentage; private Instant submittedAt;
 private List<String> answers=new ArrayList<>();
 public String getId(){return id;} public void setId(String v){id=v;}
 public String getLessonId(){return lessonId;} public void setLessonId(String v){lessonId=v;}
 public String getClassCode(){return classCode;} public void setClassCode(String v){classCode=v;}
 public String getStudentEmail(){return studentEmail;} public void setStudentEmail(String v){studentEmail=v;}
 public String getStudentName(){return studentName;} public void setStudentName(String v){studentName=v;}
 public int getScore(){return score;} public void setScore(int v){score=v;} public int getMaxScore(){return maxScore;} public void setMaxScore(int v){maxScore=v;}
 public double getPercentage(){return percentage;} public void setPercentage(double v){percentage=v;}
 public Instant getSubmittedAt(){return submittedAt;} public void setSubmittedAt(Instant v){submittedAt=v;}
 public List<String> getAnswers(){return answers;} public void setAnswers(List<String> v){answers=v==null?new ArrayList<>():v;}
}
