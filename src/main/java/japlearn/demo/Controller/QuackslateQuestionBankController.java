package japlearn.demo.Controller;

import java.util.Collections;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import japlearn.demo.Entity.QuackslateQuestion;
import japlearn.demo.Repository.QuackslateQuestionRepository;
import japlearn.demo.Repository.QuackslateContentRepository;
import japlearn.demo.Repository.QuackslateGameCodeRepository;
import japlearn.demo.Entity.QuackslateContent;
import japlearn.demo.Entity.QuackslateGameCode;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/quackslate/question-bank")
public class QuackslateQuestionBankController {
    private final QuackslateQuestionRepository repository;
    private final QuackslateContentRepository contentRepository;
    private final QuackslateGameCodeRepository gameRepository;

    public QuackslateQuestionBankController(QuackslateQuestionRepository repository,
            QuackslateContentRepository contentRepository, QuackslateGameCodeRepository gameRepository) {
        this.repository = repository;
        this.contentRepository = contentRepository;
        this.gameRepository = gameRepository;
    }

    @GetMapping
    public List<QuackslateQuestion> getAll() { return repository.findAll(); }

    @GetMapping("/system")
    public List<QuackslateQuestion> getSystemQuestions(@RequestParam(defaultValue = "10") int limit) {
        List<QuackslateQuestion> questions = repository.findByApprovedTrueAndSystemAvailableTrue();
        Collections.shuffle(questions);
        return questions.stream().limit(Math.max(1, Math.min(limit, 30))).toList();
    }

    @PostMapping
    public QuackslateQuestion create(@RequestBody QuackslateQuestion question) {
        return repository.save(question);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuackslateQuestion> update(@PathVariable String id,
            @RequestBody QuackslateQuestion question) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        question.setId(id);
        return ResponseEntity.ok(repository.save(question));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{gameCode}")
    public ResponseEntity<?> assignSessionQuestions(@PathVariable String gameCode,
            @RequestBody List<String> questionIds) {
        QuackslateGameCode game = gameRepository.findByGameCode(gameCode).orElse(null);
        if (game == null) return ResponseEntity.notFound().build();
        contentRepository.deleteAll(contentRepository.findByGameCode(gameCode));
        int baseId = Math.abs(gameCode.hashCode() % 100000) * 100;
        List<QuackslateQuestion> selected = repository.findAllById(questionIds);
        for (int index = 0; index < selected.size(); index++) {
            QuackslateQuestion question = selected.get(index);
            QuackslateContent content = new QuackslateContent(baseId + index, question.getPrompt(),
                    question.getTranslation(), question.getCategory(), gameCode, question.getOptions(),
                    question.getCorrectAnswer(), null);
            contentRepository.save(content);
        }
        return ResponseEntity.ok(selected.size());
    }
}
