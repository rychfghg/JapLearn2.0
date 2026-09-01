package japlearn.demo.Config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import japlearn.demo.Entity.SituationalQuestion;
import japlearn.demo.Repository.SituationalQuestionRepository;

@Component
public class PolitenessQuestionSeeder implements ApplicationRunner {
    private final SituationalQuestionRepository repository;
    private final ObjectMapper objectMapper;

    public PolitenessQuestionSeeder(SituationalQuestionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var resource = new ClassPathResource("politeness-seed.json");
        SituationalQuestion[] records = objectMapper.readValue(resource.getInputStream(), SituationalQuestion[].class);
        Map<Integer, SituationalQuestion> existingByOrder = new HashMap<>();
        repository.findByGameTypeIgnoreCaseOrderByOrderAsc("POLITENESS")
                .forEach(item -> existingByOrder.put(item.getOrder(), item));

        for (SituationalQuestion seed : Arrays.asList(records)) {
            SituationalQuestion existing = existingByOrder.get(seed.getOrder());
            if (existing == null) {
                repository.save(seed);
                continue;
            }

            // Backfill newly introduced media/character fields only. Never
            // overwrite content already edited or hidden by an administrator.
            boolean changed = false;
            if (isBlank(existing.getSpeaker())) { existing.setSpeaker(seed.getSpeaker()); changed = true; }
            if (isBlank(existing.getCharacterKey())) { existing.setCharacterKey(seed.getCharacterKey()); changed = true; }
            if (isBlank(existing.getNpcLine())) { existing.setNpcLine(seed.getNpcLine()); changed = true; }
            if (isBlank(existing.getNpcRomaji())) { existing.setNpcRomaji(seed.getNpcRomaji()); changed = true; }
            if (isBlank(existing.getAudioUrl())) { existing.setAudioUrl(seed.getAudioUrl()); changed = true; }
            if (changed) repository.save(existing);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
