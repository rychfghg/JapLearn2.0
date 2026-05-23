package japlearn.demo.Controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.QuackslateContent;
import japlearn.demo.Service.QuackslateContentService;

@RestController
@RequestMapping("/api/quackslateContent")
@CrossOrigin(origins ="*")
public class QuackslateContentController {

    private final QuackslateContentService quackslateContentService;
    

    @Autowired
    public QuackslateContentController(QuackslateContentService quackslateContentService) {
        this.quackslateContentService = quackslateContentService;
        
    }

    // Add new content
    @PostMapping("/addQuackslateContent")
    public QuackslateContent addQuackslateContent(@RequestBody QuackslateContent quackslateContent) {
        return quackslateContentService.addQuackslateContent(quackslateContent);
    }

    // Get all content (shared across all class codes)
    @GetMapping("/getAllQuackslateContent")
    public List<QuackslateContent> getAllQuackslateContent() {
        return quackslateContentService.getAllQuackslateContent();
    }

    // Get content by ID
    @GetMapping("/getQuackslateContentById/{id}")
    public Optional<QuackslateContent> getQuackslateContentById(@PathVariable int id) {
        return quackslateContentService.getQuackslateContentById(id);
    }

    // Get content by game code (optional)
    @GetMapping("/getByGameCode/{gameCode}")
    public List<QuackslateContent> getByGameCode(@PathVariable String gameCode) {
        return quackslateContentService.getByGameCode(gameCode);
    }

    // Update content
    @PutMapping("/updateQuackslateContent/{id}")
    public QuackslateContent updateQuackslateContent(@PathVariable int id, @RequestBody QuackslateContent quackslateContent) {
        quackslateContent.setId(id);
        return quackslateContentService.updateQuackslateContent(quackslateContent);
    }

    // Delete content by ID
    @DeleteMapping("/deleteQuackslateContent/{id}")
    public void deleteQuackslateContent(@PathVariable int id) {
        quackslateContentService.deleteQuackslateContent(id);
    }

}
