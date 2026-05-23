package japlearn.demo.Controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import japlearn.demo.Entity.DatabankLessonContent;
import japlearn.demo.Service.DatabankLessonContentService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/DatabankLessonContent")
public class DatabankLessonContentController {

	@Autowired
	DatabankLessonContentService databanklessoncontentservice;
	
	@PostMapping("/addDatabankLessonContent")
    public DatabankLessonContent addLessonContent(
        @RequestPart("lessonContent") String lessonContentJson, 
        @RequestPart(value = "imageFile", required = false) MultipartFile imageFile, 
        @RequestPart(value = "audioFile", required = false) MultipartFile audioFile) throws IOException {

        // Deserialize the lessonContent JSON into the LessonContent object
        ObjectMapper objectMapper = new ObjectMapper();
        DatabankLessonContent lessonContent = objectMapper.readValue(lessonContentJson, DatabankLessonContent.class);

        return databanklessoncontentservice.addLessonContent(lessonContent, imageFile, audioFile);
    }
	
	
	@GetMapping("/getAllDatabankLessonContentWithFiles/{lessonPageId}")
	public List<DatabankLessonContent> getAllLessonContent(@PathVariable String lessonPageId) throws IOException {
		return databanklessoncontentservice.getAllLessonContentWithFiles(lessonPageId);
	}
		
	@DeleteMapping("/deleteDatabankLessonContent")
	public void deleteLessonContent(@RequestParam String lessonContentId) {
		databanklessoncontentservice.deleteLessonContent(lessonContentId);
	}
	
	@DeleteMapping("/deleteAllDatabankLessonContent")
	public void deleteAllLessonContent(@RequestParam String lessonPageId) {
		databanklessoncontentservice.deleteAllLessonContent(lessonPageId);
	}
	
	@PutMapping("/updateDatabankLessonContent/{lessonContentId}")
	public DatabankLessonContent updateLessonContent(@PathVariable("lessonContentId") String lessonContentId,
			@RequestPart("lessonContent") DatabankLessonContent lessonContent,
			@RequestPart(value="imageFile", required=false) MultipartFile imageFile, 
			@RequestPart(value="audioFile", required=false) MultipartFile audioFile) throws IOException {
		return databanklessoncontentservice.updateLessonContent(lessonContent, lessonContentId, imageFile, audioFile);
	}
}
