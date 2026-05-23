package japlearn.demo.Service;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.client.gridfs.model.GridFSFile;

import japlearn.demo.Entity.DatabankLessonContent;
import japlearn.demo.Repository.DatabankLessonContentRepository;

@Service
public class DatabankLessonContentService {

	@Autowired
	private GridFsTemplate template;
	
	@Autowired
	private GridFsOperations operations;
	
	@Autowired
	DatabankLessonContentRepository databanklessoncontentrepository;
	
	public DatabankLessonContent addLessonContent(DatabankLessonContent lessoncontent, MultipartFile imageFile, 
			MultipartFile audioFile) throws IOException {

		String imageFileId = null;
		String audioFileId = null;
		
		if(imageFile != null) { 
			imageFileId = addFile(imageFile);
			lessoncontent.setImageFileId(imageFileId);
			lessoncontent.setImage_name(imageFile.getOriginalFilename());
			lessoncontent.setImage_type(imageFile.getContentType());
		}
		
		if(audioFile != null) {
			audioFileId = addFile(audioFile);;
			lessoncontent.setAudioFileId(audioFileId);
			lessoncontent.setAudio_name(audioFile.getOriginalFilename());
			lessoncontent.setAudio_type(audioFile.getContentType());
		}
		
		return databanklessoncontentrepository.save(lessoncontent);
	}
	
	public String addFile(MultipartFile file) throws IOException {
		
		Object fileId = template.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
		
		return fileId.toString();
	}
	
	public List<DatabankLessonContent> getAllLessonContentWithFiles(String LessonPageid) throws IOException {
		List<DatabankLessonContent> lessonContents = databanklessoncontentrepository.findByLessonPageId(LessonPageid);
		
		for(DatabankLessonContent lessonContent: lessonContents) {
			if(lessonContent.getImageFileId() != null) { 
				GridFSFile imageFile = template.findOne(new Query(Criteria.where("_id").is(lessonContent.getImageFileId() )) );
				GridFsResource imageResource = operations.getResource(imageFile);
				lessonContent.setImageData(imageResource.getInputStream().readAllBytes());
			}
			
			if(lessonContent.getAudioFileId() != null) {
				GridFSFile audioFile = template.findOne(new Query(Criteria.where("_id").is(lessonContent.getAudioFileId() )) );
				GridFsResource audioResource = operations.getResource(audioFile);
				lessonContent.setAudioData(audioResource.getInputStream().readAllBytes());
			}
		}
		
		return lessonContents;
	}
	
	public List<DatabankLessonContent> getAllLessonContent(String lessonPageId){
		return databanklessoncontentrepository.findByLessonPageId(lessonPageId);
	}
	
	public void deleteLessonContent(String lessonContentId) {
		DatabankLessonContent lessonContent = databanklessoncontentrepository.findById(lessonContentId)
				.orElseThrow(() -> new NoSuchElementException("LessonContent not found with ID: " + lessonContentId));
		
		template.delete(new Query(Criteria.where("_id").is(lessonContent.getImageFileId() )) );
		template.delete(new Query(Criteria.where("_id").is(lessonContent.getAudioFileId() )) );
		
		databanklessoncontentrepository.deleteById(lessonContentId); 
	}
	
	public void deleteAllLessonContent(String lessonPageId) {
		List <DatabankLessonContent> lessonContents = databanklessoncontentrepository.findByLessonPageId(lessonPageId);
		
		for(DatabankLessonContent lessonContent : lessonContents) {
			
			template.delete(new Query(Criteria.where("_id").is(lessonContent.getImageFileId() )) );
			template.delete(new Query(Criteria.where("_id").is(lessonContent.getAudioFileId() )) );
		
		}
		
		databanklessoncontentrepository.deleteByLessonPageId(lessonPageId);
	}
	
	public DatabankLessonContent updateLessonContent(DatabankLessonContent newLessonContent, 
			String lessonContentId, MultipartFile imageFile, 
			MultipartFile audioFile) throws IOException {
		
		DatabankLessonContent lessonContent = databanklessoncontentrepository.findById(lessonContentId)
				.orElseThrow(() -> new IllegalArgumentException("LessonContent not found with ID: " + lessonContentId));
		
		lessonContent.setText_content(newLessonContent.getText_content());
		
		if(imageFile != null) {
			if(lessonContent.getImageFileId() != null ) {
				deleteExistingStoredFile(lessonContent.getImageFileId());
			}
			
			String imageFileId = addFile(imageFile);
			lessonContent.setImageFileId(imageFileId);
			lessonContent.setImage_name(imageFile.getOriginalFilename());
			lessonContent.setImage_type(imageFile.getContentType());
		}
		
		if(audioFile != null) {
			if(lessonContent.getAudioFileId() != null ) {
				deleteExistingStoredFile(lessonContent.getAudioFileId());
			}
			
			String audioFileId = addFile(audioFile);
			lessonContent.setAudioFileId(audioFileId);
			lessonContent.setAudio_name(audioFile.getOriginalFilename());
			lessonContent.setAudio_type(audioFile.getContentType());
		}
		
		return databanklessoncontentrepository.save(lessonContent);
	}
	
	public void deleteExistingStoredFile(String id) {
		template.delete(new Query(Criteria.where("_id").is(id) ));
	}
}
