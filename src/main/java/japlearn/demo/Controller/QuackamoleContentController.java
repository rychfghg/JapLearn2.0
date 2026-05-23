package japlearn.demo.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.QuackamoleContent;
import japlearn.demo.Service.QuackamoleContentService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/quackamolecontent")
public class QuackamoleContentController {

    private final QuackamoleContentService service;

    public QuackamoleContentController(QuackamoleContentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<QuackamoleContent>> getAllContent() {
        // This returns a list of selected characters from the database
        List<QuackamoleContent> selectedCharacters = service.getAllContent();
        return ResponseEntity.ok(selectedCharacters);
    }

    @PostMapping("/add")
    public ResponseEntity<QuackamoleContent> addCharacter(@RequestBody CharacterRequest request) {
        return ResponseEntity.ok(service.addCharacter(request.getKana(), request.getRomaji()));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<QuackamoleContent> removeCharacter(@RequestBody CharacterRequest request) {
        return ResponseEntity.ok(service.removeCharacter(request.getKana(), request.getRomaji()));
    }

    static class CharacterRequest {
        private String kana;
        private String romaji;

        public String getKana() {
            return kana;
        }

        public void setKana(String kana) {
            this.kana = kana;
        }

        public String getRomaji() {
            return romaji;
        }

        public void setRomaji(String romaji) {
            this.romaji = romaji;
        }
    }
}
