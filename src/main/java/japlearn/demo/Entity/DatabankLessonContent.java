package japlearn.demo.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "DatabankLessonContent")
public class DatabankLessonContent {

	@Id
	private String id;
	private String lessonPageId;
	private String text_content;
	
	private byte[] imageData;
	private String imageFileId;
	private String image_name;
	private String image_type;
	
	private byte[] audioData;
	private String audioFileId;
	private String audio_name;
	private String audio_type;
	
	private String contentGame;

	public DatabankLessonContent() {
		super();
	}
	
	public DatabankLessonContent(String id, String lessonPageId, String text_content, String imageFileId, String audioFileId, String contentGame) {
		super();
		this.id = id;
		this.lessonPageId = lessonPageId;
		this.text_content = text_content;
		this.imageFileId = imageFileId;
		this.audioFileId = audioFileId;
		this.contentGame = contentGame;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getLessonPageId() {
		return lessonPageId;
	}
	
	public void setLessonPageId(String lessonPageId) {
		this.lessonPageId = lessonPageId;
	}
	
	public String getText_content() {
		return text_content;
	}
	
	public void setText_content(String text_content) {
		this.text_content = text_content;
	}
	
	public String getImageFileId() {
		return imageFileId;
	}
	
	public void setImageFileId(String imageFileId) {
		this.imageFileId = imageFileId;
	}
	
	public String getAudioFileId() {
		return audioFileId;
	}
	
	public void setAudioFileId(String audioFileId) {
		this.audioFileId = audioFileId;
	}

	public String getImage_name() {
		return image_name;
	}

	public void setImage_name(String image_name) {
		this.image_name = image_name;
	}

	public String getImage_type() {
		return image_type;
	}

	public void setImage_type(String image_type) {
		this.image_type = image_type;
	}

	public String getAudio_name() {
		return audio_name;
	}

	public void setAudio_name(String audio_name) {
		this.audio_name = audio_name;
	}

	public String getAudio_type() {
		return audio_type;
	}

	public void setAudio_type(String audio_type) {
		this.audio_type = audio_type;
	}

	public String getContentGame() {
		return contentGame;
	}

	public void setContentGame(String contentGame) {
		this.contentGame = contentGame;
	}

	public byte[] getImageData() {
		return imageData;
	}

	public void setImageData(byte[] imageData) {
		this.imageData = imageData;
	}

	public byte[] getAudioData() {
		return audioData;
	}

	public void setAudioData(byte[] audioData) {
		this.audioData = audioData;
	}
}
