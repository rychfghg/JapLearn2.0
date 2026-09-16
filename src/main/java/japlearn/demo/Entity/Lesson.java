package japlearn.demo.Entity;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lessons")
public class Lesson {
	
	@Id
	private String id;
	private String classId;
	private String lesson_title;
	private String lesson_type;
	private String lesson_description;
	private String ownerTeacherEmail;
	private String sourceFileName;
	private List<LessonSection> sections = new ArrayList<>();
	private List<QuizQuestion> quiz = new ArrayList<>();
	private Integer quizTimerSeconds;
	private boolean published = true;
	private Instant createdAt;

	public static class LessonSection {
		private String heading;
		private String body;
		public LessonSection() {}
		public LessonSection(String heading, String body) { this.heading=heading; this.body=body; }
		public String getHeading(){return heading;} public void setHeading(String value){heading=value;}
		public String getBody(){return body;} public void setBody(String value){body=value;}
	}
	public static class QuizQuestion {
		private String prompt;
		private String type;
		private List<String> choices = new ArrayList<>();
		private String correctAnswer;
		private String explanation;
		public String getPrompt(){return prompt;} public void setPrompt(String value){prompt=value;}
		public String getType(){return type;} public void setType(String value){type=value;}
		public List<String> getChoices(){return choices;} public void setChoices(List<String> value){choices=value==null?new ArrayList<>():value;}
		public String getCorrectAnswer(){return correctAnswer;} public void setCorrectAnswer(String value){correctAnswer=value;}
		public String getExplanation(){return explanation;} public void setExplanation(String value){explanation=value;}
	}
	
	public Lesson() {}

	public Lesson(String id, String classId, String lesson_title, String lesson_type) {
		super();
		
		this.id = id;
		this.classId = classId;
		this.lesson_title = lesson_title;
		this.lesson_type = lesson_type;
	}


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLesson_title() {
		return lesson_title;
	}

	public void setLesson_title(String lesson_title) {
		this.lesson_title = lesson_title;
	}

	public String getLesson_type() {
		return lesson_type;
	}

	public void setLesson_type(String lesson_type) {
		this.lesson_type = lesson_type;
	}

	public String getLesson_description() {
		return lesson_description;
	}

	public void setLesson_description(String lesson_description) {
		this.lesson_description = lesson_description;
	}

	public String getClassId() {
		return classId;
	}

	public void setClassId(String classId) {
		this.classId = classId;
	}
	public String getOwnerTeacherEmail(){return ownerTeacherEmail;} public void setOwnerTeacherEmail(String value){ownerTeacherEmail=value;}
	public String getSourceFileName(){return sourceFileName;} public void setSourceFileName(String value){sourceFileName=value;}
	public List<LessonSection> getSections(){return sections==null?List.of():sections;} public void setSections(List<LessonSection> value){sections=value==null?new ArrayList<>():value;}
	public List<QuizQuestion> getQuiz(){return quiz==null?List.of():quiz;} public void setQuiz(List<QuizQuestion> value){quiz=value==null?new ArrayList<>():value;}
	public Integer getQuizTimerSeconds(){return quizTimerSeconds;} public void setQuizTimerSeconds(Integer value){quizTimerSeconds=value;}
	public boolean isPublished(){return published;} public void setPublished(boolean value){published=value;}
	public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant value){createdAt=value;}
}
