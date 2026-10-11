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
public class QuackmanContentController {

    @Autowired
    private QuackmanContentService service;

    @GetMapping
    public List<QuackmanContent> getAllContent() {
        return service.getAllContent();
    }

    @PostMapping
    public ResponseEntity<?> addContent(@RequestBody QuackmanContent content) {
        try {
            return ResponseEntity.ok(service.addContent(content));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateContent(
            @PathVariable String id, @RequestBody QuackmanContent updatedContent) {
        try {
            return ResponseEntity.ok(service.updateContent(id, updatedContent));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "That word no longer exists. Refresh the list."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteContent(@PathVariable String id) {
        service.deleteContent(id);
        return ResponseEntity.ok("Content deleted successfully.");
    }
}
