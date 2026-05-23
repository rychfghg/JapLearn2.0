package japlearn.demo.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lessonPages")
public class LessonPage {
	
	@Id
	private String id;
	private String lessonId;
	private String page_title;
	
	public LessonPage() {}

	public LessonPage(String id, String lessonId, String page_title) {
		super();
		this.id = id;
		this.lessonId = lessonId;
		this.page_title = page_title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLessonId() {
		return lessonId;
	}

	public void setLessonId(String lessonId) {
		this.lessonId = lessonId;
	}

	public String getPage_title() {
		return page_title;
	}

	public void setPage_title(String page_title) {
		this.page_title = page_title;
	}
	
	
}
