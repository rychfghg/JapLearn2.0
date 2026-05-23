package japlearn.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.QuackmanContent;
import japlearn.demo.Service.QuackmanContentService;

@RestController
@RequestMapping("/api/quackmancontent")
@CrossOrigin(origins = "*")
public class QuackmanContentController {

    @Autowired
    private QuackmanContentService service;

    @GetMapping
    public List<QuackmanContent> getAllContent() {
        return service.getAllContent();
    }

    @PostMapping
    public ResponseEntity<QuackmanContent> addContent(@RequestBody QuackmanContent content) {
        QuackmanContent createdContent = service.addContent(content);
        return ResponseEntity.ok(createdContent);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuackmanContent> updateContent(
            @PathVariable String id, @RequestBody QuackmanContent updatedContent) {
        QuackmanContent content = service.updateContent(id, updatedContent);
        return ResponseEntity.ok(content);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteContent(@PathVariable String id) {
        service.deleteContent(id);
        return ResponseEntity.ok("Content deleted successfully.");
    }
}
