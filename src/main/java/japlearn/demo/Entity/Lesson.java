package japlearn.demo.Entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lessons")
public class Lesson {
	
	@Id
	private String id;
	private String classId;
	private String lesson_title;
	private String lesson_type;
	
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

	public String getClassId() {
		return classId;
	}

	public void setClassId(String classId) {
		this.classId = classId;
	}
}
