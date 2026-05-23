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

import japlearn.demo.Entity.LessonContent;
import japlearn.demo.Service.LessonContentService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/lessonContent")
public class LessonContentController {

	@Autowired
	LessonContentService lessoncontentservice;
	
	@PostMapping("/addLessonContent")
    public LessonContent addLessonContent(
        @RequestPart("lessonContent") String lessonContentJson, 
        @RequestPart(value = "imageFile", required = false) MultipartFile imageFile, 
        @RequestPart(value = "audioFile", required = false) MultipartFile audioFile) throws IOException {

        // Deserialize the lessonContent JSON into the LessonContent object
        ObjectMapper objectMapper = new ObjectMapper();
        LessonContent lessonContent = objectMapper.readValue(lessonContentJson, LessonContent.class);

        return lessoncontentservice.addLessonContent(lessonContent, imageFile, audioFile);
    }
	
	
	@GetMapping("/getAllLessonContentWithFiles/{lessonPageId}")
	public List<LessonContent> getAllLessonContent(@PathVariable String lessonPageId) throws IOException {
		return lessoncontentservice.getAllLessonContentWithFiles(lessonPageId);
	}
		
	@DeleteMapping("/deleteLessonContent")
	public void deleteLessonContent(@RequestParam String lessonContentId) {
		lessoncontentservice.deleteLessonContent(lessonContentId);
	}
	
	@DeleteMapping("/deleteAllLessonContent")
	public void deleteAllLessonContent(@RequestParam String lessonPageId) {
		lessoncontentservice.deleteAllLessonContent(lessonPageId);
	}
	
	@PutMapping("/updateLessonContent/{lessonContentId}")
	public LessonContent updateLessonContent(@PathVariable("lessonContentId") String lessonContentId,
			@RequestPart("lessonContent") String lessonContentJson,
			@RequestPart(value="imageFile", required=false) MultipartFile imageFile, 
			@RequestPart(value="audioFile", required=false) MultipartFile audioFile) throws IOException {
		
		ObjectMapper objectMapper = new ObjectMapper();
        LessonContent lessonContent = objectMapper.readValue(lessonContentJson, LessonContent.class);
		return lessoncontentservice.updateLessonContent(lessonContent, lessonContentId, imageFile, audioFile);
	}
}
